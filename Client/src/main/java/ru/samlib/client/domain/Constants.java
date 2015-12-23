package ru.samlib.client.domain;

import org.intellij.lang.annotations.RegExp;

/**
 * Created by Rufim on 07.01.2015.
 */
public class Constants {

    public static class Assets {
        public static final String ROBOTO_FONT_PATH = "fonts/roboto/Roboto-Regular.ttf";
        public static final String DROID_SANS_FONT_PATH = "fonts/droidsans/DroidSans.ttf";
        public static final String ROBOTO_Capture_it = "fonts/Capture-it/Capture_it.ttf";

    }

    public static class ArgsName {
        public static final String LAST_FRAGMENT_TAG = "last_fragment_tag";
        public static final String LAST_FRAGMENT = "last_fragment";
        public static final String FRAGMENT_CLASS = "fragment_class";
        public static final String FRAGMENT_ARGS = "fragment_args";
        public static final String SEARCH_QUERY = "search_query";
        public static final String AUTHOR = "author";
        public static final String LINK = "link";
        public static final String Type = "type";
        public static final String IMAGE = "image";
        public static final String TITLE = "title";
        public static final String CONFIG_CHANGE = "config_change";
        public static final String WORK = "work";
        public static final String MESSAGE = "message";
        public static final String RESOURCE_ID = "resource_id";
        public static final String TTS_PLAY_POSITION = "position";
        public static final String COMMENTS_PAGE = "comments_page";
    }

    public static class Net {
        public static final String BASE_SCHEME = "http";
        public static final String BASE_HOST = "budclub.ru";
        public static final String BASE_DOMAIN = "http://budclub.ru";
        public static final String USER_AGENT = "Mozilla";
    }

    public static class Cache {
        public static final String CACHE_NAME = "html_cache";
        public static final String CACHE_ASYNC = "async_html_cache";
        public static final int RAM_MAX_SIZE = 1024 * 1024 * 20;
        public static final int DISK_MAX_SIZE = 1024 * 1024 * 50;
    }

    public static class App {
        public static final int APP_VERSION = 1;
    }

    public static class Pattern {
        public static final String TIME_PATTERN = "HH:mm";
        public static final String DATA_PATTERN = "dd-MM-yyyy";
        public static final String DATA_TIME_PATTERN = "dd-MM-yyyy HH:mm";
        @RegExp
        public static final String WORK_URL_REGEXP = "/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+\\.shtml";
        @RegExp
        public static final String AUTHOR_URL_REGEXP = "/*[a-z]/+[a-z_0-9]+(/*)";
        @RegExp
        public static final String COMMENTS_URL_REGEXP = "/comment/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+";
        @RegExp
        public static final String ILLUSTRATIONS_URL_REGEXP = "/img/*[a-z]/+[a-z_0-9]+/+[a-z-_0-9]+/index\\.shtml";
    }

}
