package eu.kanade.tachiyomi.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

import butterknife.Bind;
import butterknife.ButterKnife;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity;
import eu.kanade.tachiyomi.ui.catalogue.CatalogueFragment;
import eu.kanade.tachiyomi.ui.download.DownloadFragment;
import eu.kanade.tachiyomi.ui.library.LibraryFragment;
import eu.kanade.tachiyomi.ui.recent.RecentChaptersFragment;
import eu.kanade.tachiyomi.ui.setting.SettingsActivity;
import icepick.State;
import nucleus.view.ViewWithPresenter;

public class MainActivity extends BaseActivity {

    @Bind(R.id.appbar) AppBarLayout appBar;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.drawer_container) FrameLayout container;
    @State int selectedItem;
    private Drawer drawer;
    private FragmentStack fragmentStack;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Do not let the launcher create a new activity
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupToolbar(toolbar);

        fragmentStack = new FragmentStack(this, getSupportFragmentManager(), R.id.content_layout,
                fragment -> {
                    if (fragment instanceof ViewWithPresenter)
                        ((ViewWithPresenter) fragment).getPresenter().destroy();
                });

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withRootView(container)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withOnDrawerNavigationListener(view -> {
                    if (fragmentStack.size() > 1) {
                        onBackPressed();
                        return true;
                    }
                    return false;
                })
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.label_library)
                                .withIdentifier(R.id.nav_drawer_library),
                        new PrimaryDrawerItem()
                                .withName(R.string.label_recent_updates)
                                .withIdentifier(R.id.nav_drawer_recent_updates),
                        new PrimaryDrawerItem()
                                .withName(R.string.label_catalogues)
                                .withIdentifier(R.id.nav_drawer_catalogues),
                        new PrimaryDrawerItem()
                                .withName(R.string.label_download_queue)
                                .withIdentifier(R.id.nav_drawer_downloads),
                        new PrimaryDrawerItem()
                                .withName(R.string.label_settings)
                                .withIdentifier(R.id.nav_drawer_settings)
                                .withSelectable(false)
                )
                .withSavedInstance(savedState)
                .withOnDrawerItemClickListener(
                        (view, position, drawerItem) -> {
                            if (drawerItem != null) {
                                int identifier = drawerItem.getIdentifier();
                                switch (identifier) {
                                    case R.id.nav_drawer_library:
                                        setFragment(LibraryFragment.newInstance());
                                        break;
                                    case R.id.nav_drawer_recent_updates:
                                        setFragment(RecentChaptersFragment.newInstance());
                                        break;
                                    case R.id.nav_drawer_catalogues:
                                        setFragment(CatalogueFragment.newInstance());
                                        break;
                                    case R.id.nav_drawer_downloads:
                                        setFragment(DownloadFragment.newInstance());
                                        break;
                                    case R.id.nav_drawer_settings:
                                        startActivity(new Intent(this, SettingsActivity.class));
                                        break;
                                }
                            }
                            return false;
                        }
                )
                .build();

        if (savedState != null) {
            // Recover icon state after rotation
            if (fragmentStack.size() > 1) {
                showBackArrow();
            }

            // Set saved selection
            drawer.setSelection(selectedItem, false);
        } else {
            // Set default selection
            drawer.setSelection(R.id.nav_drawer_library);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        selectedItem = drawer.getCurrentSelection();
        super.onSaveInstanceState(outState);
    }

    public void setFragment(Fragment fragment) {
        fragmentStack.replace(fragment);
    }

    public void pushFragment(Fragment fragment) {
        fragmentStack.push(fragment);
        if (fragmentStack.size() > 1) {
            showBackArrow();
        }
    }

    @Override
    public void onBackPressed() {
        if (!fragmentStack.pop()) {
            super.onBackPressed();
        } else if (fragmentStack.size() == 1) {
            showHamburgerIcon();
            drawer.getActionBarDrawerToggle().syncState();
        }
    }

    private void showHamburgerIcon() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
            drawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private void showBackArrow() {
        if (getSupportActionBar() != null) {
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            drawer.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public AppBarLayout getAppBar() {
        return appBar;
    }

}
