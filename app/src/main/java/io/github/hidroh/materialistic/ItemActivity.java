/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic;

import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.ItemPagerAdapter;

public class ItemActivity extends InjectableActivity {

    public static final String EXTRA_ITEM = ItemActivity.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_CACHE_MODE = ItemActivity.class.getName() + ".EXTRA_CACHE_MODE";
    public static final String EXTRA_OPEN_COMMENTS = ItemActivity.class.getName() + ".EXTRA_OPEN_COMMENTS";
    private static final String PARAM_ID = "id";
    private static final String STATE_ITEM = "state:item";
    private static final String STATE_ITEM_ID = "state:itemId";
    private WebItem mItem;
    private String mItemId = null;
    private ImageView mBookmark;
    private boolean mExternalBrowser;
    private Preferences.StoryViewMode mStoryViewMode;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject FavoriteManager mFavoriteManager;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject UserServices mUserServices;
    @Inject SessionManager mSessionManager;
    @Inject CustomTabsDelegate mCustomTabsDelegate;
    @Inject VolumeNavigationDelegate mVolumeNavigationDelegate;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;
    private CoordinatorLayout mCoordinatorLayout;
    private ImageButton mVoteButton;
    private FloatingActionButton mReplyButton;
    private ItemPagerAdapter mAdapter;
    private ViewPager mViewPager;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mItem == null) {
                return;
            }
            if (FavoriteManager.isCleared(uri)) {
                mItem.setFavorite(false);
                bindFavorite();
            } else if (TextUtils.equals(mItemId, uri.getLastPathSegment())) {
                mItem.setFavorite(FavoriteManager.isAdded(uri));
                bindFavorite();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        if (getIntent().getBooleanExtra(EXTRA_OPEN_COMMENTS, false)) {
            mStoryViewMode = Preferences.StoryViewMode.Comment;
        } else {
            mStoryViewMode = Preferences.getDefaultStoryView(this);
        }
        setContentView(R.layout.activity_item);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mReplyButton = (FloatingActionButton) findViewById(R.id.reply_button);
        mVoteButton = (ImageButton) findViewById(R.id.vote_button);
        mBookmark = (ImageView) findViewById(R.id.bookmarked);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.content_frame);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        final Intent intent = getIntent();
        getContentResolver().registerContentObserver(MaterialisticProvider.URI_FAVORITE,
                true, mObserver);
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(STATE_ITEM);
            mItemId = savedInstanceState.getString(STATE_ITEM_ID);
        } else {
            if (Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction())) {
                if (intent.getData() != null) {
                    if (TextUtils.equals(intent.getData().getScheme(), BuildConfig.APPLICATION_ID)) {
                        mItemId = intent.getData().getLastPathSegment();
                    } else {
                        mItemId = intent.getData().getQueryParameter(PARAM_ID);
                    }
                }
            } else if (intent.hasExtra(EXTRA_ITEM)) {
                mItem = intent.getParcelableExtra(EXTRA_ITEM);
                mItemId = mItem.getId();
            }
        }

        if (mItem != null) {
            bindData(mItem);
        } else if (!TextUtils.isEmpty(mItemId)) {
            mItemManager.getItem(mItemId,
                    getIntent().getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT),
                    new ItemResponseListener(this));
        }
        if (!AppUtils.hasConnection(this)) {
            Snackbar.make(mCoordinatorLayout, R.string.offline_notice, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCustomTabsDelegate.bindCustomTabsService(this);
        mVolumeNavigationDelegate.attach(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_share).setVisible(mItem != null);
        return mItem != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openExternal(ItemActivity.this, mAlertDialogBuilder, mItem,
                    mCustomTabsDelegate.getSession());
            return true;
        }
        if (item.getItemId() == R.id.menu_share) {
            AppUtils.share(ItemActivity.this, mAlertDialogBuilder, mItem);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_ITEM, mItem);
        outState.putString(STATE_ITEM_ID, mItemId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabsDelegate.unbindCustomTabsService(this);
        mVolumeNavigationDelegate.detach(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mVolumeNavigationDelegate.setScrollable(getScrollable(), mAppBar);
        return mVolumeNavigationDelegate.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mVolumeNavigationDelegate.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mVolumeNavigationDelegate.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event);
    }

    private void onItemLoaded(@Nullable Item response) {
        mItem = response;
        supportInvalidateOptionsMenu();
        bindData(mItem);
    }

    private void bindFavorite() {
        if (mItem == null) {
            return;
        }
        if (!mItem.isStoryType()) {
            return;
        }
        mBookmark.setVisibility(View.VISIBLE);
        decorateFavorite(mItem.isFavorite());
        mBookmark.setOnClickListener(new View.OnClickListener() {
            private boolean mUndo;

            @Override
            public void onClick(View v) {
                final int toastMessageResId;
                if (!mItem.isFavorite()) {
                    mFavoriteManager.add(ItemActivity.this, mItem);
                    toastMessageResId = R.string.toast_saved;
                } else {
                    mFavoriteManager.remove(ItemActivity.this, mItem.getId());
                    toastMessageResId = R.string.toast_removed;
                }
                if (!mUndo) {
                    Snackbar.make(mCoordinatorLayout, toastMessageResId, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.undo, v1 -> {
                                mUndo = true;
                                mBookmark.performClick();
                            })
                            .show();
                }
                mUndo = false;
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void bindData(@Nullable final WebItem story) {
        if (story == null) {
            return;
        }
        mCustomTabsDelegate.mayLaunchUrl(Uri.parse(story.getUrl()), null, null);
        bindFavorite();
        mSessionManager.view(this, story.getId());
        mVoteButton.setVisibility(View.VISIBLE);
        mVoteButton.setOnClickListener(v -> vote(story));
        final TextView titleTextView = (TextView) findViewById(android.R.id.text2);
        if (story.isStoryType()) {
            titleTextView.setText(story.getDisplayedTitle());
            if (!TextUtils.isEmpty(story.getSource())) {
                TextView sourceTextView = (TextView) findViewById(R.id.source);
                sourceTextView.setText(story.getSource());
                sourceTextView.setVisibility(View.VISIBLE);
            }
        } else {
            AppUtils.setHtmlText(titleTextView, story.getDisplayedTitle());
        }

        final TextView postedTextView = (TextView) findViewById(R.id.posted);
        postedTextView.setText(story.getDisplayedTime(this, false, true));
        postedTextView.setMovementMethod(LinkMovementMethod.getInstance());
        switch (story.getType()) {
            case Item.JOB_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_white_18dp, 0, 0, 0);
                break;
            case Item.POLL_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_white_18dp, 0, 0, 0);
                break;
        }
        mViewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.divider));
        mViewPager.setPageMarginDrawable(R.color.blackT12);
        mAdapter = new ItemPagerAdapter(this, getSupportFragmentManager(),
                story, !mExternalBrowser,
                getIntent().getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT));
        mViewPager.setAdapter(mAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                mReplyButton.setImageResource(tab.getPosition() == 0 ?
                        R.drawable.ic_reply_white_24dp : R.drawable.ic_search_white_24dp);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Scrollable scrollable = getScrollable();
                if (scrollable != null) {
                    scrollable.scrollToTop();
                }
                mAppBar.setExpanded(true, true);
            }
        });
        switch (mStoryViewMode) {
            case Article:
                if (mViewPager.getAdapter().getCount() == 3) {
                    mViewPager.setCurrentItem(1);
                }
                break;
            case Readability:
                mViewPager.setCurrentItem(mViewPager.getAdapter().getCount() - 1);
                break;
        }
        if (story.isStoryType() && mExternalBrowser) {
            findViewById(R.id.header_card_view).setOnClickListener(v ->
                    AppUtils.openWebUrlExternal(ItemActivity.this,
                            story.getUrl(), mCustomTabsDelegate.getSession()));
        } else {
            findViewById(R.id.header_card_view).setClickable(false);
        }
        mReplyButton.setOnClickListener(v -> mAdapter.onFabClick(mReplyButton, mViewPager.getCurrentItem()));
    }

    private void decorateFavorite(boolean isFavorite) {
        mBookmark.setImageResource(isFavorite ?
                R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_border_white_24dp);
    }

    private Scrollable getScrollable() {
        return mAdapter != null ? (Scrollable) mAdapter.getItem(mViewPager.getCurrentItem()) : null;
    }
    private void vote(final WebItem story) {
        mUserServices.voteUp(ItemActivity.this, story.getId(), new VoteCallback(this));
    }

    private void onVoted(Boolean successful) {
        if (successful == null) {
            Toast.makeText(this, R.string.vote_failed, Toast.LENGTH_SHORT).show();
        } else if (successful) {
            Drawable drawable = DrawableCompat.wrap(mVoteButton.getDrawable());
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.greenA700));
            Toast.makeText(this, R.string.voted, Toast.LENGTH_SHORT).show();
        } else {
            AppUtils.showLogin(this, mAlertDialogBuilder);
        }
    }

    private static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<ItemActivity> mItemActivity;

        public ItemResponseListener(ItemActivity itemActivity) {
            mItemActivity = new WeakReference<>(itemActivity);
        }

        @Override
        public void onResponse(@Nullable Item response) {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onItemLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            // do nothing
        }
    }

    private static class VoteCallback extends UserServices.Callback {
        private final WeakReference<ItemActivity> mItemActivity;

        public VoteCallback(ItemActivity itemActivity) {
            mItemActivity = new WeakReference<>(itemActivity);
        }

        @Override
        public void onDone(boolean successful) {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onVoted(successful);
            }
        }

        @Override
        public void onError() {
            if (mItemActivity.get() != null && !mItemActivity.get().isActivityDestroyed()) {
                mItemActivity.get().onVoted(null);
            }
        }
    }
}
