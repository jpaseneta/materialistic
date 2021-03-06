package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.ShadowTextView;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestItemActivity;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ToggleItemViewHolder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowRecyclerView.class, ShadowSupportPreferenceManager.class, ShadowTextView.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ItemFragmentSinglePageTest {
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;
    @Captor ArgumentCaptor<UserServices.Callback> voteCallback;
    private RecyclerView recyclerView;
    private SinglePageItemRecyclerViewAdapter adapter;
    private ToggleItemViewHolder viewHolder;
    private ToggleItemViewHolder viewHolder1;
    private ToggleItemViewHolder viewHolder2;
    private TestItemActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        reset(userServices);
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        final TestHnItem item0 = new TestHnItem(1, 1);
        item0.populate(new TestItem() { // level 0
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{2L};
            }
        });
        final TestHnItem item1 = new TestHnItem(2, 2);
        item1.populate( new TestItem() { // level 1
            @Override
            public String getId() {
                return "2";
            }

            @Override
            public String getParent() {
                return "1";
            }

            @Override
            public boolean isDeleted() {
                return true;
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{3L};
            }
        });
        item0.getKidItems()[0] = item1;
        final TestHnItem item2 = new TestHnItem(3, 3);
        item2.populate(new TestItem() { // level 2
            @Override
            public String getId() {
                return "3";
            }

            @Override
            public long[] getKids() {
                return new long[0];
            }

            @Override
            public int getDescendants() {
                return 0;
            }

            @Override
            public String getParent() {
                return "2";
            }

            @Override
            public boolean isDead() {
                return true;
            }
        });
        item1.getKidItems()[0] = item2;
        TestHnItem story = new TestHnItem(0);
        story.populate(new TestItem() {
            @Override
            public long[] getKids() {
                return new long[]{1L};
            }

            @Override
            public int getDescendants() {
                return 1;
            }
        });
        story.getKidItems()[0] = item0;
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, story);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        activity = Robolectric.buildActivity(TestItemActivity.class)
                .create().start().resume().visible().get();
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .apply();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content_frame, fragment, ItemFragment.class.getName())
                .commit();
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        // auto expand all
        viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        viewHolder1 = adapter.createViewHolder(recyclerView, 1);
        adapter.bindViewHolder(viewHolder1, 1);
        viewHolder2 = adapter.createViewHolder(recyclerView, 2);
        adapter.bindViewHolder(viewHolder2, 2);
    }

    @Test
    public void testExpand() {
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testGetItemType() {
        assertEquals(0, adapter.getItemViewType(0));
    }

    @Test
    public void testPendingItem() {
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public Item[] getKidItems() {
                return new Item[]{new TestItem() {
                    @Override
                    public int getLocalRevision() {
                        return -1;
                    }
                }};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        ToggleItemViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
    }

    @Test
    public void testToggle() {
        // collapse all
        viewHolder.itemView.findViewById(R.id.toggle).performClick();
        adapter.bindViewHolder(viewHolder, 0);
        assertEquals(1, adapter.getItemCount());

        // expand again, should add item when binding
        viewHolder.itemView.findViewById(R.id.toggle).performClick();
        adapter.bindViewHolder(viewHolder, 0);
        adapter.bindViewHolder(viewHolder1, 1);
        adapter.bindViewHolder(viewHolder2, 2);
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testScrollToParent() {
        // test smooth scroll
        ShadowRecyclerView shadowRecyclerView =
                (ShadowRecyclerView) ShadowExtractor.extract(recyclerView);
        assertEquals(-1, shadowRecyclerView.getSmoothScrollToPosition());
        viewHolder2.itemView.findViewById(R.id.parent).performClick();
        assertEquals(1, shadowRecyclerView.getSmoothScrollToPosition());
    }

    @Test
    public void testDeleted() {
        assertNull(shadowOf(viewHolder1.itemView.findViewById(R.id.posted)).getOnClickListener());
    }

    @Test
    public void testDead() {
        assertThat(((TextView) viewHolder.itemView.findViewById(R.id.text)))
                .hasCurrentTextColor(ContextCompat.getColor(activity, R.color.blackT87));
        assertThat(((TextView) viewHolder2.itemView.findViewById(R.id.text)))
                .hasCurrentTextColor(ContextCompat.getColor(activity,
                        AppUtils.getThemedResId(activity, android.R.attr.textColorSecondary)));
    }

    @Test
    public void testDefaultCollapsed() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_comment_display),
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_collapsed))
                .apply();
        final TestItem item0 = new TestItem() { // level 0
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public int getKidCount() {
                return 1;
            }

            @Override
            public Item[] getKidItems() {
                return new Item[]{new TestItem() {
                    @Override
                    public String getId() {
                        return "2";
                    }
                }};
            }
        };
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public Item[] getKidItems() {
                return new Item[]{item0};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        assertEquals(1, adapter.getItemCount());
        ToggleItemViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        assertEquals(1, adapter.getItemCount()); // should not add kid to adapter
    }

    @Test
    public void testSavedState() {
        shadowOf(activity).recreate();
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testDefaultDisplayAllLines() {
        assertThat(viewHolder.itemView.findViewById(R.id.more)).isNotVisible();
    }

    @Test
    public void testDisplayMaxLines() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "3")
                .apply();
        adapter.onAttachedToRecyclerView(recyclerView);
        TextView textView = (TextView) viewHolder.itemView.findViewById(R.id.text);
        View more = viewHolder.itemView.findViewById(R.id.more);
        ((ShadowTextView) ShadowExtractor.extract(textView)).setLineCount(10);
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(3);
        assertThat(more).isVisible();
        more.performClick();
        assertThat(textView).hasMaxLines(10);
        assertThat(more).isNotVisible();
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(more).isNotVisible();
    }

    @Test
    public void testMaxLines() {
        adapter.bindViewHolder(viewHolder, 0);
        TextView textView = (TextView) viewHolder.itemView.findViewById(R.id.text);
        ((ShadowTextView) ShadowExtractor.extract(textView)).setLineCount(4); // content has 4 lines
        View more = viewHolder.itemView.findViewById(R.id.more);
        assertThat(textView).hasMaxLines(Integer.MAX_VALUE);
        assertThat(more).isNotVisible();

        // display all regardless of content size
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "-1") //all
                .apply();
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(Integer.MAX_VALUE);
        assertThat(more).isNotVisible();

        // content longer than max lines
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "3")
                .apply();
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(3);
        assertThat(more).isVisible();
        // rebind should not post runnable
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(3);
        assertThat(more).isVisible();

        // content shorter than max lines
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "5")
                .apply();
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(Integer.MAX_VALUE);
        assertThat(more).isNotVisible();
    }

    @Test
    public void testChangeThreadDisplay() {
        assertSinglePage();
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_comment_display),
                        activity.getString(R.string.pref_comment_display_value_single))
                .apply(); // still single
        assertSinglePage();
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_comment_display),
                        activity.getString(R.string.pref_comment_display_value_multiple))
                .apply(); // multiple
        assertMultiplePage();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVote() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), anyString(), voteCallback.capture());
        voteCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.voted), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemPromptToLogin() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), anyString(), voteCallback.capture());
        voteCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemFailed() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), anyString(), voteCallback.capture());
        voteCallback.getValue().onError();
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testReply() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_comment));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ComposeActivity.class)
                .hasExtra(ComposeActivity.EXTRA_PARENT_ID, "1");
    }

    @After
    public void tearDown() {
        recyclerView.setAdapter(null);
        reset(hackerNewsClient);
    }

    private void assertSinglePage() {
        assertEquals(activity.getString(R.string.pref_comment_display_value_single),
                Preferences.getCommentDisplayOption(activity));
        assertThat(recyclerView.getAdapter()).isInstanceOf(SinglePageItemRecyclerViewAdapter.class);
    }

    private void assertMultiplePage() {
        assertEquals(activity.getString(R.string.pref_comment_display_value_multiple),
                Preferences.getCommentDisplayOption(activity));
        assertThat(recyclerView.getAdapter()).isInstanceOf(MultiPageItemRecyclerViewAdapter.class);
    }
}
