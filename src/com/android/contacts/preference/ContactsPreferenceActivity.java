/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.preference;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.ProviderStatus;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.contacts.R;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.interactions.ImportDialogFragment;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment.ProfileListener;
import com.android.contacts.preference.DisplayOptionsPreferenceFragment.ProfileQuery;
import com.android.contacts.util.AccountSelectionUtil;
/**
 * SPRD:Bug693207 Import/Export vcf contacts.
 * @{
 */
import android.content.Intent;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.os.Parcel;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import android.content.ComponentName;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

import android.provider.ContactsContract.Contacts;
import com.sprd.contacts.BatchOperationService;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.vcard.ExportVCardActivity;
import com.android.contacts.vcard.VCardCommonArguments;
import com.android.contacts.util.Constants;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.model.account.SimAccountType;
import com.android.contacts.model.account.USimAccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.model.AccountTypeManager;
import android.content.ActivityNotFoundException;
import com.sprd.contacts.interactions.ImportExportDialogFragmentSprd;
/* @}
 */
import android.app.ActivityManagerNative;
import com.android.contacts.activities.RequestPermissionsActivity;
/**
 * Contacts settings.
 */
public final class ContactsPreferenceActivity extends PreferenceActivity
        implements ProfileListener, SelectAccountDialogFragment.Listener
        , ImportExportDialogFragmentSprd.Listener{

    private static final String TAG = "ContactsPreferenceActivity";
    private static final String TAG_ABOUT = "about_contacts";
    private static final String TAG_DISPLAY_OPTIONS = "display_options";

    private String mNewLocalProfileExtra;
    private boolean mAreContactsAvailable;

    private ProviderStatusWatcher mProviderStatusWatcher;

    private AppCompatDelegate mCompatDelegate;

    public static final String EXTRA_NEW_LOCAL_PROFILE = "newLocalProfile";

    /**
     * SPRD:Bug693207 Import/Export vcf contacts.
     * @{
     */
    //UNISOC: add for bug1144233 , Modify MAX_DATA_SIZE to 80000
    private static final int MAX_DATA_SIZE = 80000;
    private static final int SUBACTIVITY_BATCH_EXPORT_TO_SDCARD = 1;
    private static final int SUBACTIVITY_SHARE_VISIBLE = 2;
    private static final int SUBACTIVITY_BATCH_IMPORT = 8;
    private static final String DO_EXPORT = "mode_export";
    private static final String DO_SHARE = "mode_share";
    //private static final String CARDDAV_ACCOUNT = "com.sprd.carddav.account";
    private static final String CALLING_ACTIVITY = "com.android.contacts.preference.ContactsPreferenceActivity";
    /* @}
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCompatDelegate = AppCompatDelegate.create(this, null);

        super.onCreate(savedInstanceState);
        mCompatDelegate.onCreate(savedInstanceState);

        final ActionBar actionBar = mCompatDelegate.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }

        mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);

        mNewLocalProfileExtra = getIntent().getStringExtra(EXTRA_NEW_LOCAL_PROFILE);
        final int providerStatus = mProviderStatusWatcher.getProviderStatus();
        mAreContactsAvailable = providerStatus == ProviderStatus.STATUS_NORMAL;

        if (savedInstanceState == null) {
            final DisplayOptionsPreferenceFragment fragment = DisplayOptionsPreferenceFragment
                    .newInstance(mNewLocalProfileExtra, mAreContactsAvailable);
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment, TAG_DISPLAY_OPTIONS)
                    .commit();
            setActivityTitle(R.string.activity_title_settings);
        } else {
            final AboutPreferenceFragment aboutFragment = (AboutPreferenceFragment)
                    getFragmentManager().findFragmentByTag(TAG_ABOUT);

            if (aboutFragment != null) {
                setActivityTitle(R.string.setting_about);
            } else {
                setActivityTitle(R.string.activity_title_settings);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mCompatDelegate.onPostCreate(savedInstanceState);
    }

    public void setSupportActionBar(Toolbar toolbar) {
        mCompatDelegate.setSupportActionBar(toolbar);
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return mCompatDelegate.getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutRes) {
        mCompatDelegate.setContentView(layoutRes);
    }

    @Override
    public void setContentView(View view) {
        mCompatDelegate.setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mCompatDelegate.setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        mCompatDelegate.addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mCompatDelegate.onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        mCompatDelegate.setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mCompatDelegate.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompatDelegate.onDestroy();
    }

    @Override
    public void invalidateOptionsMenu() {
        mCompatDelegate.invalidateOptionsMenu();
    }

    protected void showAboutFragment() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, AboutPreferenceFragment.newInstance(), TAG_ABOUT)
                .addToBackStack(null)
                .commit();
        setActivityTitle(R.string.setting_about);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    /**
     * SPRD: Bug717880, close phone permission, then pinned ContactsPreferenceActivity, contacts crash
     * @{
     */
    @Override
    public void onBackPressed() {
        if (isInLockTaskMode() && RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        } else {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                setActivityTitle(R.string.activity_title_settings);
                getFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }

    private boolean isInLockTaskMode() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * @}
     */

    private void setActivityTitle(@StringRes int res) {
        final ActionBar actionBar = mCompatDelegate.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(res);
        }
    }

    @Override
    public void onProfileLoaded(Cursor cursor) {
        boolean hasProfile = false;
        String displayName = null;
        long contactId = -1;
        int displayNameSource = DisplayNameSources.UNDEFINED;
        if (cursor != null && cursor.moveToFirst()) {
            hasProfile = cursor.getInt(ProfileQuery.CONTACT_IS_USER_PROFILE) == 1;
            displayName = cursor.getString(ProfileQuery.CONTACT_DISPLAY_NAME);
            contactId = cursor.getLong(ProfileQuery.CONTACT_ID);
            displayNameSource = cursor.getInt(ProfileQuery.DISPLAY_NAME_SOURCE);
        }
        if (hasProfile && TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.missing_name);
        }
        final DisplayOptionsPreferenceFragment fragment = (DisplayOptionsPreferenceFragment)
                getFragmentManager().findFragmentByTag(TAG_DISPLAY_OPTIONS);
        fragment.updateMyInfoPreference(hasProfile, displayName, contactId, displayNameSource);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
       /**
         * SPRD:Bug693207 Import/Export vcf contacts.
         *
         * Original Android code:
        AccountSelectionUtil.doImport(this, extraArgs.getInt(ImportDialogFragment
                .KEY_RES_ID), account, extraArgs.getInt(ImportDialogFragment.KEY_SUBSCRIPTION_ID));
         * @{
         */
        if (account != null) {
            this.doImport(account);
        }
        /* @}
         */
    }
    /**
     * SPRD:Bug693207 Import/Export vcf contacts.
     * @{
     */
    public void doImport(final AccountWithDataSet dstAccount) {
        if (dstAccount != null && (!isFinishing())
                && (SimAccountType.ACCOUNT_TYPE.equals(dstAccount.type) || USimAccountType.ACCOUNT_TYPE
                        .equals(dstAccount.type))) {
            Bundle args = new Bundle();
            args.putParcelable("accounts", dstAccount);
            ConfirmCopyContactDialogFragment dialog =
                    new ConfirmCopyContactDialogFragment();
            dialog.setArguments(args);
            //UNISOC: add for bug1146596, java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            dialog.showAllowingStateLoss(getFragmentManager(), null);
        } else {
            Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION);
            intent.setData(Contacts.CONTENT_URI);
            intent.putExtra("dst_account", dstAccount);
            intent.putExtra("mode", SUBACTIVITY_BATCH_IMPORT);
            startActivityForResult(intent, SUBACTIVITY_BATCH_IMPORT);
        }
    }

    public static class ConfirmCopyContactDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION);
                                    intent.setData(Contacts.CONTENT_URI);
                                    AccountWithDataSet accountData = (AccountWithDataSet) getArguments()
                                            .getParcelable("accounts");
                                    intent.putExtra("dst_account", accountData);
                                    intent.putExtra("mode", SUBACTIVITY_BATCH_IMPORT);
                                    getActivity().startActivityForResult(intent,
                                            SUBACTIVITY_BATCH_IMPORT);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.alert_maybe_lost_info)
                    .create();
        }
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    public void doPreImport(int resId, int subscriptionId) {
        if (hasWritableAccount()) {
            ImportToAccountDialogFragment dialogFragment = ImportToAccountDialogFragment
                    .newInstance(resId);
            //UNISOC: add for bug1146596, java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            dialogFragment.showAllowingStateLoss(getFragmentManager(), null);
        } else {
            AccountSelectionUtil.doImport(this, resId, AccountTypeManager.getInstance(this)
                    .getPhoneAccount(),subscriptionId);
        }
    }

    /**
     * UNISOC: Bug1147284 it can not import vcard to Google account after add Google account. @{
     */
    private boolean hasWritableAccount() {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        final List<AccountWithDataSet> accountList = accountTypes.blockForWritableAccounts();
        Iterator<AccountWithDataSet> iter = accountList.iterator();
        while (iter.hasNext()) {
            AccountWithDataSet accountWithDataSet = iter.next();
            if (SimAccountType.ACCOUNT_TYPE.equals(accountWithDataSet.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(accountWithDataSet.type)) {
                iter.remove();
            }
        }
        if (accountList.size() > 1) {
            return true;
        }
        return false;
    }
    /* @} */

    public void doExport() {
        final Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION);
        intent.setData(Contacts.CONTENT_URI);
        intent.putExtra("exclude_read_only", true);
        intent.putExtra("mode", DO_EXPORT);
        startActivityForResult(intent, SUBACTIVITY_BATCH_EXPORT_TO_SDCARD);
    }

    public void doCopy() {
        Bundle args = new Bundle();
        SelectAccountDialogFragment.show(getFragmentManager(),
                R.string.copy_to,
                AccountTypeManager.AccountFilter.CONTACTS_WRITABLE,
                args);
    }

    public void doShareVisible() {
        final Intent intent = new Intent(UiIntentActions.MULTI_PICK_ACTION);
        intent.setData(Contacts.CONTENT_URI);
        intent.putExtra("mode", DO_SHARE);
        startActivityForResult(intent, SUBACTIVITY_SHARE_VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_BATCH_EXPORT_TO_SDCARD: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY);
                    Intent exportIntent = new Intent(ContactsPreferenceActivity.this,
                            ExportVCardActivity.class);

                    exportIntent.putStringArrayListExtra("lookup_keys", lookupKeys);

                    exportIntent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY, CALLING_ACTIVITY);
                    startActivity(exportIntent);
                }
                break;
            }
            case SUBACTIVITY_SHARE_VISIBLE: {
                if (resultCode == RESULT_OK) {
                    ArrayList<String> lookupKeys = data
                            .getStringArrayListExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY);
                    StringBuilder uriListBuilder = new StringBuilder();
                    int index = 0;
                    for (String key : lookupKeys) {
                        if (index != 0)
                            uriListBuilder.append(':');
                        uriListBuilder.append(key);
                        index++;
                    }
                    Uri uri = Uri.withAppendedPath(
                            Contacts.CONTENT_MULTI_VCARD_URI,
                            Uri.encode(uriListBuilder.toString()));
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    Parcel parcel = Parcel.obtain();
                    intent.setType(Contacts.CONTENT_VCARD_TYPE);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    try {
                        intent.writeToParcel(parcel, 0);
                        if(Constants.DEBUG)
                            Log.d(TAG, "Shared parcel size is" + parcel.dataSize());
                        if (parcel.dataSize() > MAX_DATA_SIZE) {
                            Toast.makeText(ContactsPreferenceActivity.this, R.string.transaction_too_large,
                                    Toast.LENGTH_LONG).show();
                            break;
                        }
                    } finally {
                        parcel.recycle();
                    }

                    /**
                     * SPRD:Bug 710254 The shareContact interface is diferent between contacts list interface.
                     * @{
                     */
                    // Launch chooser to share contact via
                    final CharSequence chooseTitle = getResources().getQuantityString(
                            R.plurals.title_share_via, /* quantity */ 1);
                    final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);
                    try {
                        ImplicitIntentsUtil.startActivityOutsideApp(this,chooseIntent);
                    } catch (final ActivityNotFoundException ex) {
                        Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
                    }
                    /**
                     * @}
                     */
                }
                break;
            }
            case SUBACTIVITY_BATCH_IMPORT: {
                if (resultCode == RESULT_OK) {
                    long[] ids = data
                            .getLongArrayExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY);

                    AccountWithDataSet account = (AccountWithDataSet) (data
                            .getParcelableExtra("dst_account"));
                    if (ids != null && ids.length>0 && account != null) {
                        Intent intent = new Intent(data);
                        intent.setComponent(new ComponentName(ContactsPreferenceActivity.this,
                                BatchOperationService.class));
                        intent.putExtra(
                                BatchOperationService.KEY_MODE,
                                BatchOperationService.MODE_START_BATCH_IMPORT_EXPORT);
                        startService(intent);
                    }
                }
                break;
            }
        }
    }

    public static class ImportToAccountDialogFragment extends DialogFragment {

        public static ImportToAccountDialogFragment newInstance(int resId) {
            ImportToAccountDialogFragment fragment = new ImportToAccountDialogFragment();
            Bundle args = new Bundle();
            args.putInt("resId", resId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            final int resId = args.getInt("resId");
            return AccountSelectionUtil.getSelectAccountDialog(getActivity(), resId, null, null,
                    true);
        }
    }
    /* @}
     */
}
