package ru.samlib.client.parser;

import android.accounts.NetworkErrorException;
import android.util.Log;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import ru.kazantsev.template.domain.Valuable;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.net.Response;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.lister.PageDataSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Rufim on 27.06.2015.
 */
public class SearchStatParser implements PageDataSource<Work> {

    protected static final String TAG = SearchStatParser.class.getSimpleName();
    protected Request request;
    protected Gson gson;

    public enum SortWorksBy {
        ACTIVITY("Активности"), RATING("Рейтингу"), VIEWS("Просмотрам");

        @Getter
        final String title;

        SortWorksBy(String title) {
            this.title = title;
        }

    }

    public enum SearchParams implements Valuable {
        query(""), genre(""), type(""), sort(""), page("");

        private final String defaultValue;

        SearchParams(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object value() {
            return defaultValue;
        }
    }

    public Request getRequest() {
        return request;
    }

    public SearchStatParser() throws MalformedURLException {
        try {
            this.request = new Request(Constants.Net.STAT_SERVER_DOMAIN + "/search-works")
                    .setEncoding("UTF-8")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Samlib Client");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unknown exception", e);
        }
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(java.util.Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.getTime());
            }
        });

        builder.registerTypeAdapter(java.util.Date.class, new JsonDeserializer<java.util.Date>() {
            @Override
            public java.util.Date deserialize(com.google.gson.JsonElement p1, java.lang.reflect.Type p2,
                                              com.google.gson.JsonDeserializationContext p3) {
                return new java.util.Date(p1.getAsLong());
            }
        });
        gson = builder.create();
        request.initParams(SearchParams.values());
    }

    public SearchStatParser(String query) throws MalformedURLException {
        this();
        request.addParam(SearchParams.query, query);
    }

    public void setQuery(String query) {
        request.addParam(SearchParams.query, query);
    }

    public void setFilters(String string, Genre genre, Type type, SortWorksBy sort) {
        if (string != null)request.addParam(SearchParams.query, string);
        if (type != null) request.addParam(SearchParams.type, type);
        if (genre != null) request.addParam(SearchParams.genre, genre);
        if (sort != null) request.addParam(SearchParams.sort, sort);
    }

    @Override
    public List<Work> getPage(int page) throws IOException  {
        if (HTTPExecutor.pingHost(Constants.Net.STAT_SERVER, 80, 10000)) {
            try {
                request.addParam(SearchParams.page, page);
                Response response = request.execute();
                return gson.fromJson(response.getRawContent(), new TypeToken<List<Work>>() {
                }.getType());
            } catch (Throwable ex) {
                throw new IOException(ex);
            }
        } else {
            throw new ConnectException("Server unreachable");
        }
    }


}
