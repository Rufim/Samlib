package ru.samlib.client.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.provider.SyncStateContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;
import ru.samlib.client.R;
import ru.samlib.client.database.SuggestionProvider;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Type;
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
            Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
            searchIntent.putExtra(SearchManager.QUERY, query + " site:" + Constants.Net.BASE_HOST); // query contains search string
            startActivity(searchIntent);
            //SearchFragment.show(getCurrentFragment(), query);  TODO: make own serchview
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // update the main content by replacing fragments
        Integer itemId = item.getItemId();
        switch (itemId) {
            case R.id.drawer_favorite:
                replaceFragment(item.getTitle().toString(), RateFragment.class);
                break;
            case R.id.drawer_top:
                replaceFragment(item.getTitle().toString(), TopAuthorsFragment.class);
                break;
            case R.id.drawer_new:
                replaceFragment(item.getTitle().toString(), NewestFragment.class);
                break;
            case R.id.drawer_discuss:
                replaceFragment(item.getTitle().toString(), DiscussionFragment.class);
                break;
            case R.id.drawer_review:
                replaceFragment(GenreFragment.class, new FragmentBuilder(getSupportFragmentManager())
                        .putArg(Constants.ArgsName.TITLE, item.getTitle().toString())
                        .putArg(Constants.ArgsName.Type, Genre.LITREVIEW));
                break;
            default:
                replaceFragment(item.getTitle().toString(), BaseFragment.class);
                break;
        }
        return false;
    }

    @Override
    protected void onDrawerClosed(View drawerView) {}

    @Override
    protected void onDrawerOpened(View drawerView) {}

    @Override
    public void onEvent(FragmentAttachedEvent fragmentAttached) {}

    protected <F extends BaseFragment> void replaceFragment(String title, Class<F> fragmentClass) {
        FragmentBuilder builder = new FragmentBuilder(getSupportFragmentManager());
        builder.putArg(Constants.ArgsName.TITLE, title);
        replaceFragment(fragmentClass);
    }


}
