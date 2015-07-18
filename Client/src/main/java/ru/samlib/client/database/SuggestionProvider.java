package ru.samlib.client.database;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by Rufim on 30.06.2015.
 */
public class SuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "ru.samlib.client.database.SuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}