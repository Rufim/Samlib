package ru.samlib.client.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import de.greenrobot.event.EventBus;
import org.jsoup.Connection;
import ru.samlib.client.R;
import ru.samlib.client.database.DatabaseHelper;
import ru.samlib.client.domain.events.Event;
import ru.samlib.client.domain.events.FragmentAttachedEvent;
import ru.samlib.client.fragments.*;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by 0shad on 11.07.2015.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Bind(R.id.container)
    protected FrameLayout container;
    @Bind(R.id.container_details)
    protected FrameLayout containerDetails;
    @Bind(R.id.drawer_layout)
    protected DrawerLayout drawerLayout;
    @Bind(R.id.navigation_drawer)
    protected NavigationView navigationView;
    @Bind(R.id.toolbar)
    protected Toolbar toolbar;
    @Bind(R.id.main_border)
    protected View mainBorder;

    protected DatabaseHelper databaseHelper = null;
    protected ActionBar actionBar;

    public interface BackCallback {
        boolean allowBackPress();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        actionBar = getSupportActionBar();
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            if (!menuItem.isChecked()) menuItem.setChecked(true);
            drawerLayout.closeDrawers();
            onNavigationItemSelected(menuItem);
            return true;
        });
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
                BaseActivity.this.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank
                super.onDrawerOpened(drawerView);
                BaseActivity.this.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!isConfigChange(savedInstanceState)) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.ArgsName.LAST_FRAGMENT_TAG, getCurrentFragment().getTag());
        outState.putBoolean(Constants.ArgsName.CONFIG_CHANGE, true);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
        super.onPause();
    }



    public DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null || !databaseHelper.isOpen()) {
            databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }


    @Override
    public void onBackPressed() {
        Fragment fr = getCurrentFragment();
        if (fr instanceof BackCallback) {
            if (((BackCallback) fr).allowBackPress()) super.onBackPressed();
        } else {
            super.onBackPressed();
        }

    }

    protected abstract void handleIntent(Intent intent);

    protected boolean isConfigChange(@Nullable Bundle savedInstanceState) {
        return savedInstanceState != null && savedInstanceState.getBoolean(Constants.ArgsName.CONFIG_CHANGE, false);
    }

    protected abstract boolean onNavigationItemSelected(MenuItem item);

    protected abstract void onDrawerClosed(View drawerView);

    protected abstract void onDrawerOpened(View drawerView);

    protected void setContainerWeight(float weight) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) container.getLayoutParams();
        p.weight = weight;
        container.setLayoutParams(p);
    }

    protected void setDetailsWeight(float weight) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) containerDetails.getLayoutParams();
        p.weight = weight;
        containerDetails.setLayoutParams(p);
    }

    public abstract void onEvent(FragmentAttachedEvent fragmentAttached);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            // searchView.setOnQueryTextListener(this);
            // MenuItemCompat.setOnActionExpandListener(searchItem, this);
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
    }

    protected void postEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    protected String getResString(@StringRes int id) {
        return getResources().getString(id);
    }

    protected Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.container);
    }

    protected Fragment getLastFragment(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String lastTag = savedInstanceState.getString(Constants.ArgsName.LAST_FRAGMENT_TAG);
            if (lastTag != null) {
                FragmentManager manager = getSupportFragmentManager();
                return manager.findFragmentByTag(lastTag);
            }
        }
        return null;
    }

    public NavigationView getNavigationView() {
        return navigationView;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    @Nullable
    public ActionBar getCurrentActionBar() {
        return actionBar;
    }


    // Использовать при изменении ориентации экрана.
    public <F extends Fragment> void replaceFragment(Class<F> fragmentClass) {
        replaceFragment(fragmentClass, new FragmentBuilder(getSupportFragmentManager()));
    }

    public <F extends Fragment> void replaceFragment(Class<F> fragmentClass, FragmentBuilder builder) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            builder.addToBackStack();
        }
        builder.replaceFragment(R.id.container, fragmentClass);
        supportInvalidateOptionsMenu();
    }

    public <F extends Fragment> void replaceFragment(F fragment) {
        replaceFragment(fragment, new FragmentBuilder(getSupportFragmentManager()));
    }

    public <F extends Fragment> void replaceFragment(F fragment, FragmentBuilder builder) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            builder.addToBackStack();
        }
        builder.replaceFragment(R.id.container, fragment);
        supportInvalidateOptionsMenu();
    }


    public void showSnackbar(@StringRes int message) {
        GuiUtils.showSnackbar(container, message);
    }
}
