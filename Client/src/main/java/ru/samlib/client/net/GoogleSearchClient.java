package ru.samlib.client.net;

import android.util.Log;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by Rufim on 07.07.2015.
 */
public class GoogleSearchClient {

 //   private static final String HIDDEN_GOOGLE_URL = "http://www.google.com/uds/GwebSearch?v=1.0";
    private static final String TAG = GoogleSearchClient.class.getSimpleName();
    private static final String GOOGLE_URL = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
    private static final String CHARSET = "UTF-8";

    public GoogleResults search(String query, String site, int page) {
        URL url = null;
        try {
            query = "site:" + site + " " + query + "&start=" + page;
            url = new URL(GOOGLE_URL + URLEncoder.encode(query, CHARSET));
            Reader reader = new InputStreamReader(url.openStream(), CHARSET);
            return new Gson().fromJson(reader, GoogleResults.class);
        } catch (Exception e) {
            Log.e(TAG, "Google Search exception");
            Log.w(TAG, e);
        }
        return null;
    }

    public static class GoogleResults {

        private ResponseData responseData;

        public ResponseData getResponseData() {
            return responseData;
        }

        public void setResponseData(ResponseData responseData) {
            this.responseData = responseData;
        }

        public String toString() {
            return "ResponseData[" + responseData + "]";
        }

        static class ResponseData {
            private List<Result> results;

            public List<Result> getResults() {
                return results;
            }

            public void setResults(List<Result> results) {
                this.results = results;
            }

            public String toString() {
                return "Results[" + results + "]";
            }
        }

        public static class Result {
            private String url;
            private String title;

            public String getUrl() {
                return url;
            }

            public String getTitle() {
                return title;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String toString() {
                return "Result[url:" + url + ",title:" + title + "]";
            }
        }

    }
}
