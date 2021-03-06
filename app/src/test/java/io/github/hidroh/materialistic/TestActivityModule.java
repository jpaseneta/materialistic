package io.github.hidroh.materialistic;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.MenuRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAccountManager;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.FeedbackClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ItemSyncService;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.RestServiceFactory;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.test.TestFavoriteActivity;
import io.github.hidroh.materialistic.test.TestItemActivity;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;
import io.github.hidroh.materialistic.test.WebActivity;
import io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.PopupMenu;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Module(
        injects = {
                // source classes
                LoginActivity.class,
                SettingsActivity.class,
                AskActivity.class,
                AboutActivity.class,
                FavoriteActivity.class,
                FeedbackActivity.class,
                ItemActivity.class,
                JobsActivity.class,
                ListActivity.class,
                BestActivity.class,
                NewActivity.class,
                SearchActivity.class,
                ShowActivity.class,
                WebActivity.class,
                PopularActivity.class,
                ComposeActivity.class,
                SubmitActivity.class,
                UserActivity.class,
                ThreadPreviewActivity.class,
                FavoriteFragment.class,
                ItemFragment.class,
                ListFragment.class,
                WebFragment.class,
                ReadabilityFragment.class,
                ReleaseNotesActivity.class,
                StoryRecyclerViewAdapter.class,
                FavoriteRecyclerViewAdapter.class,
                SinglePageItemRecyclerViewAdapter.class,
                MultiPageItemRecyclerViewAdapter.class,
                SubmissionRecyclerViewAdapter.class,
                ThreadPreviewRecyclerViewAdapter.class,
                ItemSyncService.class,
                // test classes
                AppUtilsTest.class,
                SettingsActivityTest.class,
                SearchActivityTest.class,
                ItemActivityTest.class,
                ItemFragmentMultiPageTest.class,
                ItemFragmentSinglePageTest.class,
                TestItemActivity.class,
                TestReadabilityActivity.class,
                TestListActivity.class,
                io.github.hidroh.materialistic.test.ListActivity.class,
                ListFragmentViewHolderTest.class,
                ListFragmentViewHolderEdgeTest.class,
                FavoriteActivityTest.class,
                FavoriteActivityEmptyTest.class,
                TestFavoriteActivity.class,
                WebFragmentLocalTest.class,
                WebFragmentTest.class,
                FeedbackActivityTest.class,
                ListFragmentTest.class,
                PopularActivityTest.class,
                ReadabilityFragmentTest.class,
                ReadabilityFragmentLazyLoadTest.class,
                LoginActivityTest.class,
                DrawerActivityLoginTest.class,
                ComposeActivityTest.class,
                SubmitActivityTest.class,
                UserActivityTest.class,
                ThreadPreviewActivityTest.class
        },
        library = true,
        overrides = true
)
public class TestActivityModule {
    private final ItemManager hackerNewsClient = mock(ItemManager.class);
    private final ItemManager algoliaClient = mock(ItemManager.class);
    private final ItemManager algoliaPopularClient = mock(ItemManager.class);
    private final UserManager userManager = mock(UserManager.class);
    private final FavoriteManager favoriteManager = mock(FavoriteManager.class);
    private final SessionManager sessionManager = mock(SessionManager.class);
    private final SearchView searchView = mock(SearchView.class);
    private final FeedbackClient feedbackClient = mock(FeedbackClient.class);
    private final ReadabilityClient readabilityClient = mock(ReadabilityClient.class);
    private final UserServices userServices = mock(UserServices.class);
    private final CustomTabsDelegate customTabsDelegate = mock(CustomTabsDelegate.class);
    private final VolumeNavigationDelegate volumeNavigationDelegate = mock(VolumeNavigationDelegate.class);
    private final RestServiceFactory restServiceFactory = mock(RestServiceFactory.class);

    @Provides @Singleton @Named(ActivityModule.HN)
    public ItemManager provideHackerNewsClient() {
        return hackerNewsClient;
    }

    @Provides @Singleton @Named(ActivityModule.ALGOLIA)
    public ItemManager provideAlgoliaClient() {
        return algoliaClient;
    }

    @Provides @Singleton @Named(ActivityModule.POPULAR)
    public ItemManager provideAlgoliaPopularClient() {
        return algoliaPopularClient;
    }

    @Provides @Singleton
    public FavoriteManager provideFavoriteManager() {
        return favoriteManager;
    }

    @Provides @Singleton
    public SessionManager provideSessionManager() {
        return sessionManager;
    }

    @Provides @Singleton
    public FeedbackClient provideFeedbackClient() {
        return feedbackClient;
    }

    @Provides @Singleton
    public ReadabilityClient provideReadabilityClient() {
        return readabilityClient;
    }

    @Provides @Singleton
    public UserManager provideUserManager() {
        return userManager;
    }

    @Provides @Singleton
    public RestServiceFactory provideRestServiceFactory() {
        return restServiceFactory;
    }

    @Provides @Singleton
    public ActionViewResolver provideActionViewResolver() {
        ActionViewResolver resolver = mock(ActionViewResolver.class);
        when(resolver.getActionView(any(MenuItem.class))).thenReturn(searchView);
        return resolver;
    }

    @Provides
    public AlertDialogBuilder provideAlertDialogBuilder() {
        return new AlertDialogBuilder() {
            private AlertDialog.Builder builder;

            @Override
            public AlertDialogBuilder init(Context context) {
                builder = new AlertDialog.Builder(context);
                return this;
            }

            @Override
            public AlertDialogBuilder setTitle(int titleId) {
                builder.setTitle(titleId);
                return this;
            }

            @Override
            public AlertDialogBuilder setMessage(@StringRes int messageId) {
                builder.setMessage(messageId);
                return this;
            }

            @Override
            public AlertDialogBuilder setView(View view) {
                builder.setView(view);
                return this;
            }

            @Override
            public AlertDialogBuilder setSingleChoiceItems(CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
                builder.setSingleChoiceItems(items, checkedItem, listener);
                return this;
            }

            @Override
            public AlertDialogBuilder setNegativeButton(@StringRes int textId,
                                                        DialogInterface.OnClickListener listener) {
                builder.setNegativeButton(textId, listener);
                return this;
            }

            @Override
            public AlertDialogBuilder setPositiveButton(@StringRes int textId,
                                                        DialogInterface.OnClickListener listener) {
                builder.setPositiveButton(textId, listener);
                return this;
            }

            @Override
            public Dialog create() {
                return builder.create();
            }

            @Override
            public Dialog show() {
                return builder.show();
            }
        };
    }

    @Provides @Singleton
    public UserServices provideUserServices() {
        return userServices;
    }

    @Provides
    public AccountManager provideAccountManager() {
        return ShadowAccountManager.get(RuntimeEnvironment.application);
    }

    @Provides @Singleton
    public VolumeNavigationDelegate provideVolumeNavigationDelegate() {
        return volumeNavigationDelegate;
    }

    @Provides
    public PopupMenu providePopupMenu() {
        return new PopupMenu() {
            private android.widget.PopupMenu popupMenu;

            @SuppressLint("NewApi")
            @Override
            public void create(Context context, View anchor, int gravity) {
                popupMenu = new android.widget.PopupMenu(context, anchor, gravity);
            }

            @SuppressLint("NewApi")
            @Override
            public void inflate(@MenuRes int menuRes) {
                popupMenu.inflate(menuRes);
            }

            @SuppressLint("NewApi")
            @Override
            public Menu getMenu() {
                return popupMenu.getMenu();
            }

            @SuppressLint("NewApi")
            @Override
            public void setOnMenuItemClickListener(final OnMenuItemClickListener listener) {
                popupMenu.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return listener.onMenuItemClick(item);
                    }
                });
            }

            @SuppressLint("NewApi")
            @Override
            public void show() {
                popupMenu.show();
            }
        };
    }

    @Provides @Singleton
    public CustomTabsDelegate provideCustomTabsDelegate() {
        return customTabsDelegate;
    }
}
