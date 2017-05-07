package ru.samlib.client.net;

import android.util.Log;
import ru.kazantsev.template.net.*;
import ru.samlib.client.App;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.entity.SavedHtml;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Rufim on 25.06.2015.
 */
public class HtmlClient {

    private static final String TAG = HtmlClient.class.getSimpleName();

    private static HtmlClient instance;
    private static Hashtable<String, CachedResponse> htmlfiles = new Hashtable<>(1);
    private App app;

    public static synchronized HtmlClient getInstance() {
        if (instance == null) {
            instance = new HtmlClient(App.getInstance());
        }
        return instance;
    }

    private HtmlClient(App app) {
        this.app = app;
        List<SavedHtml> savedHtmls = app.getDataStore().select(SavedHtml.class).distinct().get().toList();
        for (SavedHtml savedHtml : savedHtmls) {
            String url = savedHtml.getUrl();
            try {
                Request request = new Request(url);
                CachedResponse cachedResponse = new CachedResponse(savedHtml.getFilePath(), request);
                cachedResponse.setEncoding("CP1251");
                htmlfiles.put(url, cachedResponse);
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception", e);
            }
        }
    }

    private class AsyncHtmlDownloader extends HTTPExecutor {

        public AsyncHtmlDownloader(Request request) {
            super(request);
        }

        @Override
        protected Response prepareResponse() throws IOException {
            File cacheDir = app.getExternalCacheDir();
            String fileName = request.getBaseUrl().getPath().replace("//", "/");
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
            cachedResponse.setEncoding("CP1251");
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

    public CachedResponse takeHtmlAsync(Request request, long minBytes, boolean cached) throws Exception {
        if (cached) {
            String url = getUrl(request);
            if (htmlfiles.containsKey(url)) {
                return htmlfiles.get(url);
            }
        }
        CachedResponse response = (CachedResponse) new AsyncHtmlDownloader(request).execute(request, minBytes);
        cache(response);
        return response;
    }

    public static CachedResponse executeRequest(Request request, boolean cached) throws IOException {
       return executeRequest(request, Long.MAX_VALUE, cached);
    }

    public static CachedResponse executeRequest(Request request, long minBodySize, boolean cached) throws IOException {
        CachedResponse cachedResponse = null;
        HtmlClient client = HtmlClient.getInstance();
        try {
            cachedResponse = client.takeHtmlAsync(request, minBodySize, cached);
        } catch (Exception e) {
            throw new IOException("Error when try to get html ", e);
        }
        return cachedResponse;
    }

    public static long getContentLength(String url) throws IOException {
        return new URL(url).openConnection().getContentLength();
    }


    private void cache(CachedResponse cachedResponse) throws UnsupportedEncodingException, MalformedURLException {
        if(!cachedResponse.getRequest().isWithParams()) {
            String url = getUrl(cachedResponse.getRequest());
            htmlfiles.put(url, cachedResponse);
            SavedHtml savedHtml = app.getDataStore().findByKey(SavedHtml.class, cachedResponse.getAbsolutePath());
            if (savedHtml == null) {
                savedHtml = new SavedHtml(cachedResponse);
                savedHtml.setSize(cachedResponse.length());
                savedHtml.setUrl(url);
                savedHtml.setUpdated(new Date());
                app.getDataStore().insert(savedHtml);
            } else {
                savedHtml.setSize(cachedResponse.length());
                savedHtml.setUrl(url);
                savedHtml.setUpdated(new Date());
                app.getDataStore().update(savedHtml);
            }
        }
    }

    private static String getUrl(Request request) throws UnsupportedEncodingException, MalformedURLException {
        URL url = request.getUrl();
        if(url.getPath().contains("//")) {
            return url.getProtocol() + "://" + url.getHost() + url.getPath().replace("//","/");
        } else {
            return url.toString();
        }
    }
}
