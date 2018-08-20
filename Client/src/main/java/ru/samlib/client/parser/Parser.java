package ru.samlib.client.parser;

import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.Constants;
import ru.kazantsev.template.net.CachedResponse;
import ru.samlib.client.net.HtmlClient;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rufim on 03.01.2015.
 */
public abstract class Parser {

    protected static final String TAG = Parser.class.getSimpleName();

    protected static final int MIN_BODY_SIZE = 1024 * 50;

    protected static final String ACCEPT_VALUE = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    protected static final String ACCEPT_ENCODING_VALUE = "gzip, deflate";
    protected static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0";

    private static LruCache<Request, Document> parserCache = new LruCache<>(1);
    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    protected Request request;
    protected CachedResponse htmlFile;
    protected Document document;
    protected boolean lazyLoad = false;
    protected Set<Request> loadedSet = new HashSet<>();
    protected static boolean cached = false;
    protected static String commentCookie = null;
    protected static String voteCookie = null;
    protected static String loginCookie = null;

    public void setPath(String path) throws MalformedURLException {
        if (path == null) {
            throw new MalformedURLException("Link is NULL");
        }
        try {
            this.request = new Request(Constants.Net.BASE_DOMAIN + path)
                    .setEncoding("CP1251")
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT);
            if (hasCookieComment() || hasLogin()) {
                request.addHeader("Cookie", getCookie());
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unknown exception", e);
        }
    }

    protected static String getCookie() {
        StringBuilder cookie = new StringBuilder();
        if (hasLogin()) {
            cookie.append(loginCookie);
            String comment = HTTPExecutor.parseParamFromHeader(loginCookie, "COMMENT");
            if(TextUtils.notEmpty(comment)) {
                return cookie.toString();
            }
        }
        if (hasCookieComment()) {
            cookie.append("COMMENT=" + commentCookie);
        }
        return cookie.toString();
    }

    public Document getDocument(Request request) throws IOException {
        return getDocument(request, Long.MAX_VALUE, false);
    }

    public Document getDocument(Request request, long minBodySize) throws IOException {
        return getDocument(request, minBodySize, false);
    };

    public void setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
    }

    public Document getDocument(Request request, long minBodySize, boolean cached) throws IOException {

        if(lazyLoad && loadedSet.contains(request)){
            if(parserCache.get(request) != null) {
                return parserCache.get(request);
            } else {
                loadedSet.remove(request);
            }
        }

        htmlFile = HtmlClient.executeRequest(request, minBodySize, cached || Parser.cached);

        document = null;

        if (htmlFile != null) {

            if (htmlFile.isCached() && (document = parserCache.get(request)) != null) {
                return document;
            }

            boolean isOver = htmlFile.isDownloadOver();
            try {
                document = Jsoup.parse(htmlFile, request.getEncoding(), request.getUrl().toString());
                Log.i(TAG, "Document parsed: " + htmlFile.getAbsolutePath());
            } catch (Exception ex) {
                Log.w(TAG, "Url is not exist or not have valid content: " + request);
                throw new IOException("Network is not available", ex);
            }
            if (isOver) {
                if (!htmlFile.isCached()) {
                    parserCache.put(request, document);
                    htmlFile.setCached(true);
                    if(lazyLoad) {
                        loadedSet.add(request);
                    }
                }
            } else {
                executor.submit(new PendingParse(htmlFile));
            }
        } else {
            throw new IOException("Network is not available");
        }
        return document;
    }

    public static void setCommentCookie(String commentCookie) {
        Parser.commentCookie = commentCookie;
    }

    public static String getCommentCookie() {
        return commentCookie;
    }

    public static String getLoginCookie() {
        return loginCookie;
    }

    public static void setLoginCookie(String loginCookie) {
        Parser.loginCookie = loginCookie;
    }

    public static String getVoteCookie() {
        return voteCookie;
    }

    public static void setVoteCookie(String voteCookie) {
        Parser.voteCookie = voteCookie;
    }

    public static void dropCache() {
        parserCache.evictAll();
    }

    private class PendingParse implements Callable<Boolean> {

        private CachedResponse htmlFile;

        public PendingParse(CachedResponse htmlFile) {
            this.htmlFile = htmlFile;
        }

        @Override
        public Boolean call() throws Exception {
            while (!htmlFile.isDownloadOver()) {
                SystemClock.sleep(50);
            }
            String url = htmlFile.getRequest().getUrl().toString();
            parserCache.put(htmlFile.getRequest(), Jsoup.parse(htmlFile, htmlFile.getEncoding(), url));
            Log.i(TAG, "Document parsed: " + htmlFile.getAbsolutePath());
            htmlFile.setCached(true);
            return Boolean.TRUE;
        }
    }

    public static boolean hasCookieComment() {
        return commentCookie != null;
    }

    public static boolean hasLogin() {
        return loginCookie != null;
    }

    public static boolean hasCookieVote() {
        return voteCookie != null;
    }

    public static boolean isCachedMode() {
        return cached;
    }

    public static void setCachedMode(boolean cached) {
        Parser.cached = cached;
    }
}
