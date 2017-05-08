package ru.samlib.client.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import ru.kazantsev.template.activity.BaseActivity;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.R;
import ru.samlib.client.database.SuggestionProvider;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.fragments.*;

import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.parser.Parser;


public class MainActivity extends BaseActivity {

    public static final String ONLINE = "online";

    private boolean doubleBackToExitPressedOnce = false;

    private boolean online = false;

    public static MainActivity singleInstance;

    public static MainActivity getInstance() {
        return singleInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleInstance = this;
        if(!isConfigChange(savedInstanceState)) {
            online = AndroidSystemUtils.isNetworkAvailable(this);
        } else {
            online = savedInstanceState.getBoolean(ONLINE);
        }
        View header = getLayoutInflater().inflate(R.layout.header_main, navigationView, false);
        Button switchButton = GuiUtils.getView(header, R.id.header_main_status);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(!online);
            }
        });
        navigationView.addHeaderView(header);
        switchStatus(online);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ONLINE, online);
    }

    protected void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            if(Linkable.isSamlibLink(TextUtils.eraseHost(query))) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.Net.BASE_DOMAIN + TextUtils.eraseHost(query)));
                startActivity(i);
            }  else {
                Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
                searchIntent.putExtra(SearchManager.QUERY, query + " site:" + Constants.Net.BASE_HOST); // query contains search string
                startActivity(searchIntent);
            }
            //SearchFragment.show(getCurrentFragment(), query);  TODO: make own serchview
        }
        if (intent.getAction() == null && ObservableFragment.class.getSimpleName().equals(intent.getStringExtra(Constants.ArgsName.FRAGMENT_CLASS))) {
            replaceFragment(ObservableFragment.class);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // update the main content by replacing fragments
        Integer itemId = item.getItemId();
        switch (itemId) {
            case R.id.drawer_rate:
                replaceFragment(RateFragment.class);
                break;
            case R.id.drawer_top:
                replaceFragment(TopAuthorsFragment.class);
                break;
            case R.id.drawer_new:
                replaceFragment(NewestFragment.class);
                break;
            case R.id.drawer_discuss:
                replaceFragment(DiscussionFragment.class);
                break;
            case R.id.drawer_review:
                replaceFragment(GenreFragment.class, newFragmentBuilder().putArg(Constants.ArgsName.Type, Genre.LITREVIEW));
                break;
            case R.id.drawer_history:
                replaceFragment(HistoryFragment.class);
                break;
            case R.id.drawer_observable:
                replaceFragment(ObservableFragment.class);
                break;
            default:
                replaceFragment(BaseFragment.class);
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 0) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            showSnackbar(R.string.back_to_exit);
            new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
        } else {
            super.onBackPressed();
        }
    }


    public void switchMode(boolean online) {
        if (online != this.online) {
            boolean check = AndroidSystemUtils.isNetworkAvailable(this);
            if (!check && online) {
                GuiUtils.toast(this, R.string.network_not_available);
            } else {
               switchStatus(online);
            }
        }
    }

    private void switchStatus(boolean online) {
        this.online = online;
        Parser.setCachedMode(!online);
        int id = getCheckedNavigationItem();
        navigationView.getMenu().clear();
        if (online) {
            navigationView.inflateMenu(R.menu.drawer);
        } else {
            navigationView.inflateMenu(R.menu.drawer_offline);
        }
        if(id == -1) {
            id = getCheckedNavigationItem();
        }
        if (navigationView.getMenu().findItem(id) != null) {
            navigationView.setCheckedItem(id);
            onNavigationItemSelected(navigationView.getMenu().findItem(id));
        } else {
            onNavigationItemSelected(navigationView.getMenu().findItem(getCheckedNavigationItem()));
        }
        Button switchButton = GuiUtils.getView(navigationView.getHeaderView(0), R.id.header_main_status);
        if(online) {
            switchButton.setText(R.string.online);
            switchButton.setTextColor(getResources().getColor(R.color.light_blue));
        } else {
            switchButton.setText(R.string.offline);
            switchButton.setTextColor(getResources().getColor(R.color.light_grey));
        }
    }

}
