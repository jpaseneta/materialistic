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

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.widget.CommentItemDecoration;
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager;
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter;

public class UserActivity extends InjectableActivity implements Scrollable {
    public static final String EXTRA_USERNAME = UserActivity.class.getName() + ".EXTRA_USERNAME";
    private static final String STATE_USER = "state:user";
    private static final String PARAM_ID = "id";
    @Inject UserManager mUserManager;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManger;
    @Inject VolumeNavigationDelegate mVolumeNavigationDelegate;
    private VolumeNavigationDelegate.RecyclerViewHelper mScrollableHelper;
    private String mUsername;
    private UserManager.User mUser;
    private TextView mInfo;
    private TextView mAbout;
    private RecyclerView mRecyclerView;
    private TabLayout mTabLayout;
    private View mEmpty;
    private BottomSheetBehavior<View> mBottomSheetBehavior;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(mUsername) && getIntent().getData() != null) {
            if (TextUtils.equals(getIntent().getData().getScheme(), BuildConfig.APPLICATION_ID)) {
                mUsername = getIntent().getData().getLastPathSegment();
            } else {
                mUsername = getIntent().getData().getQueryParameter(PARAM_ID);
            }
        }
        if (TextUtils.isEmpty(mUsername)) {
            finish();
            return;
        }
        AppUtils.setStatusBarDim(getWindow(), true);
        setContentView(R.layout.activity_user);
        findViewById(R.id.touch_outside).setOnClickListener(v -> finish());
        mBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        finish();
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        AppUtils.setStatusBarDim(getWindow(), false);
                        mRecyclerView.setLayoutFrozen(false);
                        break;
                    default:
                        AppUtils.setStatusBarDim(getWindow(), true);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // no op
            }
        });
        ((TextView) findViewById(R.id.title)).setText(mUsername);
        mInfo = (TextView) findViewById(R.id.user_info);
        mAbout = (TextView) findViewById(R.id.about);
        mEmpty = findViewById(R.id.empty);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // no op
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                scrollToTop();
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new SnappyLinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new CommentItemDecoration(this));
        mScrollableHelper = new VolumeNavigationDelegate.RecyclerViewHelper(mRecyclerView,
                VolumeNavigationDelegate.RecyclerViewHelper.SCROLL_ITEM);
        if (savedInstanceState != null) {
            mUser = savedInstanceState.getParcelable(STATE_USER);
        }
        if (mUser == null) {
            load();
        } else {
            bind();
        }
        if (!AppUtils.hasConnection(this)) {
            Snackbar.make(findViewById(R.id.content_frame),
                    R.string.offline_notice, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVolumeNavigationDelegate.attach(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_USER, mUser);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVolumeNavigationDelegate.detach(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mVolumeNavigationDelegate.setScrollable(this, null);
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

    @Override
    public void scrollToTop() {
        mScrollableHelper.scrollToTop();
    }

    @Override
    public boolean scrollToNext() {
        return mScrollableHelper.scrollToNext();
    }

    @Override
    public boolean scrollToPrevious() {
        return mScrollableHelper.scrollToPrevious();
    }

    @Override
    protected boolean isTranslucent() {
        return true;
    }

    private void load() {
        mUserManager.getUser(mUsername, new UserResponseListener(this));
    }

    private void onUserLoaded(UserManager.User response) {
        if (response != null) {
            mUser = response;
            bind();
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        mInfo.setVisibility(View.GONE);
        mAbout.setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, 0, "").trim()));
    }

    private void bind() {
        mInfo.setText(getString(R.string.user_info, mUser.getCreated(this), mUser.getKarma()));
        if (TextUtils.isEmpty(mUser.getAbout())) {
            mAbout.setVisibility(View.GONE);
        } else {
            AppUtils.setTextWithLinks(mAbout, mUser.getAbout());
        }
        int count = mUser.getItems().length;
        mTabLayout.addTab(mTabLayout.newTab()
                .setText(getResources().getQuantityString(R.plurals.submissions_count, count, count)));
        mRecyclerView.setAdapter(new SubmissionRecyclerViewAdapter(mItemManger, mUser.getItems()));
        mRecyclerView.setLayoutFrozen(mBottomSheetBehavior.getState() !=
                BottomSheetBehavior.STATE_EXPANDED);
    }

    private static class UserResponseListener implements ResponseListener<UserManager.User> {
        private final WeakReference<UserActivity> mUserActivity;

        public UserResponseListener(UserActivity userActivity) {
            mUserActivity = new WeakReference<>(userActivity);
        }

        @Override
        public void onResponse(@Nullable UserManager.User response) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                mUserActivity.get().onUserLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            if (mUserActivity.get() != null && !mUserActivity.get().isActivityDestroyed()) {
                Toast.makeText(mUserActivity.get(), R.string.user_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
