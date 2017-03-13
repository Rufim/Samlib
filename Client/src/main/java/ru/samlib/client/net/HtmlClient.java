package ru.samlib.client.net;

import android.util.Log;
import ru.samlib.client.App;
import ru.samlib.client.util.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
            connection.setConnectTimeout(3000);
            connection.setUseCaches(false);
        }

        @Override
        protected CachedResponse prepareResponse() throws IOException {
            File cacheDir = App.getInstance().getExternalCacheDir();
            String fileName = request.getBaseUrl().getPath();
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("/"));
            }
            String hash = "";
            if (request.isWithParams()) {
                hash = "." + Integer.toHexString(request.hashCode());
            }
            if (!TextUtils.contains(fileName, false, ".shtml", ".html", ".htm")) {
                fileName += hash + ".html";
            }
            if(fileName.endsWith(".shtml")) {
                fileName = fileName.substring(0, fileName.lastIndexOf(".shtml")) + ".html";
            }
            CachedResponse cachedResponse = new CachedResponse(cacheDir, fileName, request);
            htmlfiles.put(request.hashCode(), cachedResponse);
            if (cachedResponse.exists()) {
                cachedResponse.delete();
            }
            cachedResponse.getParentFile().mkdirs();
            if(cachedResponse.createNewFile()) {
                return cachedResponse;
            } else {
                return null;
            }
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

    public static long getContentLength(String url) throws IOException {
        return new URL(url).openConnection().getContentLength();
    }

}
