package ru.samlib.client.net;

import android.util.Log;
import ru.samlib.client.SamlibApplication;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.concurrent.*;

/**
 * Created by Rufim on 25.06.2015.
 */
public class HtmlClient {

    private static final String TAG = HtmlClient.class.getSimpleName();

    private static HtmlClient instance;
    private static ExecutorService executor;
    private static Hashtable<Integer, CachedResponse> htmlfiles = new Hashtable<>(1);

    public static synchronized HtmlClient getInstance() {
        if (instance == null) {
            instance = new HtmlClient();
            executor = Executors.newCachedThreadPool();
        }
        return instance;
    }

    private HtmlClient() {
    }

    private class AsyncHtmlDownloader extends HTTPExecutor {

        public AsyncHtmlDownloader(Request request) {
            super(request);
        }

        @Override
        protected void configConnection(HttpURLConnection connection) {
        }

        @Override
        protected boolean prepareResponse() throws IOException {
            File cacheDir = SamlibApplication.getInstance().getExternalCacheDir();
            String fileName = request.getBaseUrl().getPath();
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("/"));
            }
            String hash = "";
            if (request.isWithParams()) {
                hash = "." + Integer.toHexString(request.hashCode());
            }
            if (!fileName.endsWith(".shtml") || !fileName.endsWith(".html")) {
                fileName += hash + ".shtml";
            }
            CachedResponse cachedResponse = new CachedResponse(cacheDir, fileName, request);
            htmlfiles.put(request.hashCode(), cachedResponse);
            if (cachedResponse.exists()) {
                cachedResponse.delete();
            }
            cachedResponse.getParentFile().mkdirs();
            this.cachedResponse = cachedResponse;
            return cachedResponse.createNewFile();
        }
    }

    public CachedResponse takeHtmlAsync(Request request, long minBytes) throws Exception {
        if (request.isSaveInCache()) {
            if (htmlfiles.containsKey(request.hashCode())) {
                return htmlfiles.get(request.hashCode());
            }
        }
        AsyncHtmlDownloader async = new AsyncHtmlDownloader(request);
        Future future = executor.submit(async);
        while (async.cachedResponse == null || async.cachedResponse.length() < minBytes) {
            try {
                return (CachedResponse) future.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                //ignore and try again
            }
        }
        return async.cachedResponse;
    }

    public static CachedResponse executeRequest(Request request) {
       return executeRequest(request, Long.MAX_VALUE);
    }

    public static CachedResponse executeRequest(Request request, long minBodySize) {
        CachedResponse cachedResponse = null;
        HtmlClient client = HtmlClient.getInstance();
        try {
            cachedResponse = client.takeHtmlAsync(request, minBodySize);
        } catch (Exception e) {
            Log.e(TAG, "Error when ", e);
        }
        return cachedResponse;
    }

}
