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

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager;

public abstract class BaseListFragment extends BaseFragment implements Scrollable {
    private static final String STATE_ADAPTER = "state:adapter";
    @Inject CustomTabsDelegate mCustomTabsDelegate;
    private VolumeNavigationDelegate.RecyclerViewHelper mScrollableHelper;
    protected RecyclerView mRecyclerView;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_font,
                R.string.pref_text_size,
                R.string.pref_list_item_view);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {
            getAdapter().setCardViewEnabled(Preferences.isListItemCardView(getActivity()));
        } else {
            getAdapter().restoreState(savedInstanceState.getBundle(STATE_ADAPTER));
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setLayoutManager(new SnappyLinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        final int verticalMargin = getResources()
                .getDimensionPixelSize(R.dimen.cardview_vertical_margin);
        final int horizontalMargin = getResources()
                .getDimensionPixelSize(R.dimen.cardview_horizontal_margin);
        final int divider = getResources().getDimensionPixelSize(R.dimen.divider);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                if (getAdapter().isCardViewEnabled()) {
                    outRect.set(horizontalMargin, verticalMargin, horizontalMargin, 0);
                } else {
                    outRect.set(0, 0, 0, divider);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getAdapter().setCustomTabsDelegate(mCustomTabsDelegate);
        mRecyclerView.setAdapter(getAdapter());
        mScrollableHelper = new VolumeNavigationDelegate.RecyclerViewHelper(mRecyclerView,
                VolumeNavigationDelegate.RecyclerViewHelper.SCROLL_PAGE);
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_list) {
            showPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPreferences() {
        Bundle args = new Bundle();
        args.putInt(PopupSettingsFragment.EXTRA_XML_PREFERENCES, R.xml.preferences_list);
        ((DialogFragment) Fragment.instantiate(getActivity(),
                PopupSettingsFragment.class.getName(), args))
                .show(getFragmentManager(), PopupSettingsFragment.class.getName());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ADAPTER, getAdapter().saveState());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPreferenceObservable.unsubscribe(getActivity());
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

    private void onPreferenceChanged(int key, boolean contextChanged) {
        if (contextChanged) {
            mRecyclerView.setAdapter(getAdapter());
        } else if (key == R.string.pref_list_item_view) {
            getAdapter().setCardViewEnabled(Preferences.isListItemCardView(getActivity()));
        }
    }

    protected abstract ListRecyclerViewAdapter getAdapter();
}
