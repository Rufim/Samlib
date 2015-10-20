package ru.samlib.client.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;
import ru.samlib.client.R;
import ru.samlib.client.database.SuggestionProvider;
import ru.samlib.client.domain.events.FragmentAttachedEvent;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;


public class MainActivity extends BaseActivity {


    private CharSequence title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMenuInflater().inflate(R.menu.drawer, navigationView.getMenu());
        if (savedInstanceState != null) {
            Fragment fr = getLastFragment(savedInstanceState);
            if (!navigationView.isShown()) {
                setTitle(fr.getArguments().getString(Constants.ArgsName.TITLE));
            }
        } else {
            replaceFragment(getResString(R.string.drawer_new), NewestFragment.class);
            setTitle(R.string.drawer_new);
        }
    }

    protected void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, SearchFragment.newInstance(query))
                    .commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // update the main content by replacing fragments
        Integer itemId = item.getItemId();
        if (itemId == R.id.drawer_favorite) {
            replaceFragment(item.getTitle().toString(), RateFragment.class);
        } else if (itemId == R.id.drawer_top) {
            replaceFragment(item.getTitle().toString(), TopAuthorsFragment.class);
        } else if (itemId == R.id.drawer_new) {
            replaceFragment(item.getTitle().toString(), NewestFragment.class);
        } else {
            replaceFragment(item.getTitle().toString(), BaseFragment.class);
        }
        return false;
    }

    @Override
    protected void onDrawerClosed(View drawerView) {
        setTitle(title);
    }

    @Override
    protected void onDrawerOpened(View drawerView) {
        setTitle(getTitle());
    }


    public void onEvent(FragmentAttachedEvent fragmentAttached) {
        title = fragmentAttached.fragment.getArguments().getString(Constants.ArgsName.TITLE);
    }

    protected <F extends BaseFragment> void replaceFragment(String title, Class<F> fragmentClass) {
        new FragmentBuilder(getSupportFragmentManager())
                .putArg(Constants.ArgsName.TITLE, title).
                replaceFragment(R.id.container, fragmentClass);
        supportInvalidateOptionsMenu();
    }


}
