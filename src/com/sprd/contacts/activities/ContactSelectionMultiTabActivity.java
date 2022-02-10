package com.sprd.contacts.activities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import androidx.legacy.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.ContactsActivity;
import com.sprd.contacts.group.GroupBrowseListFragmentSprd;
import com.sprd.contacts.list.AllInOneBrowserPickerFragment;
import com.sprd.contacts.list.AllInOneCallLogPickerFragment;
import com.sprd.contacts.list.AllInOneDataPickerFragment;
import com.sprd.contacts.list.AllInOneFavoritesPickerFragment;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.R;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.activities.ActionBarAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import android.text.TextUtils;
import android.app.ActionBar.LayoutParams;
import java.util.Locale;
import android.view.ViewGroup;
import android.os.Parcelable;
import java.util.HashSet;
import android.os.SystemProperties;
import com.android.internal.telephony.TelephonyIntents;
import com.android.ims.ImsManager;
import com.sprd.contacts.util.ViewPagerTabStrip;
import com.sprd.contacts.util.ViewPagerTabs;
import android.os.Handler;
import android.os.Looper;
import android.content.ComponentName;
import java.util.ArrayList;

import android.telephony.PhoneNumberUtils;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.list.MultiSelectContactsListFragment.OnCheckBoxListActionListener;
import com.sprd.contacts.group.GroupBrowseListFragmentSprd.OnGroupBrowserActionListener;
import com.android.contacts.ContactsApplication;
import com.android.contacts.ContactSaveService;

public class ContactSelectionMultiTabActivity extends AppCompatContactsActivity
    implements ActionBarAdapter.Listener, OnCheckBoxListActionListener{
    private static final String TAG = "MultiTabContactSelectionActivity";
    private static final int TAB_INDEX_GROUP_NEWUI = 0;
    private static final int TAB_INDEX_FAVORITES_NEWUI = 1;
    private static final int TAB_INDEX_ALL_NEWUI = 2;
    private static final int TAB_INDEX_CALLLOG_NEWUI = 3;

    private static final int TAB_INDEX_COUNT_NEWUI = 4;

    private static final int REQUEST_CODE_PICK = 1;

    private static final String KEY_TAB = "tab_position";

    private ViewPager mViewPager;
    private PageChangeListener mPageChangeListener = new PageChangeListener();

    private GroupBrowseListFragmentSprd mGroupBrowseListFragment;
    private AllInOneFavoritesPickerFragment mFavoriteFragment;
    private AllInOneBrowserPickerFragment mAllInOneDataPickerFragment;
    private AllInOneCallLogPickerFragment mCallLogFragment;

    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;
    private ContactListFilterController mContactListFilterController;
    private ContactListFilter mFilter = null;

    private boolean mMultiSupport = false;
    private boolean mIsFirstEnter = true;

    private ImageButton mDoneMenuItem;
    private TextView mShowCount;
    private boolean mDoneEnable = false;
    private boolean mDoneSplitEnable = false;//SPRD:add  for bug614908
    private int mDoneMenuDisableColor = Color.WHITE;
    private int mCurrentTabPosition = -1;
    private BroadcastReceiver mSelecStatusReceiver;
    //SPRD:add newfuture for bug423428
    private static final String BLACK_CALL_LIST_ACTION = "com.sprd.blacklist.action";
    /*
     * SPRD:
     * For Bug 377703 and 382921
     * @{
     */
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final String KEY_SEARCH_TAB = "tab_search";
    private static final String KEY_DONE_ENABLE = "done_enable";
    private static final String KEY_LIMIIT_COUNT = "checked_limit_count";
    private ViewPagerTabs mViewPagerTabs;
    private String[] mTabTitles;
    private boolean showsearchmenu = false;
    private boolean mIsSearchMode = false;
    private MultiTabViewPagerAdapter mViewPagerAdapter;
    private int searchOnTab = -1;
    private ActionBarAdapter mActionBarAdapter;
    private Toolbar mToolbar;
    private View customActionBarView;
    private int mLimitCount = 3500;
    /*
     * @}
     */
    /**
     * Bug515797 If ContactSelectionMultiTabActivity is lunched and contact has no permission,
     * contacts can't be selected.
     * @{
     */
    private static boolean mIsNeedPromptPermissionMessage = false;
    private Handler mMainHandler;
    /**
     * @}
     */

    /**
     * SPRD:Bug519952,523978 "Done" button is grey while some contacts is chosen
     *
     * @{
     */
    private static Boolean mFavoriteDoneEnable = false;
    private static Boolean mAllInOneDoneEnable = false;
    private static Boolean mCallLogDoneEnable = false;
    private boolean mCallFireWallActivityCalling = false;
    private HashMap<String, String> mDataAll = new HashMap<String, String>();
    private HashMap<String, String> mGroupBrowseListData = new HashMap<String, String>();
    /**
     * @}
     */

    // SPRD:add for volte
    private static final int MIN_CONTACT_COUNT = 2;

    /**
     * SPRD bug535541 During inviting contacts from Contacts group from conference
     * call, could select more than three contacts
     * @{
     */
    public static final int CHECKED_ITEMS_MAX = 3500;
    public int mCheckedLimitCount = 3500 ;
    /**
     * @}
     */
    // add for Bug543785 scroll to CallLog,select all,rotate phone and scroll to other tab the
    // choosen contacts can't be added blacklist
    private int mCurrentPosition = -1;
    private int mNextPosition = -1;
    public ContactSelectionMultiTabActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * Bug515797 If ContactSelectionMultiTabActivity is lunched and contact has no permission,
         * contacts can't be selected.
         * @{
         */
        mMainHandler = new Handler(Looper.getMainLooper());
        ComponentName callingActivity = getCallingActivity();
        /**
         * SPRD: Bug 532299 The dut can not add contacts for conference call
         * after selecting contacts and switching the tags.
         * @{
         */
        String intentParam = getIntent().getStringExtra("multi");
        mLimitCount = getIntent().getIntExtra(KEY_LIMIIT_COUNT, 3500);
        if (intentParam != null && (intentParam.equals("addMultiCall")
                || intentParam.equals("addMultiCallAgain"))) {
            mCallFireWallActivityCalling = true;
        }
        /*
         * {@
         */
        if (callingActivity != null) {
            String className = callingActivity.getShortClassName();
            /**
             * SPRD:Bug519952 "Done" button is grey while some contacts is chosen
             * @{
             */
            if (className.endsWith("CallFireWallActivity")) {
                mCallFireWallActivityCalling = true;
            }
            /**
             * @}
             */
            needPromptMessage(className);
        }
        if (mIsNeedPromptPermissionMessage && callingActivity == null) {
            showToast(R.string.re_add_contact);
            mIsNeedPromptPermissionMessage = false;
            finish();
        }
        /**
         * @}
         */
        /**
         * Bug515797 If ContactSelectionMultiTabActivity is lunched and contact has no permission, contacts can't be selected.
         * Bug729298 press multi call with no phone permisson for contacts, Contacts crash.
         * @{
         */
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        mIsNeedPromptPermissionMessage = false;
        /**
         * @}
         */

        /**
         * SPRD:723430 Add toast and return when add contact widget and batch options at the same time
         * @{
         */
        if (ContactsApplication.sApplication.isBatchOperation()
                || ContactSaveService.mIsGroupSaving
                || ContactSaveService.mIsJoinContacts) {
            Toast.makeText(this, R.string.toast_batchoperation_is_running,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        /**
         * @}
         */

        if (savedInstanceState != null) {
            mCurrentTabPosition = savedInstanceState.getInt(KEY_TAB);
            //SPRD:Bug543785 reacquire current tab while rotate the phone
            mCurrentPosition = savedInstanceState.getInt(KEY_TAB);
            mIsFirstEnter = false;
            /* SPRD: Add search menu for MultiTab Activity * @{*/
            mIsSearchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
            searchOnTab = savedInstanceState.getInt(KEY_SEARCH_TAB);
            mDoneEnable = savedInstanceState.getBoolean(KEY_DONE_ENABLE);
            //SPRD:Bug614908 "Done" button is grey when split screen
            mDoneSplitEnable = mDoneEnable;
            /* @} */
            if (mLimitCount == CHECKED_ITEMS_MAX){
                mLimitCount = savedInstanceState.getInt(KEY_LIMIIT_COUNT, 3500);
            }
        }
        /**
         * SPRD:Bug 522240 Done menu is bright while there is no contactselected
         *
         * @{
         */
        else {
            mFavoriteDoneEnable = false;
            mCallLogDoneEnable = false;
            mAllInOneDoneEnable = false;
        }
        /*
         * @}
         */
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        /**
         * SPRD:Bug462535 Add intent null value judgment.
         * @{
         */
        if (mRequest == null || mRequest.getCascadingData() == null) {
            Log.d(TAG," onCreate missing required data");
            finish();
        }
        /**
         * @}
         */

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mFilter = mContactListFilterController.getFilter();

        configureActivityTitle();
        /*
         * SPRD:
         * Bug 377703 Add MultiTab selection activity UI feature.
         * @{
         */
        setContentView(R.layout.selection_activity);
        findViewById(R.id.selection_list_container).addOnLayoutChangeListener(
                mOnLayoutChangeListener);
        /*
         * @}
         */
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPagerAdapter = new MultiTabViewPagerAdapter(getFragmentManager());
        if (mViewPager != null) {
            mViewPager.setAdapter(mViewPagerAdapter);
            mViewPager.setOnPageChangeListener(mPageChangeListener);
        }
        /* SPRD: Bug 377703 and 382921  * @{*/
        createTabViews(savedInstanceState);
        prepareActionBar();
        /* @} */
        if (mViewPager != null && mCurrentTabPosition != -1) {
            mViewPager.setCurrentItem(mCurrentTabPosition);
        }
        // add for SPRD:Bug 565982 set pre-loaded pages from 4 to 1
        if (mCallFireWallActivityCalling) {
            mViewPager.setOffscreenPageLimit(4);
        }
    }

    // add for SPRD:Bug 547154 more than designated contacs can be selected while merge calls during
    // contacts inviting process
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        // add for SPRD:Bug 564314 clear data after merge calls and return to invite tabs
        clearDataAll();
    }

    /**
     * Bug515797 If ContactSelectionMultiTabActivity is lunched and contact has no permission,
     * contacts can't be selected.
     * @param className the activity name who invoke ContactSelectionMultiTabActivity
     */
    private void needPromptMessage(String className) {
        if (className.endsWith("CallFireWallActivity")) {
            mIsNeedPromptPermissionMessage = true;
        }
    }
    /**
     * Shows a toast on the UI thread.
     */
    private void showToast(final int message) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSelectionMultiTabActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    /*
     * SPRD:
     * Bug 377703 and 382921.
     * @{
     */
    private void createTabViews(Bundle savedInstanceState) {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        mTabTitles = new String[TAB_INDEX_COUNT_NEWUI];
        mTabTitles[TAB_INDEX_GROUP_NEWUI] = getString(R.string.contactsGroupsLabel);
        mTabTitles[TAB_INDEX_FAVORITES_NEWUI] = getString(R.string.contactsFavoritesLabel);
        mTabTitles[TAB_INDEX_ALL_NEWUI] = getString(R.string.people);
        mTabTitles[TAB_INDEX_CALLLOG_NEWUI] = getString(R.string.recentCallsIconLabel);
        final ViewPagerTabs portraitViewPagerTabs
                = (ViewPagerTabs) findViewById(R.id.selection_lists_pager_header);
        ViewPagerTabs landscapeViewPagerTabs = null;
        mToolbar = getView(R.id.selection_toolbar);
        setSupportActionBar(mToolbar);
        if (portraitViewPagerTabs == null) {
            landscapeViewPagerTabs = (ViewPagerTabs) getLayoutInflater().inflate(
                    R.layout.people_activity_tabs_lands, mToolbar, /* attachToRoot = */false);
            mViewPagerTabs = landscapeViewPagerTabs;
        } else {
            mViewPagerTabs = portraitViewPagerTabs;
        }
        mViewPagerTabs.setViewPager(mViewPager);

        mActionBarAdapter = new ActionBarAdapter(this, this, getSupportActionBar(), portraitViewPagerTabs, landscapeViewPagerTabs, mToolbar, R.string.enter_contact_name);
        mActionBarAdapter.initialize(savedInstanceState, mRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchItem.setVisible(!mIsSearchMode);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (mIsSearchMode) {
            searchItem.setVisible(false);
            customActionBarView.setVisibility(View.GONE);
        } else {
            if (customActionBarView.getVisibility() == View.GONE) {
                prepareActionBar();
            }
            /**SPRD: Bug692321 The menu and donot match the page selected in ContactSelectionMultiTabActivity {@*/
            switch (getTabPositionForTextDirection(mViewPager.getCurrentItem())) {
            /**@}*/
                case TAB_INDEX_ALL_NEWUI:
                    searchItem.setVisible(true);
                    break;
                case TAB_INDEX_FAVORITES_NEWUI:
                    searchItem.setVisible(true);
                    break;
                case TAB_INDEX_GROUP_NEWUI:
                    searchItem.setVisible(false);
                    customActionBarView.setVisibility(View.GONE);
                    break;
                case TAB_INDEX_CALLLOG_NEWUI:
                    searchItem.setVisible(false);
                    break;
            }
            updataCountAndDone();
        }
        return true;
    }
    @Override
    public void onUpButtonPressed() {
        onBackPressed();
    }
    @Override
    public void onSelectedTabChanged() {
        updateFragmentsVisibility();
  }
    @Override
    public void onAction(int action) {
        switch (action) {
            case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.STOP_SEARCH_AND_SELECTION_MODE:
                setQueryTextToFragment("");
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.CHANGE_SEARCH_QUERY:
                final String queryString = mActionBarAdapter.getQueryString();
                setQueryTextToFragment(queryString);
                break;
            default:
                throw new IllegalStateException("Unkonwn ActionBarAdapter action: " + action);
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mActionBarAdapter.isSearchMode()) {
            mActionBarAdapter.setFocusOnSearchView();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if (mActionBarAdapter != null) {
            mActionBarAdapter.setListener(null);
        }
        super.onDestroy();
    }

    public boolean isSearchMode() {
        return mIsSearchMode;
    }

    @Override
    public void onStartDisplayingCheckBoxes() {
    }

    @Override
    public void onStopDisplayingCheckBoxes() {
    }

    @Override
    public void onSelectedContactIdsChanged() {
        updataCountAndDone();
    }

    @Override
    public void onBackPressed() {
        if (mActionBarAdapter.isSearchMode()) {
            mIsSearchMode = false;
            mActionBarAdapter.setSearchMode(mIsSearchMode);
        } else if (isTaskRoot()) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private void updateFragmentsVisibility() {
        int tab = mActionBarAdapter.getCurrentTab();
        if (mActionBarAdapter.isSearchMode()) {
            mViewPagerAdapter.setSearchMode(true);
        } else {
            final boolean wasSearchMode = mViewPagerAdapter.isSearchMode();
            mViewPagerAdapter.setSearchMode(false);
        }
        return;
    }
    /*
     * @}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //add for Bug550820 return from search tab, tab isn't updated
        outState.putInt(KEY_TAB, mCurrentPosition);
        /**
         * SPRD bug382921 Add search menu for MultiTab Activity
         * SPRD bug523978,538887 During adding contacts to blacklist, contacts checked has not
         * added into blacklist successfully
         * @{
         */
        outState.putBoolean(KEY_SEARCH_MODE, mIsSearchMode);
        outState.putInt(KEY_SEARCH_TAB,searchOnTab);
        outState.putBoolean(KEY_DONE_ENABLE, mDoneEnable);
        outState.putInt(KEY_LIMIIT_COUNT, mLimitCount);
        mActionBarAdapter.onSaveInstanceState(outState);
        mActionBarAdapter.setListener(null);
        if (mViewPager != null) {
            mViewPager.setOnPageChangeListener(null);
        }
        /**
         * @}
         */
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mViewPager != null && mIsFirstEnter) {
            mViewPager.setCurrentItem(TAB_INDEX_ALL_NEWUI);
            mIsFirstEnter = false;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // add for SPRD:Bug 547154,564316 more than designated contacs can be selected while merge
        // calls during contacts inviting process
        int mIntentChecknum = getIntent().getIntExtra("checked_limit_count", CHECKED_ITEMS_MAX);
        int mFavorNum = mFavoriteFragment == null ? 0 : mFavoriteFragment.getSelectedDatas().size();
        int mCallLogNum = mCallLogFragment == null ? 0 : mCallLogFragment.getSelectedDatas().size();
        int mAllInOneNum = mAllInOneDataPickerFragment == null ? 0 : mAllInOneDataPickerFragment
                .getSelectedDatas().size();
        int mCheckedNum = mFavorNum + mCallLogNum + mAllInOneNum;
        mCheckedLimitCount = (mIntentChecknum != CHECKED_ITEMS_MAX) ? (mIntentChecknum - mCheckedNum)
                : mIntentChecknum;
        mLimitCount = getIntent().getIntExtra(KEY_LIMIIT_COUNT, 3500);
        /* SPRD: Bug 382921 Add search menu for MultiTab Activity * @{ */
        mActionBarAdapter.setListener(this);
        if (mViewPager != null) {
            mViewPager.setOnPageChangeListener(mPageChangeListener);
        }
        updateFragmentsVisibility();
        /* @} */
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof GroupBrowseListFragmentSprd) {
            if (mGroupBrowseListFragment == null) {
                mGroupBrowseListFragment = (GroupBrowseListFragmentSprd) fragment;
                setupActionListener(TAB_INDEX_GROUP_NEWUI);
            }
        } else if (fragment instanceof AllInOneFavoritesPickerFragment) {
            if (mFavoriteFragment == null) {
                mFavoriteFragment = (AllInOneFavoritesPickerFragment) fragment;
                setupActionListener(TAB_INDEX_FAVORITES_NEWUI);
            }
        } else if (fragment instanceof AllInOneCallLogPickerFragment) {
            if (mCallLogFragment == null) {
                mCallLogFragment = (AllInOneCallLogPickerFragment) fragment;
                setupActionListener(TAB_INDEX_CALLLOG_NEWUI);
            }
        } else if (fragment instanceof AllInOneBrowserPickerFragment) {
            if (mAllInOneDataPickerFragment == null) {
                mAllInOneDataPickerFragment = (AllInOneBrowserPickerFragment) fragment;
                setupActionListener(TAB_INDEX_ALL_NEWUI);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
            /* SPRD: Bug 382921 Add search menu for MultiTab Activity * @{*/
            case R.id.menu_search:
                /**SPRD: Bug692321 The menu and donot match the page selected in ContactSelectionMultiTabActivity {@*/
                searchOnTab = getTabPositionForTextDirection(mViewPager.getCurrentItem());
                /**@}*/
                mIsSearchMode = !mIsSearchMode;
                mActionBarAdapter.setSearchMode(mIsSearchMode);
                int mIntentChecknum = getIntent().getIntExtra("checked_limit_count",
                        CHECKED_ITEMS_MAX);
                int mFavorNum = mFavoriteFragment == null ? 0 : mFavoriteFragment
                        .getSelectedDatas().size();
                int mCallLogNum = mCallLogFragment == null ? 0 : mCallLogFragment
                        .getSelectedDatas().size();
                int mAllInOneNum = mAllInOneDataPickerFragment == null ? 0
                        : mAllInOneDataPickerFragment
                        .getSelectedDatas().size();
                int mCheckedNum = mFavorNum + mCallLogNum + mAllInOneNum;
                mCheckedLimitCount = (mIntentChecknum != CHECKED_ITEMS_MAX) ? (mIntentChecknum - mCheckedNum)
                        : mIntentChecknum;
                customActionBarView.setVisibility(View.GONE);
                return true;
                /* @} */
        }
        return super.onOptionsItemSelected(item);
    }

    public class MultiTabViewPagerAdapter extends FragmentPagerAdapter {
        /* SPRD: Bug 382921 Add search menu for MultiTab Activity * @{*/
        private boolean mTabPagerAdapterSearchMode;
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;
        private final FragmentManager mFragmentManager;
        /* @} */
        public MultiTabViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            mFragmentManager = getFragmentManager();
        }

        @Override
        public Fragment getItem(int position) {
            /**SPRD Bug690887 The tabs donot match the data shown below in ContactSelectionMultiTabActivity {@*/
            switch (getTabPositionForTextDirection(position)) {
            /**@}*/
                case TAB_INDEX_GROUP_NEWUI:
                    mGroupBrowseListFragment = new GroupBrowseListFragmentSprd();
                    configTabAdapter(TAB_INDEX_GROUP_NEWUI);
                    return mGroupBrowseListFragment;
                case TAB_INDEX_FAVORITES_NEWUI:
                    mFavoriteFragment = new AllInOneFavoritesPickerFragment();
                    configTabAdapter(TAB_INDEX_FAVORITES_NEWUI);
                    return mFavoriteFragment;
                case TAB_INDEX_ALL_NEWUI:
                    mAllInOneDataPickerFragment = new AllInOneBrowserPickerFragment();
                    configTabAdapter(TAB_INDEX_ALL_NEWUI);
                    return mAllInOneDataPickerFragment;
                case TAB_INDEX_CALLLOG_NEWUI:
                    mCallLogFragment = new AllInOneCallLogPickerFragment();
                    configTabAdapter(TAB_INDEX_CALLLOG_NEWUI);
                    return mCallLogFragment;
            }
            throw new IllegalStateException("No fragment at position "
                    + position);
        }
        /*
         * SPRD:
         * Bug 377703 and 382921
         * @{
         */
        public boolean isSearchMode() {
            return mTabPagerAdapterSearchMode;
        }
        public void setSearchMode(boolean searchMode) {
            if (searchMode == mTabPagerAdapterSearchMode) {
                return;
            }
            mTabPagerAdapterSearchMode = searchMode;
            notifyDataSetChanged();
        }
        @Override
        public int getCount() {
            return mTabPagerAdapterSearchMode ? 1 : TAB_INDEX_COUNT_NEWUI;
        }
        @Override
        public int getItemPosition(Object object) {
            if (mTabPagerAdapterSearchMode) {
                if (searchOnTab == TAB_INDEX_FAVORITES_NEWUI) {
                    if (object == mFavoriteFragment) {
                        return 0;
                    }
                } else if (searchOnTab == TAB_INDEX_ALL_NEWUI) {
                    if (object == mAllInOneDataPickerFragment) {
                        return 0;
                    }
                }
            } else {
                if (object == mAllInOneDataPickerFragment) {
                    return getTabPositionForTextDirection(TAB_INDEX_ALL_NEWUI);
                }
                if (object == mGroupBrowseListFragment) {
                    return getTabPositionForTextDirection(TAB_INDEX_GROUP_NEWUI);
                }
                if (object == mCallLogFragment) {
                    return getTabPositionForTextDirection(TAB_INDEX_CALLLOG_NEWUI);
                }
                if (object == mFavoriteFragment) {
                    return getTabPositionForTextDirection(TAB_INDEX_FAVORITES_NEWUI);
                }
            }
            return POSITION_NONE;
        }
        private Fragment getFragment(int position) {
            position = getTabPositionForTextDirection(position);
            if (mTabPagerAdapterSearchMode) {
                if (position != 0) {
                    Log.w(TAG, "Request fragment at position=" + position + ", eventhough we " +
                            "are in search mode");
                }
                if (searchOnTab == TAB_INDEX_FAVORITES_NEWUI) {
                    return mFavoriteFragment;
                }
                return mAllInOneDataPickerFragment;
            } else {
                if (position == TAB_INDEX_FAVORITES_NEWUI) {
                    return mFavoriteFragment;
                } else if (position == TAB_INDEX_ALL_NEWUI) {
                    return mAllInOneDataPickerFragment;
                } else if (position == TAB_INDEX_GROUP_NEWUI) {
                    return mGroupBrowseListFragment;
                } else if (position == TAB_INDEX_CALLLOG_NEWUI) {
                    return mCallLogFragment;
                }
            }
            throw new IllegalArgumentException("position: " + position);
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mTabPagerAdapterSearchMode) {
               if (mCurTransaction == null) {
                  mCurTransaction = mFragmentManager.beginTransaction();
               }
               Fragment f = getFragment(position);
               mCurTransaction.show(f);
               f.setUserVisibleHint(f == mCurrentPrimaryItem);
               return f;
            } else {
               return super.instantiateItem(container,position);
            }
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }
        /*
         * @}
         */
    }
    /*
     * SPRD:
     * Bug 377703 and 382921
     * @{
     */
    private void setQueryTextToFragment(String query) {
        if (searchOnTab == TAB_INDEX_FAVORITES_NEWUI) {
            mFavoriteFragment.setQueryString(query, /* delaySelection */ false);
            mFavoriteFragment.setVisibleScrollbarEnabled(!mFavoriteFragment.isSearchMode());
        } else if (searchOnTab == TAB_INDEX_ALL_NEWUI) {
            mAllInOneDataPickerFragment.setQueryString(query, /* delaySelection */ false);
            mAllInOneDataPickerFragment.setVisibleScrollbarEnabled(!mAllInOneDataPickerFragment.isSearchMode());
        }
    }
    private int getTabPositionForTextDirection(int position) {
        if (isRTL()) {
            return TAB_INDEX_COUNT_NEWUI - 1 - position;
        }
        return position;
    }

    private boolean isRTL() {
        final Locale locale = Locale.getDefault();
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
    }

    private void showStripForTab(int tab) {
        mViewPagerTabs.onPageScrolled(tab, 0, 0);
    }
    /*
     * @}
     */
    private class PageChangeListener implements OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            /*
             * SPRD:
             * Bug 377703 Add MultiTab selection activity UI feature.
             * @{
             */
            if (!mViewPagerAdapter.isSearchMode()) {
               mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
            /*
             * @}
             */
        }

        @Override
        public void onPageSelected(int position) {
            /*
             * SPRD:
             * Bug 377703 Add MultiTab selection activity UI feature.
             * @{
             */
            if (!mViewPagerAdapter.isSearchMode()) {
              mViewPagerTabs.onPageSelected(position);
              showStripForTab(position);
            }
            /*
             * @}
             */
            if (mCurrentPosition == position) {
                Log.w(TAG, "Previous position and next position became same ("
                        + position + ")");
            }
            mNextPosition = position;
        }

        public void setCurrentPosition(int position) {
            mCurrentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int status) {
            /*
             * SPRD:
             * Bug 377703 Add MultiTab selection activity UI feature.
             * @{
             */
            if (!mViewPagerAdapter.isSearchMode()) {
               mViewPagerTabs.onPageScrollStateChanged(status);
            }
            /*
             * @}
             */
            switch (status) {
                case ViewPager.SCROLL_STATE_IDLE:
                    if (mCurrentPosition >= 0) {
                        sendFragmentVisibilityChange(mCurrentPosition, false);
                    }
                    if (mNextPosition >= 0) {
                        sendFragmentVisibilityChange(mNextPosition, true);
                    }
                    invalidateOptionsMenu();

                    mCurrentPosition = mNextPosition;
                    break;
                case ViewPager.SCROLL_STATE_DRAGGING:
                case ViewPager.SCROLL_STATE_SETTLING:
                default:
                    break;
            }

        }
    }

    private void sendFragmentVisibilityChange(int position, boolean visibility) {
        if (position >= 0) {
            final Fragment fragment = getFragmentAt(position);
            if (fragment != null) {
                fragment.setMenuVisibility(visibility);
                fragment.setUserVisibleHint(visibility);
            }
        }
    }

    private Fragment getFragmentAt(int position) {
        /**unisoc: Bug942268 fix XB language select all function {@*/
        position = getTabPositionForTextDirection(position);
        /**@}*/
        if(!mIsSearchMode) {
            switch (position) {
                case TAB_INDEX_GROUP_NEWUI:
                    return mGroupBrowseListFragment;
                case TAB_INDEX_FAVORITES_NEWUI:
                    return mFavoriteFragment;
                case TAB_INDEX_ALL_NEWUI:
                    return mAllInOneDataPickerFragment;
                case TAB_INDEX_CALLLOG_NEWUI:
                    return mCallLogFragment;
                default:
                    throw new IllegalStateException("Unknown fragment index: " + position);
            }
        } else {
            if (position == TAB_INDEX_FAVORITES_NEWUI) {
                return mFavoriteFragment;
            } else {
                return mAllInOneDataPickerFragment;
            }
        }
    }

    private void configureActivityTitle() {
        setTitle(R.string.contactPickerActivityTitle);
    }

    private void prepareActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
            mDoneMenuItem = (ImageButton) customActionBarView.findViewById(R.id.save_menu_item_button);
            mDoneMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    /**SPRD: Bug692321 The menu and donot match the page selected in ContactSelectionMultiTabActivity {@*/
                    configCompleteListener(mViewPager != null ? getTabPositionForTextDirection(mViewPager.getCurrentItem()) : -1);
                    /**@}*/

                }
            });
            mShowCount = (TextView) customActionBarView.findViewById(R.id.selection_count_text);
            actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
            /* SPRD: Bug 382921 Add search menu for MultiTab Activity * @{*/
            if (!mIsSearchMode) {
               actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO
                      | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
            } else {
                /**
                 * SPRD: BUG 729967 Invite the contacts screen, click search,
                 * and then display two return keys in the title bar
                 * @{
                 * */
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO
                        | ActionBar.DISPLAY_SHOW_TITLE);
                /**
                 * @}
                 *
                 **/
            }
            /* @} */
        }
    }

    private void updataCountAndDone() {
        /**
         * SPRD: Bug 764046 Blacklist add contacts, select the same number under tab, and the number of checked lists does not match the actual number.
         * @{
         * original code:
         *  int mFavorNum = mFavoriteFragment == null ? 0 : mFavoriteFragment
         *          .getSelectedDatas().size();
         *  int mCallLogNum = mCallLogFragment == null ? 0 : mCallLogFragment
         *          .getSelectedDatas().size();
         *  int mAllInOneNum = mAllInOneDataPickerFragment == null ? 0: mAllInOneDataPickerFragment
         *          .getSelectedDatas().size();
         *  int mSumNum = mFavorNum + mCallLogNum + mAllInOneNum;
         */
            HashMap<String, String> mFavor = mFavoriteFragment == null ? new HashMap<>() : mFavoriteFragment.getSelectedDatas();
            HashMap<String, String> mCallLog = mCallLogFragment == null ? new HashMap<>() : mCallLogFragment.getSelectedDatas();
            HashMap<String, String> mAllInOne = mAllInOneDataPickerFragment == null ? new HashMap<>() : mAllInOneDataPickerFragment.getSelectedDatas();

            HashMap<String, String> mSum = new HashMap<>();

            mSum.putAll(mFavor);
            mSum.putAll(mCallLog);
            mSum.putAll(mAllInOne);

            int mSumNum = mSum.size();
        /**
          * @}
          */
        if (mSumNum > 0) {
            if (mDoneMenuItem != null) {
                mDoneMenuItem.setVisibility(View.VISIBLE);
            }
            if (mShowCount != null) {
                mShowCount.setVisibility(View.VISIBLE);
                mShowCount.setText(String.valueOf(mSumNum));
            }
        } else {
            if (mDoneMenuItem != null) {
                mDoneMenuItem.setVisibility(View.GONE);
            }
            if (mShowCount != null) {
                mShowCount.setVisibility(View.GONE);
            }
        }

        /**
         * SPRD:Bug873473 During conference meeting, when choose the sixth contact,show the tip.
         * @{
         */
        int mIntentChecknum = getIntent().getIntExtra(KEY_LIMIIT_COUNT, CHECKED_ITEMS_MAX);
        int mCheckedNum = mFavor.size() + mCallLog.size() + mAllInOne.size();
        mCheckedLimitCount = mIntentChecknum - mCheckedNum;
        Log.d(TAG, "ContactSelectionMultiTabActivity updataCountAndDone: mCheckedLimitCount = " + mCheckedLimitCount);
        /**
         * @}
         */
    }
    private final View.OnLayoutChangeListener mOnLayoutChangeListener = new
            View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                        int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                }
            };

    /*
     * For Touch Assistant and KEYCODE_SEARCH event.
     */
    @Override
    public boolean onSearchRequested() {
        /**SPRD: Bug692321 The menu and donot match the page selected in ContactSelectionMultiTabActivity {@*/
        searchOnTab = getTabPositionForTextDirection(mViewPager.getCurrentItem());
        /**@}*/
        // there is not support quick search in group and calllog page.
        switch (searchOnTab) {
            case TAB_INDEX_GROUP_NEWUI:
                return true;
            case TAB_INDEX_CALLLOG_NEWUI:
                return true;
            default:
                break;
        }
        mIsSearchMode = !mIsSearchMode;
        mActionBarAdapter.setSearchMode(true);
        customActionBarView.setVisibility(View.GONE);
        return true;
    }

    private final class GroupBrowserActionListener implements OnGroupBrowserActionListener {

        @Override
        public void onViewGroupAction(Uri groupUri) {
            /**
             * SPRD: Bug 531368 The call screen becomes abnormal after inviting email
             * contact of the group.
             * {@
             */
            String intentParam = getIntent().getStringExtra("multi");
            if ((getIntent().getStringExtra("blackcall_type") != null)
                    || (intentParam != null && (intentParam.equals("addMultiCall") || intentParam
                            .equals("addMultiCallAgain")))) {
            /*
             * @}
             */
                Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION).
                        putExtra("cascading",new Intent(UiIntentActions.MULTI_PICK_ACTION).setType(Phone.CONTENT_ITEM_TYPE));
                intent.putExtra("mode","mode_select_group_member");
                intent.putExtra("select_group_member", groupUri != null ? ContentUris.parseId(groupUri): -1);
                intent.putExtra("with_phone_number", 1);
                /**
                 * SPRD bug535541 During inviting contacts from Contacts group from conference
                 * call, could select more than three contacts
                 * @{
                 */
                if (getIntent().getIntExtra("checked_limit_count", -1) > 0) {
                    intent.putExtra("checked_limit_count", getIntent().getIntExtra("checked_limit_count", CHECKED_ITEMS_MAX));
                }
                /**
                 * @}
                 */
                startActivityForResult(intent, REQUEST_CODE_PICK);
            } else {
                Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION).
                        putExtra(
                                "cascading",
                                new Intent(UiIntentActions.MULTI_PICK_ACTION).setType(Phone.CONTENT_ITEM_TYPE).
                                        putExtra(
                                                "cascading",
                                                new Intent(UiIntentActions.MULTI_PICK_ACTION)
                                                        .setType(Email.CONTENT_ITEM_TYPE)));
                intent.putExtra("select_group_member", groupUri != null ? ContentUris.parseId(groupUri)
                        : -1);
                /**
                 * SPRD bug535541 During inviting contacts from Contacts group from conference
                 * call, could select more than three contacts
                 * @{
                 */
                if (getIntent().getIntExtra("checked_limit_count", -1) > 0) {
                    intent.putExtra("checked_limit_count", getIntent().getIntExtra("checked_limit_count", CHECKED_ITEMS_MAX));
                }
                /**
                 * @}
                 */
                startActivityForResult(intent, REQUEST_CODE_PICK);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * SPRD:Bug 523290 The Dut can not make a conference call after selecting the group
         * contact members.
         * SPRD:Bug 528769 The Contacts app crashes after canceling the selection of the group
         * contact members.
         * SPRD:Bug 528828 The dut can not add a voice call for the conference call.
         * {@
         */
        String intentParam = getIntent().getStringExtra("multi");
        ArrayList numberList = new ArrayList();
        boolean repeat = false;
        ArrayList numArrayIn = new ArrayList();
        //UNISOC: modify for bug1174972, addMultiCall does not need volte switch in contacts
        if (data != null && data.getSerializableExtra("result") != null
                && intentParam != null
                && (intentParam.equals("addMultiCall") || intentParam.equals("addMultiCallAgain"))) {
            final HashMap<String, String> contacts = (HashMap<String, String>) data
                    .getSerializableExtra("result");
            final int minLimitCount = getIntent().getIntExtra("checked_min_limit_count",
                    MIN_CONTACT_COUNT);
            String [] numberArray = getIntent().getExtras().getStringArray("number");
            String[] numListArray = null;
            if (contacts != null && contacts.size() > 0) {
                numListArray = new String[contacts.size()];
                Iterator it = contacts.entrySet().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Entry entry = (Entry) it.next();
                    numListArray[i] = ((String) entry.getKey()).replace(" ", "");
                    Log.i(TAG, "numListArray[ " + i + "]: " + numListArray[i]);
                    i++;
                }
                /**
                 * SPRD:Bug547762 deal with the space in phone number S
                 * PRD:Bug565060 Deal with prefix numbers
                 * @{
                 */
                for (int j = 0; j < i; j++) {
                    boolean repeated = false;
                    for (int m = j + 1; m < i; m++) {
                        if (PhoneNumberUtils.compare(numListArray[j], numListArray[m])) {
                            repeated = true;
                            break;
                        }
                    }
                    if (repeated == false) {
                        numberList.add(numListArray[j]);
                    }
                }
                //add for SPRD:Bug565060 remove contacts in conference meeting already
                boolean repeated = false;
                if (numberArray != null) {
                        for (int m = 0 ; m <numberList.size(); m++){
                            repeated = false;
                            for (int j = 0; j < numberArray.length; j++) {
                                if (PhoneNumberUtils.compare (numberList.get(m).toString(), numberArray[j])){
                                    repeat = true;
                                    repeated = true;
                                    break;
                                }
                            }
                            if (repeated == false) {
                                numArrayIn.add(numberList.get(m));
                            }
                        }
                }
                if (numArrayIn.isEmpty() && numberArray != null) {
                    Toast.makeText(ContactSelectionMultiTabActivity.this,
                            R.string.contacts_in_conference_reselect,
                            Toast.LENGTH_SHORT).show();
                    if (repeat == true){
                        repeat = false;
                    }
                    mDataAll.clear();
                    return;
                }
                if (!numArrayIn.isEmpty()  && repeat == true) {
                    Toast.makeText(ContactSelectionMultiTabActivity.this,
                            R.string.contacts_in_conference,
                            Toast.LENGTH_SHORT).show();
                    repeat = false;
                }
            }
            if (contacts != null && contacts.size() < minLimitCount) {
                if (numberArray == null) {
                Toast.makeText(ContactSelectionMultiTabActivity.this,
                        getString(R.string.conferenceCallPartyCountLimit, minLimitCount),
                        Toast.LENGTH_SHORT).show();
                }
                return;
            }
            String[] array = numberArray == null ? (String[]) numberList
                    .toArray(new String[numberList.size()]) : (String[]) numArrayIn
                    .toArray(new String[numArrayIn.size()]);
                Bundle bundle = new Bundle();
                bundle.putBoolean("android.intent.extra.IMS_CONFERENCE_REQUEST", true);
                bundle.putStringArray("android.intent.extra.IMS_CONFERENCE_PARTICIPANTS",
                        array);
                final Intent intentAction = new Intent();
                intentAction.setAction("android.intent.action.CALL_PRIVILEGED");
                intentAction.setData(Uri.parse(new StringBuffer("tel:").append(array[0])
                        .toString()));
                intentAction.putExtras(bundle);
                startActivity(intentAction);
                finish();
        }


        /*
         * }@
         */
        if (requestCode == REQUEST_CODE_PICK) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(RESULT_OK, data);
                /* SPRD:Bug 423428@ { */
                if ((getIntent().getStringExtra("blackcall_type") != null)) {
                    data.setAction(BLACK_CALL_LIST_ACTION);
                    data.putExtra("blackcall_type", getIntent().getStringExtra("blackcall_type"));
                }
                /* @}*/
                finish();
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
        }
    }


    public void returnPickerResult(HashMap<String, String> data) {
        Intent intent = new Intent();
        // SPRD:Bug 519952 "Done" button is grey while some contacts is chosen
        if (data.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_contact_selected,
                    Toast.LENGTH_SHORT).show();
            return;
        } else if (data.size() > mLimitCount) {
            /**
             * SPRD:Bug716548 need to erase the previous data
             * @{
             */
            mDataAll.clear();
            /**
             * @}
             */
            Toast.makeText(this, getString(R.string.contacts_selection_for_mms_limit, mLimitCount), Toast.LENGTH_LONG).show();
            return;
        } else {
            intent.putExtra("result", data);
            returnPickerResult(intent);
        }
    }

    public void returnPickerResult() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void returnPickerResult(Intent intent) {
        /*SPRD: add for VoLTE{ @*/
        String intentParam = getIntent().getStringExtra("multi");
        ArrayList numberList = new ArrayList();
        ArrayList numArrayIn = new ArrayList();
        boolean repeat = false;
        //UNISOC: modify for bug1174972, addMultiCall does not need volte switch in contacts
        if (intentParam !=null && (intentParam.equals("addMultiCall") || intentParam.equals("addMultiCallAgain"))) {
               final HashMap<String, String> contacts = (HashMap<String, String>) intent.getSerializableExtra("result");
               final int minLimitCount = getIntent().getIntExtra("checked_min_limit_count",MIN_CONTACT_COUNT);
               String [] numberArray = getIntent().getExtras().getStringArray("number");
               String[] numListArray = null;
               if (contacts != null && contacts.size() > 0) {
                   numListArray = new String[contacts.size()];
                   Iterator it = contacts.entrySet().iterator();
                   int i =0;
                   while (it.hasNext()) {
                       Entry entry = (Entry) it.next();
                       //add for bug 524706 can't make conference call due to space
                       numListArray[i] = ((String) entry.getKey()).replace(" ", "");
                       i++;
                   }
                   /**
                    * SPRD:Bug547762 deal with the space in phone number
                    * SPRD:Bug565060 Deal with prefix numbers
                    * @{
                    */
                for (int j = 0; j < i; j++) {
                    boolean repeated = false;
                    for (int m = j + 1; m < i; m++) {
                        if (PhoneNumberUtils.compare(numListArray[j], numListArray[m])) {
                            repeated = true;
                            break;
                        }
                    }
                    if (repeated == false) {
                        numberList.add(numListArray[j]);
                    }
                }
                // add for SPRD:Bug565060 remove contacts in conference meeting already
                if (numberArray != null) {
                    boolean repeated = false;
                    for (int m = 0; m < numberList.size(); m++) {
                        repeated = false;
                        for (int j = 0; j < numberArray.length; j++) {
                            if (PhoneNumberUtils.compare(numberList.get(m).toString(),
                                    numberArray[j])) {
                                repeat = true;
                                repeated = true;
                                break;
                            }
                        }
                        if (repeated == false) {
                            numArrayIn.add(numberList.get(m));
                        }
                    }
                }
                if (numArrayIn.isEmpty() && numberArray != null) {
                    Toast.makeText(ContactSelectionMultiTabActivity.this,
                            R.string.contacts_in_conference_reselect,
                            Toast.LENGTH_SHORT).show();
                    mDataAll.clear();
                    if (repeat == true){
                        repeat = false;
                    }
                    return;
                }
                if (!numArrayIn.isEmpty() && repeat) {
                    Toast.makeText(ContactSelectionMultiTabActivity.this,
                            R.string.contacts_in_conference,
                            Toast.LENGTH_SHORT).show();
                    repeat = false;
                }
                /**
                 * @}
                 */
            }
               //add for SPRD:Bug547762 deal with the space in phone number
               if (contacts != null && numberList.size() < minLimitCount) {
                   if (numberArray == null){
                   Toast.makeText(ContactSelectionMultiTabActivity.this, getString(R.string.conferenceCallPartyCountLimit,minLimitCount),
                           Toast.LENGTH_SHORT)
                           .show();
                   }
                   /* SPRD: 543746 The DUT make a conference call even switching the contact selection tag.*/
                   //add for SPRD: 546975 clear data when need to select again
                   mDataAll.clear();
                   return;
               }
               String[] array = numberArray == null ? (String[]) numberList
                       .toArray(new String[numberList.size()]) : (String[]) numArrayIn
                       .toArray(new String[numArrayIn.size()]);
               Bundle bundle = new Bundle();
               bundle.putBoolean("android.intent.extra.IMS_CONFERENCE_REQUEST", true);
               bundle.putStringArray("android.intent.extra.IMS_CONFERENCE_PARTICIPANTS", array);
               final Intent intentAction = new Intent();
               intentAction.setAction("android.intent.action.CALL_PRIVILEGED");
               intentAction.setData(Uri.parse(new StringBuffer("tel:").append(array[0]).toString()));
               intentAction.putExtras(bundle);
               startActivity(intentAction);
           /*@}*/
        }else{
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            /* SPRD: Bug423428 { */
            if (getIntent().getStringExtra("blackcall_type") != null) {
                intent.setAction(BLACK_CALL_LIST_ACTION);
                intent.putExtra("blackcall_type", getIntent().getStringExtra("blackcall_type"));
            }
            /* @} */
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    private void configTabAdapter(int position) {
        if (position < 0) {
            return;
        }
        switch (position) {
            case TAB_INDEX_GROUP_NEWUI:
                break;
            case TAB_INDEX_FAVORITES_NEWUI:
                mFavoriteFragment.setCascadingData(mRequest.getCascadingData());
                mFavoriteFragment.setSelection("star");
                break;
            case TAB_INDEX_ALL_NEWUI:
                mAllInOneDataPickerFragment.setCascadingData(mRequest
                        .getCascadingData());
                break;
            case TAB_INDEX_CALLLOG_NEWUI:
                mCallLogFragment.setCascadingData(mRequest
                        .getCascadingData());
                break;
            default:
                break;
        }
        setupActionListener(position);
    }

    private void setupActionListener(int position) {

        switch (position) {
            case TAB_INDEX_GROUP_NEWUI:
                mGroupBrowseListFragment.setListener(new GroupBrowserActionListener());
                break;
            case TAB_INDEX_FAVORITES_NEWUI:
                mFavoriteFragment.setCheckBoxListListener(this);
                break;
            case TAB_INDEX_ALL_NEWUI:
                mAllInOneDataPickerFragment.setCheckBoxListListener(this);
                break;
             case TAB_INDEX_CALLLOG_NEWUI:
                 mCallLogFragment.setCheckBoxListListener(this);
                break;

            default:
                break;
        }
    }

    private void configCompleteListener(int position) {
        // SPRD:Bug519952 "Done" button is grey while some contacts is chose
        if (mCallFireWallActivityCalling == true) {
            switch (position) {
                case TAB_INDEX_GROUP_NEWUI:
                    returnPickerResult();
                    break;
                case TAB_INDEX_FAVORITES_NEWUI:
                case TAB_INDEX_ALL_NEWUI:
                case TAB_INDEX_CALLLOG_NEWUI:
                    if (mFavoriteFragment != null) {
                        mDataAll.putAll(mFavoriteFragment.getSelectedDatas());
                    }
                    if (mCallLogFragment != null) {
                        mDataAll.putAll(mCallLogFragment.getSelectedDatas());
                    }
                    if (mAllInOneDataPickerFragment != null) {
                        mDataAll.putAll(mAllInOneDataPickerFragment.getSelectedDatas());
                    }
                    returnPickerResult(mDataAll);
                    break;
                default:
                    break;
            }
        }
        else {
            switch (position) {
                case TAB_INDEX_GROUP_NEWUI:
                    returnPickerResult();
                    break;
                case TAB_INDEX_FAVORITES_NEWUI:
                    if (mFavoriteFragment != null) {
                         //SPRD:Bug519952 "Done" button is grey while some contacts is chosen
                         mFavoriteFragment.onMultiPickerSelected();
                    }
                    break;
                case TAB_INDEX_ALL_NEWUI:
                    if (mAllInOneDataPickerFragment != null) {
                        mAllInOneDataPickerFragment.onMultiPickerSelected();
                    }
                    break;
                case TAB_INDEX_CALLLOG_NEWUI:
                    if (mCallLogFragment != null) {
                        mCallLogFragment.onMultiPickerSelected();
                    }
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * SPRD:Bug 564314 clear data after merge calls and return to invite tabs
     *
     * @{
     */
    private void clearDataAll() {
        if (mFavoriteFragment != null) {
            mFavoriteFragment.clearCheckBoxes();
        }
        if (mAllInOneDataPickerFragment != null) {
            mAllInOneDataPickerFragment.clearCheckBoxes();
        }
        if (mCallLogFragment != null) {
            mCallLogFragment.clearCheckBoxes();
        }
    }

    /**
     * @}
     */
}
