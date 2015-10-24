package ru.samlib.client.parser;

import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.util.Log;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.net.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rufim on 03.01.2015.
 */
public abstract class Parser {

    protected static final String TAG = Parser.class.getSimpleName();

    protected static final int MIN_BODY_SIZE = 1024 * 50;

    private static final String ACCEPT_VALUE = "text/html";
    private static final String ACCEPT_ENCODING_VALUE = "gzip, deflate";
    private static final String USER_AGENT = "Mozilla";

    private static LruCache<Request, Document> parserCache = new LruCache<>(20);
    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    protected Request request;

    public void setPath(String path) throws MalformedURLException {
        if (path == null) {
            throw new MalformedURLException("Link is NULL");
        }
        this.request = new Request(Link.getBaseDomain() + path)
                .setEncoding("CP1251")
                .addHeader("Accept", ACCEPT_VALUE)
                .addHeader("User-Agent", USER_AGENT);
    }

    public Document getDocument(Request request) throws IOException {
        return getDocument(request, Long.MAX_VALUE);
    }

    public Document getDocument(Request request, long minBodySize) throws IOException {
        CachedResponse htmlFile = HtmlClient.executeRequest(request, minBodySize);

        Document doc = null;

        if (htmlFile != null) {

            if (htmlFile.isCached && (doc = parserCache.get(request)) != null) {
                return doc;
            }

            boolean isOver = htmlFile.isDownloadOver;
            try {
                doc = Jsoup.parse(htmlFile, request.getEncoding(), request.getUrl().toString());
            } catch (Exception ex) {
                Log.w(TAG, "Url is not exist or not have valid content: " + request);
                return null;
            }
            if (isOver) {
                if (!htmlFile.isCached) {
                    parserCache.put(request, doc);
                    htmlFile.isCached = true;
                }
            } else {
                executor.submit(new continueToParse(htmlFile));
            }
        }
        return doc;
    }

    public static void dropCache() {
        parserCache.evictAll();
    }

    private class continueToParse implements Callable<Boolean> {

        private CachedResponse htmlFile;

        public continueToParse(CachedResponse htmlFile) {
            this.htmlFile = htmlFile;
        }

        @Override
        public Boolean call() throws Exception {
            while (!htmlFile.isDownloadOver) {
                SystemClock.sleep(50);
            }
            String url = htmlFile.getRequest().getUrl().toString();
            parserCache.put(htmlFile.getRequest(), Jsoup.parse(htmlFile, htmlFile.getEncoding(), url));
            htmlFile.isCached = true;
            return Boolean.TRUE;
        }
    }

}
