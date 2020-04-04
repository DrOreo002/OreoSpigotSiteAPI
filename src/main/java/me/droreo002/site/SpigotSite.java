package me.droreo002.site;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.cloudflare.CloudflareScrapperV1;
import me.droreo002.site.cloudflare.CloudflareScrapperV2;
import me.droreo002.site.manager.SpigotResourceManager;
import me.droreo002.site.manager.SpigotUserManager;
import me.droreo002.site.spigot.user.SpigotMasterUser;
import me.droreo002.site.utils.TOTPGenerator;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static me.droreo002.site.cloudflare.CloudflareScrapperV2.ACCEPT;
import static me.droreo002.site.cloudflare.CloudflareScrapperV2.listToString;

public class SpigotSite {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    public static final String SPIGOT_URL = "https://www.spigotmc.org";
    private static SpigotSite instance;

    @SneakyThrows
    public static SpigotSite init(File credentialsFile) {
        if (instance != null) throw new IllegalStateException("SpigotSite already initialized!");
        String[] c = Files.readLines(credentialsFile, Charset.defaultCharset()).get(0).split(",");
        instance = new SpigotSite(c[0], c[1], c[2]);
        instance.authenticateUser();
        return instance;
    }

    public static SpigotSite getInstance() {
        if (instance == null) throw new NullPointerException("SpigotSite is not yet initialized!");
        return instance;
    }

    @Getter
    private CloudflareScrapperV2 scrapper;
    @Getter
    private SpigotUserManager spigotUserManager;
    @Getter
    private SpigotResourceManager spigotResourceManager;
    @Getter
    private List<HttpCookie> siteCookieList;
    @Getter
    private String authorUserName, authorPassword, authorTotPSecret;
    @Getter @Nullable
    private SpigotMasterUser spigotMasterUser;

    @SneakyThrows
    public SpigotSite(String authorUserName, String authorPassword, String authorTotPSecret) {
        this.authorUserName = authorUserName;
        this.authorPassword = authorPassword;
        this.authorTotPSecret = authorTotPSecret;
        this.siteCookieList = new CopyOnWriteArrayList<>();

        // Core initializing
        this.scrapper = new CloudflareScrapperV2(SPIGOT_URL, CloudflareScrapperV1.USER_AGENT);
        scrapper.start(new ScrapperCallback()).get();
        System.out.println("CloudFlared: " + scrapper.isCloudFlared());
        while (!getDocument(SPIGOT_URL).get().html().contains("The SpigotMC project is run entirely in the spare time of our wonderful developers")) {
            this.siteCookieList.clear();
            this.scrapper = new CloudflareScrapperV2(SPIGOT_URL, CloudflareScrapperV1.USER_AGENT);
            scrapper.start(new ScrapperCallback()).get();
        }

        // Initialize managers
        this.spigotUserManager = new SpigotUserManager(this);
        this.spigotResourceManager = new SpigotResourceManager(this);
    }

    /**
     * Initialize the spigot site
     */
    @SneakyThrows
    public void authenticateUser() {
        Map<String, String> params = new HashMap<>();
        params.put("login", authorUserName);
        params.put("password", authorPassword);
        params.put("register", "0");
        params.put("remember", "1"); // No need to remember
        params.put("cookie_check", "1"); // Fix error Cookies required
        params.put("_xfToken", "");
        params.put("redirect", SPIGOT_URL);

        // We process totP
        postData(SPIGOT_URL + "/login/login", params).get(); // Login so we get the cookies first
        Document document = getDocument(SPIGOT_URL + "/login/two-step?remember=1&provider=totp").get();

        Element totpField = document.getElementById("ctrl_totp_code");
        if (totpField != null) {
            byte[] keyBytes = BaseEncoding.base32().decode(authorTotPSecret);
            StringBuilder sb = new StringBuilder();
            for (byte b : keyBytes) {
                sb.append(String.format("%02X", b));
            }
            String key = sb.toString().toLowerCase();
            String code = TOTPGenerator.generateTOTP(key, 6);
            String url = SPIGOT_URL + "/login/two-step";
            params = new HashMap<>();
            // Login parameters
            params.put("code", code);
            params.put("trust", "1");
            params.put("provider", "totp");
            params.put("_xfConfirm", "1");
            params.put("remember", "1");
            params.put("save", "Confirm");
            params.put("redirect", SPIGOT_URL);

            document = postData(url, params).get();
            if (document == null) throw new NullPointerException("Failed to login!");
            this.spigotMasterUser = new SpigotMasterUser(document);
        } else {
            throw new IllegalStateException("Failed to get TOTP Field!");
        }
        System.out.println("Successfully authenticated spigot user (" + authorUserName + ")");
    }

    /**
     * Get document on that url
     *
     * @param newUrl The target url
     * @return Document
     */
    @Nullable
    @SneakyThrows
    public Future<Document> getDocument(String newUrl) {
        return THREAD_POOL.submit(() -> {
            HttpURLConnection connection = newConnection(newUrl, Connection.Method.GET);

            URL possibleNewUrl = processResponseCode(connection).get();
            if (possibleNewUrl != null) {
                return getDocument(possibleNewUrl.toURI().toString()).get();
            }
            StringBuilder html = null;
            while (html == null) {
                try {
                    html = readHtml(connection);
                } catch (IOException ignored) {
                    // Ignored
                }
            }

            return Jsoup.parse(html.toString());
        });
    }

    /**
     * Read html result from connection
     *
     * @param connection The connection
     * @return Html as string
     */
    public StringBuilder readHtml(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        connection.disconnect();
        return response;
    }

    /**
     * Post data to that url (Will use the current cookies)
     *
     * @param url The url
     * @param params The data to send
     */
    @SneakyThrows
    public Future<Document> postData(String url, Map<String, String> params) {
        return THREAD_POOL.submit(() -> {
            HttpURLConnection connection = newConnection(url, Connection.Method.POST);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }

            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            connection.getOutputStream().write(postDataBytes);

            URL possibleNewUrl = processResponseCode(connection).get();
            if (possibleNewUrl != null) {
                return getDocument(possibleNewUrl.toURI().toString()).get();
            }

            return Jsoup.parse(readHtml(connection).toString());
        });
    }

    @SneakyThrows
    private Future<URL> processResponseCode(HttpURLConnection connection) {
        return THREAD_POOL.submit(() -> {
            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    this.siteCookieList = getNewestCookie(scrapper.getMCookieManager().getCookieStore().getCookies());
                    return null;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    authenticateUser();
                    return null;
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    this.siteCookieList = getNewestCookie(scrapper.getMCookieManager().getCookieStore().getCookies());
                    return new URL(connection.getHeaderField("Location"));
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    scrapper.start(new ScrapperCallback()).get(); // Start the scrapper back if failed
                    return null;
                case HttpURLConnection.HTTP_BAD_GATEWAY:
                    this.siteCookieList.clear(); // Clear cache and possibly go back?
                    return new URL(SPIGOT_URL);
            }
            return null;
        });
    }

    /**
     * Get the newest cookie
     *
     * @param cookieList The cookie list
     */
    private List<HttpCookie> getNewestCookie(List<HttpCookie> cookieList) {
        List<HttpCookie> newCookies = new ArrayList<>();
        HttpCookie newestCookie = null;
        for (HttpCookie httpCookie : cookieList) {
            if (!httpCookie.getName().equals("_cfduid")) {
                continue;
            }
            if (newestCookie == null) {
                newestCookie = httpCookie;
                continue;
            }
            newCookies.add(newestCookie);
            newestCookie = httpCookie;
        }
        if (newCookies.isEmpty()) return cookieList;
        return newCookies;
    }

    /**
     * Generate a new connection
     *
     * @param url The target url
     * @param method The connection method
     * @return A new HttpURLConnection
     */
    @SneakyThrows
    private HttpURLConnection newConnection(String url, Connection.Method method) {
        URL ConnUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) ConnUrl.openConnection();
        connection.setRequestMethod(method.name());
        connection.setRequestProperty("user-agent", CloudflareScrapperV1.USER_AGENT);
        connection.setRequestProperty("accept", ACCEPT);
        connection.setRequestProperty("referer", SPIGOT_URL);
        connection.setRequestProperty("cookie", listToString(siteCookieList));
        connection.setUseCaches(false);
        return connection;
    }

    private class ScrapperCallback implements CloudflareScrapperV2.cfCallback {

        @SneakyThrows
        @Override
        public void onSuccess(List<HttpCookie> cookieList, boolean hasNewUrl, String newUrl, StringBuilder rawResponse) {
            if (cookieList == null) {
                // Website is not protected by CloudFlare
                siteCookieList.clear();
            } else {
                siteCookieList = getNewestCookie(cookieList);
            }
        }

        @Override
        public void onFail() {
        }
    }
}
