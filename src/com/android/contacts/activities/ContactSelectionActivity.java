/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.activities;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract.Contacts;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.R;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.GroupMemberPickerFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectContactsListFragment.OnCheckBoxListActionListener;
import com.android.contacts.list.MultiSelectEmailAddressesListFragment;
import com.android.contacts.list.MultiSelectPhoneNumbersListFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.logging.ListEvent;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.ViewUtil;
import com.sprd.contacts.group.GroupMemberMoveFragmentSprd;
import com.sprd.contacts.list.AllInOneDataPickerFragment;
import com.sprd.contacts.list.MultiContactsPickerFragment;
import com.sprd.contacts.util.AccountsForMimeTypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import java.util.ArrayList;
import java.util.Iterator;

import com.android.contacts.ContactsApplication;
import com.android.contacts.ContactSaveService;
/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends AppCompatContactsActivity implements
        View.OnCreateContextMenuListener, ActionBarAdapter.Listener, OnClickListener,
        OnFocusChangeListener, OnCheckBoxListActionListener {
    private static final String TAG = "ContactSelection";

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;
    private boolean mIsSearchMode;
    private boolean mIsSearchSupported;

    private ContactsRequest mRequest;

    private ActionBarAdapter mActionBarAdapter;
    private Toolbar mToolbar;
    /**
     * androido-poting bug474752 Add features with multiSelection activity in Contacts.
     *
     * @{
     */
    private ContactListFilter mPermanentAccountFilter = null;
    private ContactListFilter mFilter = null;
    private static final String KEY_FILTER = "mFilter";
    private int mAccountNum = 0;
    private static final String KEY_MODE = "mode";
    private static final String MODE_COPYTO = "mode_copyto";
    private static final String MODE_SHARE = "mode_share";
    private static final String MODE_EXPORT = "mode_export";
    private static final String MODE_SELECT_GROUP_MEMBER = "mode_select_group_member";
    private static final String MODE_MMC_SEND = "mode_mmc_send";
    private static final String DST_ACCOUNT = "dst_account";
    private static final String KEY_LIMIIT_COUNT = "checked_limit_count";
    private static final int SUBACTIVITY_BATCH_IMPORT = 8;
    private int mLimitCount = 3500;
    private String mMode = null;
    /**
     * @}
     */
    //Bug709612 If SelectActivity is lunched and contact has no permission, contact can't be selected.
    private static boolean mIsNeedPromptPermissionMessage = false;
    private Handler mMainHandler;

    public ContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }

    /**
     * SPRD: Bug693205 Add group feature for Contacts
     * @{
     */
    @Override
    protected void onNewIntent(Intent intent) {
        final String action = intent.getAction();
        if (GroupUtil.ACTION_MOVE_GROUP_COMPLETE.equals(action)) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }
    /**
     * @}
     */

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        /**
         * Bug709612 If SelectActivity is lunched and contact has no permission,
         * contact can't be selected.
         * @{
         */
        mMainHandler = new Handler(Looper.getMainLooper());
        ComponentName callingActivity = getCallingActivity();
        if (callingActivity != null) {
            String className = callingActivity.getShortClassName();
            needPromptMessage(className);
        }

        /**
         * Bug716718 A toast should not show when add new contact to Contacts apk from Dialer,
         * so remove the judgement of callingActivity
         * @{
         */
        if (mIsNeedPromptPermissionMessage && callingActivity == null) {
            Log.d(TAG, "callingActivity= " + callingActivity);
         /**
         * @}
         */
            showToast(R.string.re_add_contact);
            mIsNeedPromptPermissionMessage = false;
            finish();
        }
        /**
         * @}
         */
        /**
         * SPRD BUG:727822 mIsNeedPromptPermissionMessage must be reset @{
         *
         **/
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        mIsNeedPromptPermissionMessage = false;
        /**
         * @}
         **/

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
        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
            mIsSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
            mPermanentAccountFilter = (ContactListFilter) savedState.getParcelable(KEY_FILTER);
            /**
             * androido-poting bug474752 Add features with multiSelection activity in Contacts.
             *
             * @{
             */
            mLimitCount = savedState.getInt(KEY_LIMIIT_COUNT);
            mMode = savedState.getString(KEY_MODE);
            /**
             * @}
             */
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        setContentView(R.layout.contact_picker);

        /**
         * androido-poting bug474752 Add features with multiSelection activity in Contacts.
         *
         * @{
         */
        if (getIntent() != null) {
            Bundle data = getIntent().getExtras();
            if (data != null) {
                mLimitCount = data.getInt(KEY_LIMIIT_COUNT, 3500);
                mMode = data.getString(KEY_MODE);
            }
        }

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }
        /**
         * @}
         */
        prepareSearchViewAndActionBar(savedState);
        configureActivityTitle();
    }

    /**
     * Bug709612/719158/720622/758324/903518/903666/907924/909518/915268, If SelectActivity is lunched and contact has no permission,
     * contact can't be selected.
     * @{
     */
    private void needPromptMessage(String className) {
        Log.d(TAG, className);
        if (className.endsWith("FastDialSettingActivity")
                || className.endsWith("BlackCallsListAddActivity")
                || className.endsWith("PeopleActivity")
                || className.endsWith("ContactsPreferenceActivity")
                || className.endsWith(".Launcher")
                // UNISOC: Add for Bug1397814
                || className.endsWith(".ProxyActivityStarter")
                || className.endsWith(".SearchLauncher")
                || className.endsWith("AttachPhotoActivity")
                || className.endsWith("ConversationActivity")
                || className.endsWith(".smil.ui.SmilMainActivity")
                || className.endsWith(".settings.VoicemailSettingsActivity")
                || className.endsWith(".edit.EditInfoActivity")
                || className.endsWith(".incallui.InCallActivity")
                || className.endsWith(".callrecording.ListedNumberActivity")
                || className.endsWith(".GsmUmtsCallForwardOptions")
                || className.endsWith(".callforward.GsmUmtsVideoCallForwardOptions")
                || className.endsWith(".settings.fdn.EditFdnContactScreen")
                || className.endsWith(".ComposeActivityGmailExternal")
                || className.endsWith(".ComposeActivityGmail")) {
            mIsNeedPromptPermissionMessage = true;
        }
    }

    private void showToast(final int message) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactSelectionActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    /**
     * @}
     */

    public boolean isSelectionMode() {
        return mActionBarAdapter.isSelectionMode();
    }

    public boolean isSearchMode() {
        return mActionBarAdapter.isSearchMode();
    }

    private void prepareSearchViewAndActionBar(Bundle savedState) {
        mToolbar = getView(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Add a shadow under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        mActionBarAdapter = new ActionBarAdapter(this, this, getSupportActionBar(), mToolbar,
                R.string.enter_contact_name);
        mActionBarAdapter.setShowHomeIcon(true);
        mActionBarAdapter.setShowHomeAsUp(true);
        mActionBarAdapter.initialize(savedState, mRequest);

        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        mIsSearchSupported = mRequest.getActionCode() != ContactsRequest.ACTION_PICK_POSTAL
                && mRequest.getActionCode() != ContactsRequest.ACTION_PICK_EMAILS
                && mRequest.getActionCode() != ContactsRequest.ACTION_PICK_PHONES
                && !mRequest.isLegacyCompatibilityMode();
        configureSearchMode();
    }

    private void configureSearchMode() {
        mActionBarAdapter.setSearchMode(mIsSearchMode);
        invalidateOptionsMenu();
    }

    /**
     * SPRD: Bug730769 The title of ContactSelectionActivity should be reconfigure when changing language
     * SPRD: Bug736694 mRequest is null leading to NullPointerException when enter into contact from ECC
     * @{
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mRequest != null) {
            configureActivityTitle();
        }
    }
    /**
     * @}
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {// Go back to previous screen, intending "cancel"
            setResult(RESULT_CANCELED);
            onBackPressed();
        } else if (id == R.id.menu_search) {
            mIsSearchMode = !mIsSearchMode;
            configureSearchMode();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
        outState.putBoolean(KEY_SEARCH_MODE, mIsSearchMode);
        outState.putParcelable(KEY_FILTER, mPermanentAccountFilter);
        /**
         * androido-poting bug474752 Add features with multiSelection activity in Contacts.
         *
         * @{
         */
        outState.putInt(KEY_LIMIIT_COUNT, mLimitCount);
        outState.putString(KEY_MODE, mMode);
        /**
         * @}
         */
        if (mActionBarAdapter != null) {
            mActionBarAdapter.onSaveInstanceState(outState);
        }
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(mRequest.getActivityTitle())) {
            getSupportActionBar().setTitle(mRequest.getActivityTitle());
            return;
        }
        int titleResId = -1;
        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                titleResId = R.string.contactInsertOrEditActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_CONTACT: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_PHONE: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_EMAIL: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_PHONES: {
                titleResId = R.string.pickerSelectContactsActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_EMAILS: {
                titleResId = R.string.pickerSelectContactsActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_POSTAL: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_JOIN: {
                titleResId = R.string.titleJoinContactDataWith;
                break;
            }
            case ContactsRequest.ACTION_PICK_GROUP_MEMBERS: {
                titleResId = R.string.groupMemberPickerActivityTitle;
                break;
            }
            /**
             * androido-poting bug474752 Add features with multiSelection activity in Contacts.
             *
             * @{
             */
            case ContactsRequest.ACTION_MULTI_PICK_CONTACT: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_MULTI_PICK_ALL_IN_ONE_DATA: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            /**
             * @}
             */
        }
        if (titleResId > 0) {
            getSupportActionBar().setTitle(titleResId);
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        switch (mActionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setEditMode(true);
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                fragment.setCreateContactEnabled(!mRequest.isSearchMode());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                //SPRD: add for bug720720 it should not show read-only contacts when enter into from other apps
                fragment.setExcludeReadOnly(true);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_DEFAULT:
            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setIncludeFavorites(mRequest.shouldIncludeFavorites());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                /**
                 * SPRD: add for Bug713142, The default contact and sim contacts should not be showed when setting head image.
                 * @{
                 */
                ComponentName callingActivity = getCallingActivity();
                if (callingActivity != null) {
                    String className = callingActivity.getShortClassName();
                    if (className != null && className.endsWith("AttachPhotoActivity")) {
                        fragment.setExcludeReadOnly(true);
                        ContactListFilter filter = getAccountFilterForMimeType(getIntent().getExtras());
                        fragment.setFilterForAttachPhoto(true, filter);
                    }
                }
                /**
                 * @}
                 */
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setCreateContactEnabled(!mRequest.isSearchMode());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setShortcutRequested(true);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                //SPRD: add for bug719535 it can not show FDN contact when create shortcut
                fragment.setListRequestModeSelection("shortcut_contact");
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                /**
                 * SPRD: bug 708294,784837 EditInfoAcvitity need show no sim contacts
                 */
                if (getCallingActivity() != null
                        && (getCallingActivity().getShortClassName().endsWith("EditInfoActivity")
                        || getCallingActivity().getShortClassName().endsWith("EditInfoSuggestion"))) {
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivity.this);
                    List<AccountWithDataSet> allAccounts = am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = new ArrayList<AccountWithDataSet>();
                    accounts.addAll(allAccounts);
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.type
                                .equals("sprd.com.android.account.sim")
                                || accountWithDataSet.type
                                        .equals("sprd.com.android.account.usim")) {
                            iter.remove();
                        }
                    }
                    fragment.setFilter(ContactListFilter
                            .createAccountsFilter(accounts));
                }
                /**
                 * @}
                 */
                fragment.setListType(ListEvent.ListType.PICK_PHONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                mListFragment.setListType(ListEvent.ListType.PICK_EMAIL);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONES: {
                mListFragment = new MultiSelectPhoneNumbersListFragment();
                mListFragment.setArguments(getIntent().getExtras());
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAILS: {
                mListFragment = new MultiSelectEmailAddressesListFragment();
                mListFragment.setArguments(getIntent().getExtras());
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_CALL);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_SENDTO);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                fragment.setListType(ListEvent.ListType.PICK_POSTAL);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_JOIN: {
                JoinContactListFragment joinFragment = new JoinContactListFragment();
                joinFragment.setTargetContactId(getTargetContactId());
                joinFragment.setListType(ListEvent.ListType.PICK_JOIN);
                mListFragment = joinFragment;
                break;
            }
            /**
             * SPRD: Bug693205 Add group feature for Contacts
             * @{
             */
            case ContactsRequest.ACTION_PICK_GROUP_MEMBERS: {
                if (getIntent().hasExtra(UiIntentActions.MOVE_GROUP_METADATA)) {
                    final GroupMetaData groupMetaData = getIntent().getParcelableExtra(UiIntentActions.MOVE_GROUP_METADATA);
                    final ArrayList<String> groupMembersIds = getIntent().getStringArrayListExtra(UiIntentActions.GROUP_CONTACT_IDS);
                    final ArrayList<String> otherGroupIds = getIntent().getStringArrayListExtra(UiIntentActions.TARGET_GROUP_IDS);
                    final ArrayList<String> otherGroupTitles = getIntent().getStringArrayListExtra(UiIntentActions.TARGET_GROUP_TITLES);
                    mListFragment = GroupMemberMoveFragmentSprd.newInstance(groupMetaData, groupMembersIds, otherGroupIds, otherGroupTitles);
                } else {
                    final String accountName = getIntent().getStringExtra(UiIntentActions.GROUP_ACCOUNT_NAME);
                    final String accountType = getIntent().getStringExtra(UiIntentActions.GROUP_ACCOUNT_TYPE);
                    final String accountDataSet = getIntent().getStringExtra(UiIntentActions.GROUP_ACCOUNT_DATA_SET);
                    final ArrayList<String> contactIds = getIntent().getStringArrayListExtra(UiIntentActions.GROUP_CONTACT_IDS);
                    mListFragment = GroupMemberPickerFragment.newInstance(accountName, accountType, accountDataSet, contactIds);
                }
                mListFragment.setListType(ListEvent.ListType.PICK_GROUP_MEMBERS);
                break;
            }
            /**
             * @}
             */

            /**
             * androido-poting bug474752 Add features with multiSelection activity in Contacts.
             *
             * @{
             */
            case ContactsRequest.ACTION_MULTI_PICK_CONTACT: {
                MultiContactsPickerFragment fragment = new MultiContactsPickerFragment();
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                if (getIntent().hasExtra(KEY_MODE)) {
                    switch (getIntent().getExtras().getInt(KEY_MODE)) {
                        case SUBACTIVITY_BATCH_IMPORT:
                            fragment.setListRequestModeSelection(MODE_COPYTO);
                            break;
                        default:
                            break;
                    }
                }
                if (getIntent().hasExtra(DST_ACCOUNT)) {
                    /**
                     * SPRD:Bug 432779 Filter for copy is related to displayed contacts.
                     *
                     * @{
                     */
                    AccountWithDataSet dstAcount = (AccountWithDataSet) getIntent()
                            .getParcelableExtra(DST_ACCOUNT);
                    final AccountTypeManager am = AccountTypeManager.getInstance(ContactSelectionActivity.this);
                    //SPRD:Bug693207 Import/Export vcf contacts.
                    ArrayList<AccountWithDataSet> accounts = new ArrayList(am.getAccounts(false));
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        final AccountWithDataSet accountWithDataSet = iter.next();
                        /**
                         * SPRD:Bug693207 Import/Export vcf contacts.
                         * @{
                         */
                        if(accountWithDataSet.name == null){
                            iter.remove();
                            continue;
                        }
                        /* @}
                         */
                        if (accountWithDataSet.name.equals(dstAcount.name)) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                    /**
                     * @}
                     */
                } else {
                    fragment.setFilter(ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
                }
                if (getIntent().hasExtra("exclude_read_only")) {
                    fragment.setExcludeReadOnly(true);
                }
                if (MODE_SHARE.equals(mMode)) {
                    fragment.setMode(mMode);
                }
                mListFragment = fragment;
                break;
            }
            case ContactsRequest.ACTION_MULTI_PICK_ALL_IN_ONE_DATA: {
                AllInOneDataPickerFragment fragment = new AllInOneDataPickerFragment();
                if (getIntent().hasExtra("select_group_member")) {
                    long groupId = (long) getIntent().getLongExtra("select_group_member", -1);
                    if (getIntent().hasExtra("with_phone_number")) {
                        fragment.setFilter(ContactListFilter.createGroupFilterOnlyPhoneNumber(groupId));
                    } else {
                        fragment.setFilter(ContactListFilter.createGroupFilter(groupId));
                    }
                } else {
                    fragment.setFilter(ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
                }
                fragment.setCascadingData(mRequest.getCascadingData());
                mListFragment = fragment;
                break;
            }
            /**
             * @}
             */
            default:
                throw new IllegalStateException("Invalid action code: " + mActionCode);
        }

        // Setting compatibility is no longer needed for PhoneNumberPickerFragment since that logic
        // has been separated into LegacyPhoneNumberPickerFragment.  But we still need to set
        // compatibility for other fragments.
        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    /**
     * SPRD: add for Bug713142, The default contact and sim contacts should not be showed when setting head image.
     * @{
     */
    private ContactListFilter getAccountFilterForMimeType(Bundle extras) {
        ArrayList<AccountWithDataSet> tmp = AccountsForMimeTypeUtils.getAccountsForMimeType(
                ContactSelectionActivity.this, extras);
        if (tmp.isEmpty()) {
            return null;
        }
        return ContactListFilter.createAccountsFilter(tmp);
    }
    /**
     * @}
     */

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
        if (mRequest.isLegacyCompatibilityMode()) {
            return new LegacyPhoneNumberPickerFragment();
        } else {
            return new PhoneNumberPickerFragment();
        }
    }

    public void setupActionListener() {
        if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else if (mListFragment instanceof MultiSelectEmailAddressesListFragment) {
            ((MultiSelectEmailAddressesListFragment) mListFragment).setCheckBoxListListener(this);
        } else if (mListFragment instanceof MultiSelectPhoneNumbersListFragment) {
            ((MultiSelectPhoneNumbersListFragment) mListFragment).setCheckBoxListListener(this);
        } else if (mListFragment instanceof JoinContactListFragment) {
            ((JoinContactListFragment) mListFragment).setOnContactPickerActionListener(
                    new JoinContactActionListener());
        } else if (mListFragment instanceof GroupMemberPickerFragment) {
            ((GroupMemberPickerFragment) mListFragment).setListener(
                    new GroupMemberPickerListener());
            getMultiSelectListFragment().setCheckBoxListListener(this);
        }
        /**
         * SPRD:Bug 474752 Add features with multi-selection activity in Contacts.
         *
         * @{
         */
        else if (mListFragment instanceof MultiContactsPickerFragment) {
            ((MultiContactsPickerFragment) mListFragment).setListener(
                    new MultiContactsPickerListener());
            getMultiSelectListFragment().setCheckBoxListListener(this);
        } else if (mListFragment instanceof AllInOneDataPickerFragment) {
            ((AllInOneDataPickerFragment) mListFragment).setListener(
                    new AllInOneDataPickerListener());
            getMultiSelectListFragment().setCheckBoxListListener(this);
        }
        /**
         * @}
         */
        /** SPRD: Bug693205 Add group feature for Contacts */
        else if (mListFragment instanceof GroupMemberMoveFragmentSprd) {
            getMultiSelectListFragment().setCheckBoxListListener(this);
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    private MultiSelectContactsListFragment getMultiSelectListFragment() {
        if (mListFragment instanceof MultiSelectContactsListFragment) {
            return (MultiSelectContactsListFragment) mListFragment;
        }
        return null;
    }

    @Override
    public void onAction(int action) {
        switch (action) {
            case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
                /**
                 * SPRD:bug 736015 search mode, add multiSelection function @{
                 **/
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                mIsSearchMode = true;
                configureSearchMode();
                invalidateOptionsMenu();
                /**
                 * @}
                 **/
                break;
            case ActionBarAdapter.Listener.Action.CHANGE_SEARCH_QUERY:
                final String queryString = mActionBarAdapter.getQueryString();
                mListFragment.setQueryString(queryString, /* delaySelection */ false);
                break;
            case ActionBarAdapter.Listener.Action.START_SELECTION_MODE:
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(true);
                }
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.STOP_SEARCH_AND_SELECTION_MODE:
                mListFragment.setQueryString("", /* delaySelection */ false);
                mActionBarAdapter.setSearchMode(false);
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void onUpButtonPressed() {
        onBackPressed();
    }

    @Override
    public void onSelectedTabChanged() {
    }

    @Override
    public void onStartDisplayingCheckBoxes() {
        mActionBarAdapter.setSelectionMode(true);
    }

    @Override
    public void onSelectedContactIdsChanged() {
        if (mListFragment instanceof MultiSelectContactsListFragment) {
            // UNISOC: add for bug1132166, Check two contacts with the same number from the group, showing the selected number is 2.
            int count = getMultiSelectListFragment().getSelectedContactIds().size();
            if (mListFragment instanceof AllInOneDataPickerFragment) {
                count = ((AllInOneDataPickerFragment) mListFragment).getSelectedDatas().size();
            }
            /**
             * SPRD:BUG740636 After closing the address book permissions, mActionBarAdapter is null
             * @{
             **/
            if (mActionBarAdapter != null) {
                mActionBarAdapter.setSelectionCount(count);
                updateAddContactsButton(count);
            }
            /**
             * @}
             **/
            // Show or hide the multi select "Done" button
            invalidateOptionsMenu();
        }
    }

    private void updateAddContactsButton(int count) {
        final ImageButton imageButton = (ImageButton) mActionBarAdapter.getSelectionContainer()
                .findViewById(R.id.add_contacts);
        if (count > 0) {
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMultiPickerSelected()) {
                        getMultiSelectListFragment().onMultiPickerSelected();
                    } else {
                        final long[] contactIds =
                                getMultiSelectListFragment().getSelectedContactIdsArray();
                        returnSelectedContacts(contactIds);
                    }
                }
            });
        } else {
            imageButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStopDisplayingCheckBoxes() {
        mActionBarAdapter.setSelectionMode(false);
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            startActivityAndForwardResult(EditorIntents.createEditContactIntent(
                    ContactSelectionActivity.this, contactLookupUri, /* materialPalette =*/ null,
                    /* photoId =*/ -1));
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }
    }

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickDataUri(Uri dataUri, boolean isVideoCall, int callInitiationType) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onPickPhoneNumber(String phoneNumber, boolean isVideoCall,
                                      int callInitiationType) {
            Log.w(TAG, "Unsupported call.");
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        @Override
        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class JoinContactActionListener implements OnContactPickerActionListener {
        @Override
        public void onPickContactAction(Uri contactUri) {
            Intent intent = new Intent(null, contactUri);
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
        }

        @Override
        public void onCreateNewContactAction() {
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
        }
    }

    private final class GroupMemberPickerListener implements GroupMemberPickerFragment.Listener {

        @Override
        public void onGroupMemberClicked(long contactId) {
            final Intent intent = new Intent();
            intent.putExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, contactId);
            returnPickerResult(intent);
        }

        @Override
        public void onSelectGroupMembers() {
            mActionBarAdapter.setSelectionMode(true);
        }
    }

    /**
     * androido-poting bug474752 Add features with multiSelection activity in Contacts.
     *
     * @{
     */
    private final class MultiContactsPickerListener implements MultiContactsPickerFragment.Listener {

        @Override
        public void onContactClicked(Cursor cursor) {
            if (isMultiPickerSelected()) {
                ArrayList<String> datas = new ArrayList<String>();
                if(cursor != null) {
                    String lookup = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));
                    datas.add(lookup);
                }
                returnPickerResult(datas);
            } else {
                if(cursor != null) {
                    Long contactId = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
                    returnSelectedContacts(new long[]{contactId});
                }
            }
        }

        @Override
        public void onSelectContacts() {
            mActionBarAdapter.setSelectionMode(true);
        }

        @Override
        public void onPickAllInOneDataAction(ArrayList<String> data) {
            returnPickerResult(data);
        }
    }

    private final class AllInOneDataPickerListener implements AllInOneDataPickerFragment.Listener {

        @Override
        public void onDataClicked(HashMap<String, String> data) {
            returnPickerResult(data);
        }

        @Override
        public void onSelectDatas() {
            mActionBarAdapter.setSelectionMode(true);
        }

        @Override
        public void onPickAllInOneDataAction(HashMap<String, String> data) {
            returnPickerResult(data);
        }
    }

    public void returnPickerResult(HashMap<String, String> data) {
        /**
         * SPRD:bug716614 Consistent with the displayed tick options.
         * @{
         */
        int count = getMultiSelectListFragment().getSelectedContactIds().size();
        if (count > mLimitCount) {
            Toast.makeText(this, getString(R.string.contacts_selection_for_mms_limit, mLimitCount), Toast.LENGTH_LONG).show();
            return;
        }
        /**
         * @}
         */
        Intent intent = getIntent();
        if (!data.isEmpty()) {
            intent.putExtra("result", data);
            returnPickerResult(intent);
        }
    }

    public void returnPickerResult(ArrayList<String> data) {
        /**
         * SPRD:bug716614 Consistent with the displayed tick options.
         * @{
         */
        int count = getMultiSelectListFragment().getSelectedContactIds().size();
        if (count > mLimitCount && !MODE_SHARE.equals(mMode)) {
            Toast.makeText(this, getString(R.string.contacts_selection_for_mms_limit, mLimitCount), Toast.LENGTH_LONG).show();
            return;
        }
        /**
         * @}
         */
        Intent intent = getIntent();
        intent.putStringArrayListExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY, data);
        returnPickerResult(intent);
    }

    private boolean isMultiPickerSelected() {
        if ( MODE_SHARE.equals(mMode) || MODE_EXPORT.equals(mMode)
                || MODE_MMC_SEND.equals(mMode)
                || MODE_SELECT_GROUP_MEMBER.equals(mMode)) {
            return true;
        } else {
            return false;
        }
    }

    private void returnSelectedContacts(long[] contactIds) {
        if (contactIds != null && contactIds.length > mLimitCount) {
            Toast.makeText(this, getString(R.string.contacts_selection_for_mms_limit, mLimitCount), Toast.LENGTH_LONG).show();
            return;
        }
        final Intent intent = getIntent();
        intent.putExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY, contactIds);
        returnPickerResult(intent);
    }
    /**
     * @}
     */
    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        try {
            ImplicitIntentsUtil.startActivityInApp(ContactSelectionActivity.this, intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "startActivity() failed: " + e);
            Toast.makeText(ContactSelectionActivity.this, R.string.missing_app,
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.search_view) {
            if (hasFocus) {
                mActionBarAdapter.setFocusOnSearchView();
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.floating_action_button) {
            startCreateNewContactActivity();
        }
    }

    private long getTargetContactId() {
        Intent intent = getIntent();
        final long targetContactId = intent.getLongExtra(
                UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, -1);
        if (targetContactId == -1) {
            Log.e(TAG, "Intent " + intent.getAction() + " is missing required extra: "
                    + UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY);
            setResult(RESULT_CANCELED);
            finish();
            return -1;
        }
        return targetContactId;
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.putExtra(ContactEditorActivity.
                INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
        startActivityAndForwardResult(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchItem.setVisible(!mIsSearchMode && mIsSearchSupported);

        final Drawable searchIcon = searchItem.getIcon();
        if (searchIcon != null) {
            searchIcon.mutate().setColorFilter(ContextCompat.getColor(this,
                    R.color.actionbar_icon_color), PorterDuff.Mode.SRC_ATOP);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            return;
        }

        if (isSelectionMode()) {
            mActionBarAdapter.setSelectionMode(false);
            if (getMultiSelectListFragment() != null) {
                getMultiSelectListFragment().displayCheckBoxes(false);
            }
        } else if (mIsSearchMode) {
            mIsSearchMode = false;
            configureSearchMode();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * SPRD: Bug693205 Add group feature for Contacts
     * @{
     */
    public ActionBarAdapter getActionBarAdapter() {
        return mActionBarAdapter;
    }
    /**
     * @}
     */

    /**
     * SPRD: Bug708280 Add search function for Touch Assistant.
     * @{
     */
    @Override
    public boolean onSearchRequested() {
        if (mToolbar == null) {
            return false;
        }
        final Menu menu = mToolbar.getMenu();
        if (menu == null) {
            return false;
        }
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && searchItem.isVisible() && !isSearchMode()
                && !isSelectionMode()) {
            mIsSearchMode = true;
            configureSearchMode();
            return true;
        }
        return false;
    }
    /**
     * @}
     */
}
