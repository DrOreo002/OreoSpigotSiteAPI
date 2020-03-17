package me.droreo002.site.cloudflare;

import lombok.Getter;
import lombok.SneakyThrows;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO SSLSocketFactory setEnabledCipherSuites
//ECDHE-ECDSA-AES128-GCM-SHA256

public class CloudflareScrapperV2 {

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    private String mUrl;
    private String mUser_agent;
    private int mRetry_count;
    private URL ConnUrl;
    private List<HttpCookie> mCookieList;
    @Getter
    private CookieManager mCookieManager;
    private HttpURLConnection mCheckConn;
    private HttpURLConnection mGetMainConn;
    private HttpURLConnection mGetRedirectionConn;

    public static final int MAX_COUNT = 3;
    public static final int CONN_TIMEOUT = 60000;
    public static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";

    private boolean canVisit = false;
    private boolean hasNewUrl = false;  //when cf return 301 you need to change old url to new url;
    @Getter
    private boolean cloudFlared = true;

    public CloudflareScrapperV2(String url) {
        mUrl = url;
    }

    public CloudflareScrapperV2(String url, String user_agent) {
        mUrl = url;
        mUser_agent = user_agent;
    }

    public String getUser_agent() {
        return mUser_agent;
    }

    public void setUser_agent(String user_agent) {
        mUser_agent = user_agent;
    }

    public Future<Void> start(final cfCallback callback) {
        return THREAD_POOL.submit(() -> {
            urlThread(callback);
            return null;
        });
    }

    @SneakyThrows
    private void urlThread(cfCallback callback){
        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL); //接受所有cookies
        CookieHandler.setDefault(mCookieManager);
        HttpURLConnection.setFollowRedirects(false);
        StringBuilder response = new StringBuilder();

        cloudFlared = !(checkUrl() == 200); // Cloud flared is false if response code is 200

        while (!canVisit && cloudFlared) {
            if (mRetry_count > MAX_COUNT) {
                throw new IllegalStateException("Failed to bypass CloudFlare!");
            }
            try {
                int responseCode = checkUrl();
                if (responseCode == 200) {
                    canVisit = true;
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            mCheckConn.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    break;
                } else {
                    getVisitCookie();
                }
            } catch (IOException| RuntimeException | InterruptedException e) {
                if (mCookieList != null){
                    mCookieList= new ArrayList<>(mCookieList);
                    mCookieList.clear();
                }
                e.printStackTrace();
            } finally {
                closeAllConn();
            }
            mRetry_count++;
        }
        if (callback != null) {
            if (canVisit || !cloudFlared) {
                callback.onSuccess(mCookieList, hasNewUrl, mUrl, response);
            } else {
                System.out.println("Failed to bypass CloudFlare. Maybe its not protected?");
                callback.onFail();
            }
        }
    }

    private void getVisitCookie() throws IOException, InterruptedException {
        ConnUrl = new URL(mUrl);
        mGetMainConn = (HttpURLConnection) ConnUrl.openConnection();
        mGetMainConn.setRequestMethod("GET");
        mGetMainConn.setConnectTimeout(CONN_TIMEOUT);
        mGetMainConn.setReadTimeout(CONN_TIMEOUT);
        if (!mUser_agent.isEmpty()) {
            mGetMainConn.setRequestProperty("user-agent",mUser_agent);
        }
        mGetMainConn.setRequestProperty("accept",ACCEPT);
        mGetMainConn.setRequestProperty("referer", mUrl);
        if (mCookieList!=null&&mCookieList.size()>0){
            mGetMainConn.setRequestProperty("cookie",listToString(mCookieList));
        }
        mGetMainConn.setUseCaches(false);
        mGetMainConn.connect();
        switch (mGetMainConn.getResponseCode()){
            case HttpURLConnection.HTTP_OK:
                e("MainUrl","visit website success");
                mCookieList = mCookieManager.getCookieStore().getCookies();
                checkCookie(mCookieList);
                return;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                mUrl = mGetMainConn.getHeaderField("Location");
                mCookieList = mCookieManager.getCookieStore().getCookies();
                checkCookie(mCookieList);
                e("MainUrl","HTTP 301 :"+mUrl);
                return;
            case HttpURLConnection.HTTP_FORBIDDEN:
                e("MainUrl","IP block or cookie err");
                return;
            case HttpURLConnection.HTTP_UNAVAILABLE:
                InputStream mInputStream = mCheckConn.getErrorStream();
                BufferedReader mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
                StringBuilder sb = new StringBuilder();
                String str;
                while ((str = mBufferedReader.readLine()) != null){
                    sb.append(str);
                }
                mInputStream.close();
                mBufferedReader.close();
                mCookieList = mCookieManager.getCookieStore().getCookies();
                str = sb.toString();
                getCheckAnswer(str);
                break;
            default:
                e("MainUrl","UnCatch Http code: "+mGetMainConn.getHeaderField("Location"));
                break;
        }
    }

    /**
     * 获取值并跳转获得cookies
     * @param str
     */
    private void getCheckAnswer(String str) throws InterruptedException, IOException,RuntimeException {
        AnswerBean bean = new AnswerBean();
        if (str.contains("POST")){
            bean.setMethod(AnswerBean.POST);

            ArrayList<String> param = (ArrayList<String>) regex(str,"<form id=\"challenge-form\" action=\"(.+?)\"");
            if (param == null || param.size() == 0){
                e("getPost param error");
                throw new RuntimeException("getPost param error");
            }
            bean.setHost("https://"+ConnUrl.getHost()+param.get(0));
            ArrayList<String> s = (ArrayList<String>) regex(str,"<input type=\"hidden\" name=\"(.+?)\" value=\"(.+?)\">");
            if (s != null && s.size() > 0){
                bean.getFromData().put(s.get(0),s.get(1).contains("input type=\"hidden\"") ? "" : s.get(1));
            }
            String jschl_vc = regex(str,"name=\"jschl_vc\" value=\"(.+?)\"").get(0);
            String pass = regex(str,"name=\"pass\" value=\"(.+?)\"").get(0);
            bean.getFromData().put("jschl_vc",jschl_vc);
            bean.getFromData().put("pass",pass);
            double jschl_answer = get_answer(str);
            e(String.valueOf(jschl_answer));
            Thread.sleep(3000);
            bean.getFromData().put("jschl_answer",String.valueOf(jschl_answer));
        }else {
            bean.setMethod(AnswerBean.GET);

//            String s = regex(str,"name=\"s\" value=\"(.+?)\"").get(0);   //正则取值
//            String jschl_vc = regex(str,"name=\"jschl_vc\" value=\"(.+?)\"").get(0);
//            String pass = regex(str,"name=\"pass\" value=\"(.+?)\"").get(0);            //
//            double jschl_answer = get_answer(str);
//            e(String.valueOf(jschl_answer));
//            Thread.sleep(3000);
//            String req = "https://" + ConnUrl.getHost() +"/cdn-cgi/l/chk_jschl?";
//            if (!s.isEmpty()){
//                s = Uri.encode(s);
//                req+="s="+s+"&";
//            }
//            req+="jschl_vc="+Uri.encode(jschl_vc)+"&pass="+Uri.encode(pass)+"&jschl_answer="+jschl_answer;
//            bean.setHost(req);
        }
        e("RedirectUrl",bean.getHost());
        getRedirectResponse(bean);
    }

    private void getRedirectResponse(AnswerBean answerBean) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        mGetRedirectionConn = (HttpURLConnection) new URL(answerBean.getHost()).openConnection();

        mGetRedirectionConn.setRequestMethod(answerBean.getMethod() == AnswerBean.GET ? "GET" : "POST");
        mGetRedirectionConn.setConnectTimeout(CONN_TIMEOUT);
        mGetRedirectionConn.setReadTimeout(CONN_TIMEOUT);
        mGetRedirectionConn.setDoInput(true);
        mGetRedirectionConn.setDoOutput(true);
        mGetRedirectionConn.setUseCaches(false);
        if (!mUser_agent.isEmpty()){
            mGetRedirectionConn.setRequestProperty("user-agent",mUser_agent);
        }
        mGetRedirectionConn.setRequestProperty("accept",ACCEPT);
        mGetRedirectionConn.setRequestProperty("referer", mUrl);
        if (mCookieList!=null&&mCookieList.size()>0){
            mGetRedirectionConn.setRequestProperty("cookie",listToString(mCookieList));
        }
        mGetRedirectionConn.setUseCaches(false);
        if (answerBean.getMethod() == AnswerBean.POST){
            mGetRedirectionConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }
        mGetRedirectionConn.connect();
        if (answerBean.getMethod() == AnswerBean.POST){
            StringBuilder stringBuilder = new StringBuilder();
            for(Map.Entry<String, String> entry :answerBean.getFromData().entrySet()){
                stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(mGetRedirectionConn.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(stringBuilder.toString());
            writer.close();
        }
        switch (mGetRedirectionConn.getResponseCode()){
            case HttpURLConnection.HTTP_OK:
                mCookieList = mCookieManager.getCookieStore().getCookies();
                break;
            case HttpURLConnection.HTTP_MOVED_TEMP:
                mCookieList = mCookieManager.getCookieStore().getCookies();
                checkCookie(mCookieList);
                break;
            default:throw new IOException("getOtherResponse Code: "+
                    mGetRedirectionConn.getResponseCode());
        }
    }

    private void checkCookie(List<HttpCookie> cookieList) {
        if (cookieList == null || cookieList.size() <= 1){
            return;
        }
        List<HttpCookie> a = new ArrayList<>();
        HttpCookie newestCookie = null;
        for (HttpCookie httpCookie : cookieList) {
            if (!httpCookie.getName().equals("_cfduid")) {
                continue;
            }
            if (newestCookie == null) {
                newestCookie = httpCookie;
                continue;
            }
            a.add(newestCookie);
            newestCookie = httpCookie;
        }
        if (a.size()>0){
            cookieList.removeAll(a);
        }
    }

    private int checkUrl() throws IOException {
        URL ConnUrl = new URL(mUrl);
        mCheckConn = (HttpURLConnection) ConnUrl.openConnection();
        mCheckConn.setRequestMethod("GET");
        mCheckConn.setConnectTimeout(CONN_TIMEOUT);
        mCheckConn.setReadTimeout(CONN_TIMEOUT);
        if (!mUser_agent.isEmpty()){
            mCheckConn.setRequestProperty("user-agent",mUser_agent);
        }
        mCheckConn.setRequestProperty("accept",ACCEPT);
        mCheckConn.setRequestProperty("referer",mUrl);
        if (mCookieList!=null&&mCookieList.size()>0){
            mCheckConn.setRequestProperty("cookie",listToString(mCookieList));
        }
        mCheckConn.setUseCaches(false);
        mCheckConn.connect();
        return mCheckConn.getResponseCode();
    }

    private void closeAllConn(){
        if (mCheckConn!=null){
            mCheckConn.disconnect();
        }
        if (mGetMainConn!=null){
            mGetMainConn.disconnect();
        }
        if (mGetRedirectionConn!=null){
            mGetRedirectionConn.disconnect();
        }
    }


    public interface cfCallback{
        void onSuccess(List<HttpCookie> cookieList, boolean hasNewUrl, String newUrl, StringBuilder useThisHtmlIfNotWorking);
        void onFail();
    }

    private double get_answer(String str) {  //取值
        double a = 0;

        try {
            List<String> s = regex(str,"var s,t,o,p,b,r,e,a,k,i,n,g,f, " +
                    "(.+?)=\\{\"(.+?)\"");
            String varA = s.get(0);
            String varB = s.get(1);
            String div_cfdn = getCfdnDOM(str);
            List<String> eval_fuc = null;
            if (!div_cfdn.isEmpty()){
                eval_fuc = checkEval(str);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("var t=\"").append(new URL(mUrl).getHost()).append("\";");
            sb.append("var a=");
            sb.append(regex(str,varA+"=\\{\""+varB+"\":(.+?)\\}").get(0));
            sb.append(";");
            List<String> b = regex(str,varA+"\\."+varB+"(.+?)\\;");
            if (b != null) {
                for (int i =0; i < b.size() - 1; i++){
                    sb.append("a");
                    if (eval_fuc != null && eval_fuc.size() > 0) {
                        sb.append(replaceEval(b.get(i), div_cfdn, eval_fuc));
                    } else {
                        sb.append(b.get(i));
                    }
                    sb.append(";");
                }
            }

            e("add", sb.toString());
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

            Context cx = Context.enter();
            cx.setLanguageVersion(Context.VERSION_1_8);
            Scriptable scope = cx.initSafeStandardObjects();

            System.out.println("Executing: " + sb.toString());
            Object evaluated = cx.evaluateString(scope, sb.toString(), "MySource", 1, null);
            a = Double.parseDouble(String.valueOf(evaluated));
            System.out.println("    > Found answer of " + a);

            List<String> fixNum = regex(str,"toFixed\\((.+?)\\)");
            if (fixNum != null) {
                e("toFix",fixNum.get(0));
                a = Double.parseDouble((String) engine.eval("String("+ a +".toFixed("+fixNum.get(0)+"));"));
            }
            if (b != null && b.get(b.size() - 1).contains("t.length")) {
                a += new URL(mUrl).getHost().length();
            }
        } catch (IndexOutOfBoundsException e){
            e("answerErr","get answer error");
            e.printStackTrace();
        } catch (MalformedURLException | ScriptException e) {
            e.printStackTrace();
        }
        return a;
    }

    private String replaceEval(String s, String div_cfdn, List<String> eval_fuc) {
        List<String> eval = regex(s,"eval\\(eval\\((.+?)");
        if (eval==null||eval.size()==0){
            return s;
        }
        s = s.replace(eval_fuc.get(0),div_cfdn);
        s+=";"+eval_fuc.get(1);
        return s;
    }

    private List<String> checkEval(String str) {
        List<String> evalDom = regex(str,"function\\(p\\)\\{var p = (.+?)\\;(.+?)\\;");
        if (evalDom==null||evalDom.size()==0){
            return null;
        }else {
            return evalDom;
        }
    }

    private String getCfdnDOM(String str) {
        List<String> dom = regex(str,"k \\= \\'(.+?)\\'\\;");
        if (dom != null && dom.size() > 0){
            String cfdn = regex(str,"id=\""+dom.get(0)+"\">(.+?)</div>").get(0);
            if (!cfdn.isEmpty()){
                return cfdn;
            }else {
                return "";
            }
        }else {
            return "";
        }
    }

    /**
     * 正则
     * @param text 本体
     * @param pattern 正则式
     * @return List<String>
     */
    private List<String> regex(String text, String pattern){
        try {
            Pattern pt = Pattern.compile(pattern);
            Matcher mt = pt.matcher(text);
            List<String> group = new ArrayList<>();

            while (mt.find()) {
                if (mt.groupCount() >= 1) {
                    if (mt.groupCount()>1){
                        group.add(mt.group(1));
                        group.add(mt.group(2));
                    }else group.add(mt.group(1));
                }
            }
            return group;
        } catch (NullPointerException ignored){
            
        }
        return null;
    }

    /**
     * Convert a list into a single string, separated with ;
     * @return The string
     */
    public static String listToString(List<?> list) {
        if (list.isEmpty()) return "";
        char separator = ';';
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            sb.append(o).append(separator);
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    /**
     * 转换为jsoup可用的Hashmap
     * @param list  HttpCookie列表
     * @return Hashmap
     */
    public static Map<String,String> List2Map(List<HttpCookie> list){
        Map<String, String> map = new HashMap<>();
        try {
            if (list != null) {
                for (HttpCookie httpCookie : list) {
                    String[] listStr = httpCookie.toString().split("=");
                    map.put(listStr[0], listStr[1]);
                }
                
            } else {
                return map;
            }

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return map;
    }

    private void e(String tag, String content) {
    }

    private void e(String content) {
    }


    private static class AnswerBean {

        private String host;
        private int method;
        private Map<String, String> fromData = new HashMap<>();
        private static final int POST = 0x01;
        private static final int GET = 0x02;


        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getMethod() {
            return method;
        }

        public void setMethod(int method) {
            this.method = method;
        }

        public Map<String, String> getFromData() {
            return fromData;
        }

        public void setFromData(Map<String, String> fromData) {
            this.fromData = fromData;
        }
    }
}
