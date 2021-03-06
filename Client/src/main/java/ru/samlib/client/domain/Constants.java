package ru.samlib.client.domain;

import org.intellij.lang.annotations.RegExp;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.BuildConfig;

/**
 * Created by Rufim on 07.01.2015.
 */
public class Constants {

    public static class Assets {
        public static final String ROBOTO_FONT_PATH = "fonts/roboto/Roboto.ttf";
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
        public static final String TTS_SPEECH_RATE = "speechRate";
        public static final String TTS_PITCH = "pitch";
        public static final String TTS_LANGUAGE = "language";
        public static final String COMMENTS_PAGE = "comments_page";
        public static final String COMMENTS_ARCHIVE = "comments_archive";
        public static final String FILE_PATH = "file_path";
        public static final String CONTENT_URI = "content";
        public static final String WORK_RESTORE = "from_notification";
        public static final String ON_CHANGE_THEME = "onChangeTheme";
    }


    public static class Net {
        public static final String BASE_SCHEME = BuildConfig.BASE_SCHEME;
        public static final String BASE_HOST = BuildConfig.BASE_HOST;
        public static final String BASE_DOMAIN = BASE_SCHEME + "://" + BASE_HOST;
        public static final String USER_AGENT = "Mozilla";
        public static final String STAT_SERVER = BuildConfig.STAT_SERVER;
        public static final String STAT_SERVER_DOMAIN = BASE_SCHEME + "://" + BuildConfig.STAT_SERVER;
        public static final String STAT_SERVER_UPDATE = STAT_SERVER_DOMAIN + "/update_date";
        public static final String LOG_PATH = BASE_DOMAIN + "/logs";
    }

    public static class Cache {
        public static final String CACHE_NAME = "html_cache";
        public static final String CACHE_ASYNC = "async_html_cache";
        public static final int RAM_MAX_SIZE = 1024 * 1024 * 20;
        public static final int DISK_MAX_SIZE = 1024 * 1024 * 50;
    }


    public static class App {
        public static final int VERSION = BuildConfig.VERSION_CODE;
        public static final String VERSION_NAME = BuildConfig.VERSION_NAME;
        public static final String DATABASE_NAME = "Samlib";
        public static final int DATABASE_VERSION = 22;
    }

    public static class Pattern {
        public static final String TIME_PATTERN = "HH:mm";
        public static final String DATA_PATTERN = "dd-MM-yyyy";
        public static final String DATA_PATTERN_LOG = "dd/MM/yyyy";
        public static final String DATA_TIME_PATTERN = "dd-MM-yyyy HH:mm";
        public static final String DATA_ISO_8601_24H = "yyyy-MM-dd HH:mm:ss";
        public static final String DATA_PATTERN_DIFF = "dd.MM.yyyy";
        public static final String DATA_ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        public static final String DATA_ISO_8601_24H_FULL_FORMAT_WITHOUT_MC = "yyyy-MM-dd'T'HH:mm:ss'Z'";
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
