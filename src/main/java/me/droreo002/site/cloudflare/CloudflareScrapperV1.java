package me.droreo002.site.cloudflare;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudflareScrapperV1 {

    private static final String URL_TEMPLATE = "http://%website%/cdn-cgi/l/chk_jschl";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
    private static final int CLOUDFLARE_MAX_TIMEOUT = 40000;

    @Getter
    private String url;
    @Getter
    private Connection connection;
    @Getter
    private ChallengeCallback challengeCallback;
    @Getter
    private int challengeToSolve = 3;
    @Getter
    private Document document;
    @Getter
    private int maxTimeOut;
    @Getter
    private List<HttpCookie> cookieList;

    public CloudflareScrapperV1(String url, ChallengeCallback challengeCallback, int maxTimeOut) {
        this.url = url;
        this.challengeCallback = challengeCallback;
        this.maxTimeOut = maxTimeOut;
        this.cookieList = new ArrayList<>();
    }

    /**
     * Start the challenge
     *
     * @throws Exception if something goes wrong
     */
    public void start() throws Exception {
        this.connection = Jsoup.connect(url)
                .timeout((maxTimeOut == 0) ? CLOUDFLARE_MAX_TIMEOUT : maxTimeOut)
                .userAgent(USER_AGENT)
                .ignoreHttpErrors(true);
        this.document = connection.get();

        for (int i = this.challengeToSolve; i > 0; i--) {
            this.challengeToSolve = i;
            if (!isChallenge()) {
                System.out.println("Challenge has been solved :D");
                challengeCallback.onSuccess(document);
                return;
            } else {
                onChallenge(document);
            }
        }
        System.out.println("Failed to solve CloudFlare challenge... :(");
    }

    private boolean isChallenge() {
        String html = document.html();

        boolean isChallenge = html.contains("a = document.getElementById(\\'jschl-answer\\');");

        if (isChallenge) {
            System.out.println("Found a challenge!");
            return true;
        }

        boolean isRedirectChallenge = html.contains("You are being redirected") || html.contains("sucuri_cloudproxy_js");

        if (isRedirectChallenge) {
            // TODO: 11/12/2019 Make
            System.out.println("Found a redirect challenge!");
            return true;
        }

        // 503 status is always a challenge
        if (connection.response().statusCode() == 503) {
            System.out.println("Found a challenge!");
            return true;
        }
        return false;
    }

    private void onChallenge(Document document) throws Exception {
        System.out.println("================== Beginning challenge solver step [" + challengeToSolve + "]");
        System.out.println("Current cookies > " + this.connection.response().cookies().toString());
        Map<String, String> answerPayload = new HashMap<>();
        String challengeString;
        String html = document.html();

        // Find first hidden input. Why?, because first hidden input has randomized name
        Matcher matcher = Pattern.compile("name=\"(.+?)\" value=\"(.+?)\"").matcher(html);
        if (matcher.find()) {
            answerPayload.put(matcher.group(1), matcher.group(2));
        }

        // Find rest hidden input
        matcher = Pattern.compile("name=\"jschl_vc\" value=\"(\\w+)\"").matcher(html);
        if (!matcher.find()) throw new NullPointerException("Failed to find jschl_vc value!");
        answerPayload.put("jschl_vc", matcher.group(1)); // Group 1 is the value

        matcher = Pattern.compile("name=\"pass\" value=\"(.+?)\"").matcher(html);
        if (!matcher.find()) throw new NullPointerException("Failed to find pass value!");
        answerPayload.put("pass", matcher.group(1));

        matcher = Pattern.compile("getElementById\\('cf-content'\\)[\\s\\S]+?setTimeout.+?\\r?\\n([\\s\\S]+?a\\.value\\s*=.+?)\\r?\\n(?:[^{<>]*},\\s*(\\d{4,}))?").matcher(html);
        if (!matcher.find()) throw new NullPointerException("Failed to extract setTimeout callback!");
        int timeOut = Integer.parseInt(matcher.group(2));
        if (timeOut > CLOUDFLARE_MAX_TIMEOUT) {
            // TODO: 01/03/2020 Do something?
        }

        challengeString = matcher.group(1);
        String innerHTML = "";
        for (String i : challengeString.split(";")) {
            if (i.trim().split("=")[0].trim().equals("k")) {
                String rawInnerHtml = i.trim().split("=")[1].replace(" \\`", "");
                innerHTML = Pattern.compile("<div.*?id=\"" + rawInnerHtml + "\".*?>(.*?)</div>").matcher(html).group(1);
            }
        }

        String newChallenge =
            "var document = {" +
                "createElement: function() {" +
                    "return { firstChild: { href: \"http://%domain/\" } }" +
                    "}," +
                    "getElementById: function() {" +
                    "   return {\"innerHTML\": \"%innerHtml\"};" +
                    "}" +
                "};" +
            "%challenge; a.value"
                    .replace("%domain", url)
                    .replace("%innerHtml", innerHTML)
                    .replace("%challenge", challengeString);

        /*
        Evaluate the challenge
         */
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        String output;

        try {
            output = (String) engine.eval(newChallenge);
        } catch (ScriptException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Output is: " + output);

        try {
            Double.parseDouble(output);
        } catch (NumberFormatException e) {
            System.out.println("Challenge answer is not a number! (" + output + ")");
            e.printStackTrace();
            return;
        }

        answerPayload.put("jschl_answer", output);

        // Check what submit method form is currently using
        matcher = Pattern.compile("id=\"challenge-form\" action=\"(.+?)\" method=\"(.+?)\"").matcher(html);
        if (!matcher.find()) throw new IllegalStateException("Unknown submit method!");

        String solveUrl = (url + matcher.group(1)).replace("&amp;", "&");

        System.out.println("Solving via: " + matcher.group(2));
        System.out.println("Solving url: " + solveUrl);
        System.out.println("Solving payload: ");
        answerPayload.forEach((k, v) -> System.out.println(" " + k + ": '" + v + "'"));
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        if (matcher.group(2).equals("POST")) {
            this.document = this.connection
                    .url(solveUrl)
                    .data(answerPayload)
                    .method(Connection.Method.POST)
                    .userAgent(USER_AGENT)
                    .post();
        }
    }
}
