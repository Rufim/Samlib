package ru.samlib.client.domain;

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
        public static final String AUTHOR = "author";
        public static final String LINK = "link";
        public static final String TITLE = "title";
        public static final String WORK = "work";
        public static final String TTS_PLAY_POSITION = "position";
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

}
