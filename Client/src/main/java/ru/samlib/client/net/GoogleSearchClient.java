package ru.samlib.client.net;

import android.util.Log;
import com.google.gson.Gson;
import org.jsoup.helper.W3CDom;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.google.ResponseData;
import ru.samlib.client.domain.google.Result;
import ru.samlib.client.lister.Lister;
import ru.samlib.client.util.SystemUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 07.07.2015.
 */
public class GoogleSearchClient implements Lister<Link> {

    private static final Integer page_size = 10;
    private final String query;

    //   private static final String HIDDEN_GOOGLE_URL = "http://www.google.com/uds/GwebSearch?v=1.0";
    private static final String TAG = GoogleSearchClient.class.getSimpleName();
    private static final String GOOGLE_URL = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
    private static final String CHARSET = "UTF-8";

    public GoogleSearchClient(String query) {
        this.query = query;
    }

    public GoogleResults search(String query, String site, int page) {
        URL url = null;
        try {
            query = URLEncoder.encode(query, CHARSET);
            query = "site:" + site + "+" + query + "&start=" + page;
            url = new URL(GOOGLE_URL + query);
            Reader reader = new InputStreamReader(url.openStream(), CHARSET);
            return new Gson().fromJson(reader, GoogleResults.class);
        } catch (Exception e) {
            Log.e(TAG, "Google Search exception");
            Log.w(TAG, e);
        }
        return null;
    }

    @Override
    public List<Link> getItems(int skip, int size) throws IOException {
        List<Link> links = new ArrayList<>();
        while (links.size() < size) {
            int index = skip / page_size;
            GoogleResults results = search(query, Link.getBaseDomain(), index);
            if(results == null) {
                throw new IOException("Google Service is not available");
            }
            if(results.getResponseData().getCursor() != null) {
                String count = results.getResponseData().getCursor().getResultCount();
                if (SystemUtils.parseInt(count) <= index) {
                    break;
                }
                List<Result> resultList = results.getResponseData().getResults();
                if (resultList == null || resultList.isEmpty()) {
                    break;
                } else {
                    for (Result result : resultList) {
                        links.add(new Link(result.getTitle(), result.getUrl(), result.getContent()));
                    }
                }
                skip += links.size();
            } else {
                break;
            }
        }
        return links;
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

    }
}
