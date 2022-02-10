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

package com.android.contacts.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;

import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.Experiments;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountTypeProvider;
import com.android.contacts.model.account.AccountTypeWithDataSet;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.model.account.GoogleAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.concurrent.ContactsExecutors;
import com.android.contactsbind.experiments.Flags;
import com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import android.database.Cursor;
import android.content.ContentValues;
import android.provider.ContactsContract.Data;

/**
 * SPRD:Bug 693198 Support sdn numbers read in Contacts.
 * @{
 */
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.android.contacts.model.account.AccountType.EditType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
/**
* @}
*/
/*
 * SPRD: Bug680321 for speedup contactslist flush when scroll
 * @{
 */
import com.sprd.contacts.util.ActiveDataManager;
/*
 * @}
 */
/**
 * Singleton holder for all parsed {@link AccountType} available on the
 * system, typically filled through {@link PackageManager} queries.
 */
public abstract class AccountTypeManager {
    static final String TAG = "AccountTypeManager";

    private static final Object mInitializationLock = new Object();
    private static AccountTypeManager mAccountTypeManager;

    /**
     * SPRD:Bug505150 Third-part icons show in contactsitemlist.
     * @{
     */
        public static final String ACCOUNT_SIM = "sprd.com.android.account.sim";
        public static final String ACCOUNT_USIM = "sprd.com.android.account.usim";
        public static final String NAME_SIM = "SIM";
        protected int[] simIconRes = {
                R.drawable.ic_sim_card_multi_sim1, R.drawable.ic_sim_card_multi_sim2,
                R.drawable.ic_sim_card_multi_sim3, R.drawable.ic_sim_card_multi_sim4,
                R.drawable.ic_sim_card_multi_sim5
        };
        //SPRD: add for bug617830, add fdn feature
        protected int[] simFdnIconRes = {
            R.drawable.ic_sim_card_multi_sim1_fdn, R.drawable.ic_sim_card_multi_sim2_fdn,
            R.drawable.ic_sim_card_multi_sim3_fdn, R.drawable.ic_sim_card_multi_sim4_fdn,
            R.drawable.ic_sim_card_multi_sim5_fdn
        };
        /**
         * @}
         */

        /**
         * SPRD:bug 693286,490245 add for orange_ef anr/aas/sne feature
         * @{
         */
        public String mIccAasUri = null;
        public String iccSneUri =  null;
        public static class SimAas {
            public String name;
            public String index;
            static final int NAME_INDEX = 0;
            static final int INDEX_INDEX = 1;
        }
        public abstract ArrayList<SimAas> getAasList(String accName);
        public abstract int getSneSize();
        public abstract Uri insertAas(Context context, String aas, Account account);
        public abstract boolean updateAas(Context context, String aasIndex, String aas, Account account);
        public abstract boolean deleteAas(Context context, String aasIndex, String aas, Account account);
        public abstract boolean findAasInContacts(Context context, String aasIndex, String aas);
        public abstract void loadAasList(Context context, Account account);
        public abstract void querySneSize(Context context, Account account);
        /**
         * @}
         */

    public static final String BROADCAST_ACCOUNTS_CHANGED = AccountTypeManager.class.getName() +
            ".AccountsChanged";
    /**
     * SPRD:Bug 693198 Support sdn numbers read in Contacts.
     * @{
     */
    protected int[] simSdnIconRes = {
         R.drawable.ic_sim_card_multi_sim1_sdn, R.drawable.ic_sim_card_multi_sim2_sdn,
         R.drawable.ic_sim_card_multi_sim3_sdn, R.drawable.ic_sim_card_multi_sim4_sdn,
         R.drawable.ic_sim_card_multi_sim5_sdn
    };
    /**
    * @}
    */
    public enum AccountFilter implements Predicate<AccountInfo> {
        ALL {
            @Override
            public boolean apply(@Nullable AccountInfo input) {
                return input != null;
            }
        },
        CONTACTS_WRITABLE {
            @Override
            public boolean apply(@Nullable AccountInfo input) {
                return input != null && input.getType().areContactsWritable();
            }
        },
        GROUPS_WRITABLE {
            @Override
            public boolean apply(@Nullable AccountInfo input) {
                return input != null && input.getType().isGroupMembershipEditable();
            }
        };
    }

    /**
     * Requests the singleton instance of {@link AccountTypeManager} with data bound from
     * the available authenticators. This method can safely be called from the UI thread.
     */
    public static AccountTypeManager getInstance(Context context) {
        if (!hasRequiredPermissions(context)) {
            // Hopefully any component that depends on the values returned by this class
            // will be restarted if the permissions change.
            return EMPTY;
        }
        synchronized (mInitializationLock) {
            if (mAccountTypeManager == null) {
                context = context.getApplicationContext();
                mAccountTypeManager = new AccountTypeManagerImpl(context);
            }
        }
        return mAccountTypeManager;
    }

    /**
     * Set the instance of account type manager.  This is only for and should only be used by unit
     * tests.  While having this method is not ideal, it's simpler than the alternative of
     * holding this as a service in the ContactsApplication context class.
     *
     * @param mockManager The mock AccountTypeManager.
     */
    public static void setInstanceForTest(AccountTypeManager mockManager) {
        synchronized (mInitializationLock) {
            mAccountTypeManager = mockManager;
        }
    }

    private static final AccountTypeManager EMPTY = new AccountTypeManager() {

        @Override
        public ListenableFuture<List<AccountInfo>> getAccountsAsync() {
            return Futures.immediateFuture(Collections.<AccountInfo>emptyList());
        }

        @Override
        public ListenableFuture<List<AccountInfo>> filterAccountsAsync(
                Predicate<AccountInfo> filter) {
            return Futures.immediateFuture(Collections.<AccountInfo>emptyList());
        }

        @Override
        public AccountInfo getAccountInfoForAccount(AccountWithDataSet account) {
            return null;
        }

        @Override
        public Account getDefaultGoogleAccount() {
            return null;
        }

        @Override
        public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
            return null;
        }

        @Override
        public List<AccountWithDataSet> getSimAccounts() {
            return null;
        }

        @Override
        public AccountWithDataSet getPhoneAccount() {
            return null;
        }

        /**
         * SPRD:Bug505150 Third-part icons show in contactsitemlist.
         * @{
         */
        @Override
        public Drawable getListSimIcon(String accountType, String accountName, boolean isSdn) {return null;}

        @Override
        public Drawable getAccountIcon(com.android.contacts.model.account.AccountWithDataSet account, boolean isSdn){return null;}

        @Override
        public boolean isSimAccount(com.android.contacts.model.account.AccountWithDataSet account) {return false;}

        @Override
        public Drawable getListFdnIcon(int phoneId){return null;}
        /**
         * @}
         */

        /**
         * SPRD:bug 693286,490245 add for orange_ef anr/aas/sne feature
         * @{
         */
        @Override
        public ArrayList<SimAas> getAasList(String accName){
            return null;
        }
        @Override
        public int getSneSize(){
            return 0;
        }
        @Override
        public Uri insertAas(Context context, String aas, Account account){
            return null;
        }
        @Override
        public boolean updateAas(Context context, String aasIndex, String aas, Account account){
            return false;
        }
        @Override
        public boolean deleteAas(Context context, String aasIndex, String aas, Account account){
            return false;
        }
        @Override
        public boolean findAasInContacts(Context context, String aasIndex, String aas){
            return false;
        }
        @Override
        public void loadAasList(Context context, Account account){
        }
        @Override
        public void querySneSize(Context context, Account account){
        }
        /**
         * @}
         */

        /**
         * SPRD:Bug 693198 Support sdn numbers read in Contacts.
         * @{
         */
        @Override
        public boolean isPhoneAccount(AccountWithDataSet account){
            return false;
        }
        /**
        * @}
        */

    };

    /**
     * Returns the list of all accounts (if contactWritableOnly is false) or just the list of
     * contact writable accounts (if contactWritableOnly is true).
     *
     * <p>TODO(mhagerott) delete this method. It's left in place to prevent build breakages when
     * this change is automerged. Usages of this method in downstream branches should be
     * replaced with an asynchronous account loading pattern</p>
     */
    public List<AccountWithDataSet> getAccounts(boolean contactWritableOnly) {
        return contactWritableOnly
                ? blockForWritableAccounts()
                : AccountInfo.extractAccounts(Futures.getUnchecked(getAccountsAsync()));
    }

    /**
     * Returns all contact writable accounts
     *
     * <p>In general this method should be avoided. It exists to support some legacy usages of
     * accounts in infrequently used features where refactoring to asynchronous loading is
     * not justified. The chance that this will actually block is pretty low if the app has been
     * launched previously</p>
     */
    public List<AccountWithDataSet> blockForWritableAccounts() {
        return AccountInfo.extractAccounts(
                Futures.getUnchecked(filterAccountsAsync(AccountFilter.CONTACTS_WRITABLE)));
    }

    /**
     * Loads accounts in background and returns future that will complete with list of all accounts
     */
    public abstract ListenableFuture<List<AccountInfo>> getAccountsAsync();

    /**
     * Loads accounts and applies the fitler returning only for which the predicate is true
     */
    public abstract ListenableFuture<List<AccountInfo>> filterAccountsAsync(
            Predicate<AccountInfo> filter);

    public abstract AccountInfo getAccountInfoForAccount(AccountWithDataSet account);

    /**
     * Returns the default google account.
     */
    public abstract Account getDefaultGoogleAccount();

    /**
     * SPRD: add for bug474772, display SIM capacity
     * @{
     */
    public abstract List<AccountWithDataSet> getSimAccounts();
    public abstract AccountWithDataSet getPhoneAccount();
    /**
     * SPRD:Bug 693198 Support sdn numbers read in Contacts.
     * @{
     */
    public abstract boolean isPhoneAccount(AccountWithDataSet account);
    /**
    * @}
    */
    /**
     * SPRD:Bug505150 Third-part icons show in contactsitemlist.
     * @{
     */
    public abstract boolean isSimAccount(com.android.contacts.model.account.AccountWithDataSet account);
    public abstract Drawable getListSimIcon(String accountType, String accountName, boolean isSdn);
    public abstract Drawable getAccountIcon(com.android.contacts.model.account.AccountWithDataSet account, boolean isSdn);
    //SPRD: add for bug617830, add fdn feature
    public abstract Drawable getListFdnIcon(int phoneId);
    /**
     * @}
     */

    /**
     * Returns the Google Accounts.
     *
     * <p>This method exists in addition to filterAccountsByTypeAsync because it should be safe
     * to call synchronously.
     * </p>
     */
    public List<AccountInfo> getWritableGoogleAccounts() {
        // This implementation may block and should be overridden by the Impl class
        return Futures.getUnchecked(filterAccountsAsync(new Predicate<AccountInfo>() {
            @Override
            public boolean apply(@Nullable AccountInfo input) {
                return  input.getType().areContactsWritable() &&
                        GoogleAccountType.ACCOUNT_TYPE.equals(input.getType().accountType);
            }
        }));
    }

    /**
     * Returns true if there are real accounts (not "local" account) in the list of accounts.
     */
    public boolean hasNonLocalAccount() {
        final List<AccountWithDataSet> allAccounts =
                AccountInfo.extractAccounts(Futures.getUnchecked(getAccountsAsync()));
        if (allAccounts == null || allAccounts.size() == 0) {
            return false;
        }
        if (allAccounts.size() > 1) {
            return true;
        }
        return !allAccounts.get(0).isNullAccount();
    }

    static Account getDefaultGoogleAccount(AccountManager accountManager,
            SharedPreferences prefs, String defaultAccountKey) {
        // Get all the google accounts on the device
        final Account[] accounts = accountManager.getAccountsByType(
                GoogleAccountType.ACCOUNT_TYPE);
        if (accounts == null || accounts.length == 0) {
            return null;
        }

        // Get the default account from preferences
        final String defaultAccount = prefs.getString(defaultAccountKey, null);
        final AccountWithDataSet accountWithDataSet = defaultAccount == null ? null :
                AccountWithDataSet.unstringify(defaultAccount);

        // Look for an account matching the one from preferences
        if (accountWithDataSet != null) {
            for (int i = 0; i < accounts.length; i++) {
                if (TextUtils.equals(accountWithDataSet.name, accounts[i].name)
                        && TextUtils.equals(accountWithDataSet.type, accounts[i].type)) {
                    return accounts[i];
                }
            }
        }

        // Just return the first one
        return accounts[0];
    }

    public abstract AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet);

    public final AccountType getAccountType(String accountType, String dataSet) {
        return getAccountType(AccountTypeWithDataSet.get(accountType, dataSet));
    }

    public final AccountType getAccountTypeForAccount(AccountWithDataSet account) {
        if (account != null) {
            return getAccountType(account.getAccountTypeWithDataSet());
        }
        return getAccountType(null, null);
    }

    /**
     * Find the best {@link DataKind} matching the requested
     * {@link AccountType#accountType}, {@link AccountType#dataSet}, and {@link DataKind#mimeType}.
     * If no direct match found, we try searching {@link FallbackAccountType}.
     */
    public DataKind getKindOrFallback(AccountType type, String mimeType) {
        return type == null ? null : type.getKindForMimetype(mimeType);
    }

    /**
     * Returns whether the specified account still exists
     */
    public boolean exists(AccountWithDataSet account) {
        final List<AccountWithDataSet> accounts =
                AccountInfo.extractAccounts(Futures.getUnchecked(getAccountsAsync()));
        return accounts.contains(account);
    }

    /**
     * Returns whether the specified account is writable
     *
     * <p>This checks that the account still exists and that
     * {@link AccountType#areContactsWritable()} is true</p>
     */
    public boolean isWritable(AccountWithDataSet account) {
        return exists(account) && getAccountInfoForAccount(account).getType().areContactsWritable();
    }

    public boolean hasGoogleAccount() {
        return getDefaultGoogleAccount() != null;
    }

    private static boolean hasRequiredPermissions(Context context) {
        final boolean canGetAccounts = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED;
        final boolean canReadContacts = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        return canGetAccounts && canReadContacts;
    }

    public static Predicate<AccountInfo> writableFilter() {
        return AccountFilter.CONTACTS_WRITABLE;
    }

    public static Predicate<AccountInfo> groupWritableFilter() {
        return AccountFilter.GROUPS_WRITABLE;
    }

    /**
     * @return max length of TextFieldsEditor by chars or bytes.
     */
    public int getAccountTypeFieldsMaxLength(Context context, Account account,
            String mimeType) {
        if (mimeType == null || context == null) {
            return -1;
        }
        AccountType bAccountType = getAccountType(AccountTypeWithDataSet.get(account == null ?
                null : account.type, null));
        if (bAccountType != null) {
            return bAccountType.getAccountTypeFieldsLength(context, account,
                    mimeType);
        }
        return -1;
    }

    /**
     * @return max length of TextFieldsEditor by chars.
     */
    public int getTextFieldsEditorMaxLength(Context context, Account account,
            String txtString, int maxLength) {
        if (context == null || maxLength <= 0) {
            return -1;
        }
        AccountType bAccountType = getAccountType(AccountTypeWithDataSet.get(account == null ?
                null : account.type, null));
        if (bAccountType != null) {
            return bAccountType.getTextFieldsEditorLength(txtString, maxLength);
        }
        return -1;
    }
}

class AccountTypeManagerImpl extends AccountTypeManager
        implements OnAccountsUpdateListener, SyncStatusObserver {

    private final Context mContext;
    private final AccountManager mAccountManager;
    private final DeviceLocalAccountLocator mLocalAccountLocator;
    private final Executor mMainThreadExecutor;
    private final ListeningExecutorService mExecutor;
    private AccountTypeProvider mTypeProvider;

    private final AccountType mFallbackAccountType;

    /**
     * SPRD: SPRD: add for bug474772, display SIM capacity
     * @{
     */
    private AccountWithDataSet mPhoneAccount;
    private List<AccountWithDataSet> mSimAccounts = new ArrayList();
    /**
     * @}
     */

    private ListenableFuture<List<AccountWithDataSet>> mLocalAccountsFuture;
    private ListenableFuture<AccountTypeProvider> mAccountTypesFuture;

    private List<AccountWithDataSet> mLocalAccounts = new ArrayList<>();
    private List<AccountWithDataSet> mAccountManagerAccounts = new ArrayList<>();

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private final Function<AccountTypeProvider, List<AccountWithDataSet>> mAccountsExtractor =
            new Function<AccountTypeProvider, List<AccountWithDataSet>>() {
                @Nullable
                @Override
                public List<AccountWithDataSet> apply(@Nullable AccountTypeProvider typeProvider) {
                    return getAccountsWithDataSets(mAccountManager.getAccounts(), typeProvider);
                }
            };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Don't use reloadAccountTypesIfNeeded when packages change in case a contacts.xml
            // was updated.
            reloadAccountTypes();
        }
    };

    /**
     * Internal constructor that only performs initial parsing.
     */
    public AccountTypeManagerImpl(Context context) {
        mContext = context;
        mLocalAccountLocator = DeviceLocalAccountLocator.create(context);
        mTypeProvider = new AccountTypeProvider(context);
        mFallbackAccountType = new FallbackAccountType(context);

        mAccountManager = AccountManager.get(mContext);

        mExecutor = ContactsExecutors.getDefaultThreadPoolExecutor();
        mMainThreadExecutor = ContactsExecutors.newHandlerExecutor(mMainThreadHandler);

        // Request updates when packages or accounts change
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mBroadcastReceiver, filter);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        mContext.registerReceiver(mBroadcastReceiver, sdFilter);

        // Request updates when locale is changed so that the order of each field will
        // be able to be changed on the locale change.
        filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        mAccountManager.addOnAccountsUpdatedListener(this, mMainThreadHandler, false);

        ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, this);

        if (Flags.getInstance().getBoolean(Experiments.CP2_DEVICE_ACCOUNT_DETECTION_ENABLED)) {
            // Observe changes to RAW_CONTACTS so that we will update the list of "Device" accounts
            // if a new device contact is added.
            mContext.getContentResolver().registerContentObserver(
                    ContactsContract.RawContacts.CONTENT_URI, /* notifyDescendents */ true,
                    new ContentObserver(mMainThreadHandler) {
                        @Override
                        public boolean deliverSelfNotifications() {
                            return true;
                        }

                        @Override
                        public void onChange(boolean selfChange) {
                            reloadLocalAccounts();
                        }

                        @Override
                        public void onChange(boolean selfChange, Uri uri) {
                            reloadLocalAccounts();
                        }
                    });
        }
        loadAccountTypes();
    }

    /**
     * SPRD:bug 693286,490245 add for orange_ef anr/aas/sne feature
     * @{
     */
    @Override
    public ArrayList<SimAas> getAasList(String accName) {
        return mAasListAll.get(accName);
    }

    public Map<String, ArrayList<SimAas>> mAasListAll = new HashMap<>();
    public int sneSize = 0;

    public int getSneSize() {
        return sneSize;
    }

    @Override
    public void loadAasList(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();
        mIccAasUri = AccountManager.get(context).getUserData(account,"icc_aas_uri");

        //avoid NPE
        if (mIccAasUri == null) {
            Log.d(TAG,"loadAasList: mIccAasUri is null, so return");
            return;
        }

        Cursor aasCursor = resolver.query(Uri.parse(mIccAasUri), new String[]{"aas","index"}, null, null, null);
        ArrayList<SimAas> aasList = new ArrayList<SimAas>();
        if (aasCursor!=null && aasCursor.moveToFirst()) {
            do {
                SimAas aas = new SimAas();
                aas.index = aasCursor.getString(SimAas.INDEX_INDEX);
                aas.name = aasCursor.getString(SimAas.NAME_INDEX);
                aasList.add(aas);
                Log.d(TAG,"loadAasList: aasIndex ==" + aas.index + ",aasTitle == " + aas.name);
            } while (aasCursor.moveToNext());
        }
        if (aasCursor != null) {
            aasCursor.close();
        }
        mAasListAll.put(account.name, aasList);
    }

    @Override
    public void querySneSize(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();
        iccSneUri = AccountManager.get(context).getUserData(account,"icc_sne_uri");
        Log.d(TAG,"iccSneUri == " + iccSneUri);
        int size = 0;
        if (iccSneUri != null) {
            Cursor sneCursor = resolver.query(Uri.parse(iccSneUri), new String[]{"size"}, null, null, null);
            if (sneCursor.moveToFirst()) {
                do {
                    size = sneCursor.getInt(0);
                } while (sneCursor.moveToNext());
            }
            if (sneCursor != null) {
                sneCursor.close();
            }
        }
        Log.d(TAG,"getSneSize == " + size);
        sneSize =  size;
    }
    @Override
    public Uri insertAas(Context context, String aas, Account account) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("aas", aas);
        mIccAasUri = AccountManager.get(context).getUserData(account,"icc_aas_uri");
        Uri uri = resolver.insert(Uri.parse(mIccAasUri), values);
        return uri;
    }

    @Override
    public boolean updateAas(Context context, String aasIndex, String aas, Account account) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("aas", aas);
        values.put("index", aasIndex);
        mIccAasUri = AccountManager.get(context).getUserData(account,"icc_aas_uri");
        int update = resolver.update(Uri.parse(mIccAasUri), values, null, null);
        if (update > 0) {
            updateContactData(context, aas, aasIndex);
            context.sendBroadcast(new Intent("android.intent.action.AAS_DISMISSDIALOG"));
            return true;
        }
        //UNISOC: add for bug1012855, add for orange_ef anr/aas/sne feature
        context.sendBroadcast(new Intent("android.intent.action.AAS_DISMISSDIALOG"));
        return false;
    }

    private boolean updateContactData(Context context, String aas, String aasIndex) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("data3",aas);
        values.put("data5",aasIndex);
        resolver.update(Data.CONTENT_URI, values, "mimetype_id = 5 and data2 = 0 and data5 = " + aasIndex, null);
        return true;
    }

    @Override
    public boolean deleteAas(Context context, String aasIndex, String aas, Account account) {
        ContentResolver resolver = context.getContentResolver();
        mIccAasUri = AccountManager.get(context).getUserData(account,"icc_aas_uri");
        int update = resolver.delete(Uri.parse(mIccAasUri), String.valueOf(aasIndex), null);
        if (update > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean findAasInContacts(Context context, String aasIndex, String aas) {
        ContentResolver resolver = context.getContentResolver();
        String selection = Data.DATA2 + " = 0 and " + Data.DATA3 + " = '" + aas
                + "' and " + Data.DATA5 + " = " + aasIndex + " and mimetype_id = 5";
        Cursor cursor = resolver.query(Data.CONTENT_URI,
                new String[] {Data.RAW_CONTACT_ID}, selection, null, null);
        // UNISOC: Bug1133522 cursor is not closed
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return false;
    }
    /**
     * @}
     */

    @Override
    public void onStatusChanged(int which) {
        reloadAccountTypesIfNeeded();
    }

    /* This notification will arrive on the UI thread */
    public void onAccountsUpdated(Account[] accounts) {
        reloadLocalAccounts();
        maybeNotifyAccountsUpdated(mAccountManagerAccounts,
                getAccountsWithDataSets(accounts, mTypeProvider));
    }

    private void maybeNotifyAccountsUpdated(List<AccountWithDataSet> current,
            List<AccountWithDataSet> update) {
        if (Objects.equal(current, update)) {
            return;
        }
        current.clear();
        current.addAll(update);
        notifyAccountsChanged();
    }

    private void notifyAccountsChanged() {
        ContactListFilterController.getInstance(mContext).checkFilterValidity(true);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                new Intent(BROADCAST_ACCOUNTS_CHANGED));
    }

    private synchronized void startLoadingIfNeeded() {
        if (mTypeProvider == null && mAccountTypesFuture == null) {
            reloadAccountTypesIfNeeded();
        }
        if (mLocalAccountsFuture == null) {
            reloadLocalAccounts();
        }
    }

    private synchronized void loadAccountTypes() {
        mTypeProvider = new AccountTypeProvider(mContext);

        mAccountTypesFuture = mExecutor.submit(new Callable<AccountTypeProvider>() {
            @Override
            public AccountTypeProvider call() throws Exception {
                // This will request the AccountType for each Account forcing them to be loaded
                getAccountsWithDataSets(mAccountManager.getAccounts(), mTypeProvider);
                return mTypeProvider;
            }
        });
    }

    private FutureCallback<List<AccountWithDataSet>> newAccountsUpdatedCallback(
            final List<AccountWithDataSet> currentAccounts) {
        return new FutureCallback<List<AccountWithDataSet>>() {
            @Override
            public void onSuccess(List<AccountWithDataSet> result) {
                maybeNotifyAccountsUpdated(currentAccounts, result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        };
    }

    private synchronized void reloadAccountTypesIfNeeded() {
        if (mTypeProvider == null || mTypeProvider.shouldUpdate(
                mAccountManager.getAuthenticatorTypes(), ContentResolver.getSyncAdapterTypes())) {
            reloadAccountTypes();
        }
    }

    private synchronized void reloadAccountTypes() {
        loadAccountTypes();
        Futures.addCallback(
                Futures.transform(mAccountTypesFuture, mAccountsExtractor),
                newAccountsUpdatedCallback(mAccountManagerAccounts),
                mMainThreadExecutor);
    }

    private synchronized void loadLocalAccounts() {
        mLocalAccountsFuture = mExecutor.submit(new Callable<List<AccountWithDataSet>>() {
            @Override
            public List<AccountWithDataSet> call() throws Exception {
                return mLocalAccountLocator.getDeviceLocalAccounts();
            }
        });
    }

    private synchronized void reloadLocalAccounts() {
        loadLocalAccounts();
        Futures.addCallback(mLocalAccountsFuture, newAccountsUpdatedCallback(mLocalAccounts),
                mMainThreadExecutor);
    }

    @Override
    public ListenableFuture<List<AccountInfo>> getAccountsAsync() {
        return getAllAccountsAsyncInternal();
    }

    private synchronized ListenableFuture<List<AccountInfo>> getAllAccountsAsyncInternal() {
        startLoadingIfNeeded();
        final AccountTypeProvider typeProvider = mTypeProvider;
        final ListenableFuture<List<List<AccountWithDataSet>>> all =
                Futures.nonCancellationPropagating(
                        Futures.successfulAsList(
                                Futures.transform(mAccountTypesFuture, mAccountsExtractor),
                                mLocalAccountsFuture));

        return Futures.transform(all, new Function<List<List<AccountWithDataSet>>,
                List<AccountInfo>>() {
            @Nullable
            @Override
            public List<AccountInfo> apply(@Nullable List<List<AccountWithDataSet>> input) {
                // input.get(0) contains accounts from AccountManager
                // input.get(1) contains device local accounts
                Preconditions.checkArgument(input.size() == 2,
                        "List should have exactly 2 elements");

                final List<AccountInfo> result = new ArrayList<>();
                for (AccountWithDataSet account : input.get(0)) {
                    result.add(
                            typeProvider.getTypeForAccount(account).wrapAccount(mContext, account));
                }

                for (AccountWithDataSet account : input.get(1)) {
                    //SPRD: add for bug693208, AndroidO porting for contacts to add Phone and SIM account
                    if (account != null && account.name == null && account.type == null) {
                        Log.d(TAG, "account is invaild");
                        continue;
                    }
                    result.add(
                            typeProvider.getTypeForAccount(account).wrapAccount(mContext, account));
                }
                AccountInfo.sortAccounts(null, result);
                return result;
            }
        });
    }

    @Override
    public ListenableFuture<List<AccountInfo>> filterAccountsAsync(
            final Predicate<AccountInfo> filter) {
        return Futures.transform(getAllAccountsAsyncInternal(), new Function<List<AccountInfo>,
                List<AccountInfo>>() {
            @Override
            public List<AccountInfo> apply(List<AccountInfo> input) {
                return new ArrayList<>(Collections2.filter(input, filter));
            }
        }, mExecutor);
    }

    @Override
    public AccountInfo getAccountInfoForAccount(AccountWithDataSet account) {
        if (account == null) {
            return null;
        }
        AccountType type = mTypeProvider.getTypeForAccount(account);
        if (type == null) {
            type = mFallbackAccountType;
        }
        return type.wrapAccount(mContext, account);
    }

    private List<AccountWithDataSet> getAccountsWithDataSets(Account[] accounts,
            AccountTypeProvider typeProvider) {
        List<AccountWithDataSet> result = new ArrayList<>();
        for (Account account : accounts) {
            final List<AccountType> types = typeProvider.getAccountTypes(account.type);
            for (AccountType type : types) {
                /**
                 * SPRD: SPRD: add for bug474772, display SIM capacity
                 *
                 * @{
                 */
                AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
                        account.name, account.type, type.dataSet);
                result.add(accountWithDataSet);

                if (type.accountType.matches("^sprd.*sim$")) {
                    mSimAccounts.add(accountWithDataSet);
                }
                if (type.accountType.matches("^sprd.*phone$")) {
                    Log.d(TAG, "accountType.accountType.matches phone");
                    mPhoneAccount = accountWithDataSet;
                }
                /**
                 * @}
                 */
            }
        }
        return result;
    }

    /**
     * Returns the default google account specified in preferences, the first google account
     * if it is not specified in preferences or is no longer on the device, and null otherwise.
     */
    @Override
    public Account getDefaultGoogleAccount() {
        final SharedPreferences sharedPreferences =
                mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
        final String defaultAccountKey =
                mContext.getResources().getString(R.string.contact_editor_default_account_key);
        return getDefaultGoogleAccount(mAccountManager, sharedPreferences, defaultAccountKey);
    }

    @Override
    public List<AccountInfo> getWritableGoogleAccounts() {
        final Account[] googleAccounts =
                mAccountManager.getAccountsByType(GoogleAccountType.ACCOUNT_TYPE);
        final List<AccountInfo> result = new ArrayList<>();
        for (Account account : googleAccounts) {
            final AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
                    account.name, account.type, null);
            final AccountType type = mTypeProvider.getTypeForAccount(accountWithDataSet);

            // AndroidGo porting
            if (type != null) {
                // Accounts with a dataSet (e.g. Google plus accounts) are not writable.
                result.add(type.wrapAccount(mContext, accountWithDataSet));
            }
        }
        return result;
    }

    /**
     * Returns true if there are real accounts (not "local" account) in the list of accounts.
     *
     * <p>This is overriden for performance since the default implementation blocks until all
     * accounts are loaded
     * </p>
     */
    @Override
    public boolean hasNonLocalAccount() {
        final Account[] accounts = mAccountManager.getAccounts();
        if (accounts == null) {
            return false;
        }
        for (Account account : accounts) {
            if (mTypeProvider.supportsContactsSyncing(account.type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the best {@link DataKind} matching the requested
     * {@link AccountType#accountType}, {@link AccountType#dataSet}, and {@link DataKind#mimeType}.
     * If no direct match found, we try searching {@link FallbackAccountType}.
     */
    @Override
    public DataKind getKindOrFallback(AccountType type, String mimeType) {
        DataKind kind = null;

        // Try finding account type and kind matching request
        if (type != null) {
            kind = type.getKindForMimetype(mimeType);
        }

        if (kind == null) {
            // Nothing found, so try fallback as last resort
            kind = mFallbackAccountType.getKindForMimetype(mimeType);
        }

        if (kind == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unknown type=" + type + ", mime=" + mimeType);
            }
        }

        return kind;
    }

    /**
     * Returns whether the account still exists on the device
     *
     * <p>This is overridden for performance. The default implementation loads all accounts then
     * searches through them for specified. This implementation will only load the types for the
     * specified AccountType (it may still require blocking on IO in some cases but it shouldn't
     * be as bad as blocking for all accounts).
     * </p>
     */
    @Override
    public boolean exists(AccountWithDataSet account) {
        final Account[] accounts = mAccountManager.getAccountsByType(account.type);
        for (Account existingAccount : accounts) {
            if (existingAccount.name.equals(account.name)) {
                return mTypeProvider.getTypeForAccount(account) != null;
            }
        }
        return false;
    }

    /**
     * Return {@link AccountType} for the given account type and data set.
     */
    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        final AccountType type = mTypeProvider.getType(
                accountTypeWithDataSet.accountType, accountTypeWithDataSet.dataSet);
        return type != null ? type : mFallbackAccountType;
    }
    /**
     * SPRD:Bug 693198 Support sdn numbers read in Contacts.
     * @{
     */
    @Override
    public boolean isPhoneAccount(AccountWithDataSet account) {
        if (account == null) {
            return false;
        }
        return mPhoneAccount.equals(account);
    }

    /**
     * SPRD: add for bug474772, display SIM capacity
     * @{
     */
    public AccountWithDataSet getPhoneAccount() {
        return mPhoneAccount;
    }

    public List<AccountWithDataSet> getSimAccounts() {
        return mSimAccounts;
    }

    /**
     * @}
     */

    /**
     * SPRD:Bug505150 Third-part icons show in contactsitemlist.
     * @{
     */
    @Override
    public Drawable getListSimIcon(String accountType, String accountName, boolean isSdn) {
        Log.d(TAG, "getSimIcon isSdn = " + isSdn);
        if (accountType == null || accountName == null) {
            Log.e(TAG, "getSimIcon,set default icon because null point!");
            return mContext.getResources().getDrawable(R.drawable.icon);
        }

        if (accountType.equalsIgnoreCase(ACCOUNT_SIM) || accountType.equalsIgnoreCase(ACCOUNT_USIM)) {
            /**
             * SPRD: Bug680321 for speedup contactslist flush when scroll
             * @{
             */
            int simNum;
            if(!ActiveDataManager.self().isExist("getListSimIcon/simNum")){
                TelephonyManager mTelphonyManagerInstance = (TelephonyManager)(mContext.getSystemService(Context.TELEPHONY_SERVICE));
                ActiveDataManager.self().setUrl("getListSimIcon/simNum",new Integer(mTelphonyManagerInstance.getPhoneCount()));
            }
            simNum = ((Integer)ActiveDataManager.self().getObj("getListSimIcon/simNum")).intValue();
            /*
             * @}
             */
            Drawable iconDrawable = null;
            Log.d(TAG, "AccountTypeManager simNum is " + simNum);
            if (simNum == 1) {
                if (isSdn) {
                    iconDrawable =  mContext.getResources().getDrawable(R.drawable.ic_sim_card_sdn);
                } else {
                    iconDrawable = mContext.getResources().getDrawable(R.drawable.ic_sim_card);
                }
                return iconDrawable;
            }

            int phoneId = Integer.parseInt(accountName.substring(3)) - 1;
            /**
             * SPRD: Bug612288 after close sim card, open dialer to check the contacts, occurrs error
             * SPRD: Bug618301 under splid-screen, open dialer to phone, it occured erorr
             * after close sim card
             * @{
             */
            try {
                if ((ActiveDataManager.self().isExist("getListSimIcon/SubscriptionManager/phoneId="+phoneId+"/SimIconTint"))||
                        (SubscriptionManager.from(mContext) != null && SubscriptionManager.from(mContext)
                        .getActiveSubscriptionInfoForSimSlotIndex(phoneId) != null)) {
                    int SimIconTint;
                    if(!ActiveDataManager.self().isExist("getListSimIcon/SubscriptionManager/phoneId="+phoneId+"/SimIconTint")){
                        ActiveDataManager.self().setUrl("getListSimIcon/SubscriptionManager/phoneId="+phoneId+"/SimIconTint",new Integer(SubscriptionManager.from(mContext)
                                .getActiveSubscriptionInfoForSimSlotIndex(phoneId)
                                .getIconTint()));
                    }
                    SimIconTint = ((Integer)ActiveDataManager.self().getObj("getListSimIcon/SubscriptionManager/phoneId="+phoneId+"/SimIconTint")).intValue();
                    Log.d(TAG, "AccountTypeManager SimIconTint is "
                            + SimIconTint);
                        /*
                         * @}
                         */
                    for (int i = 0; i < simNum; i++) {
                        if ((i < simIconRes.length)
                                && accountName.equalsIgnoreCase(NAME_SIM
                                + (i + 1))) {
                            if (isSdn) {
                                iconDrawable = mContext.getResources()
                                        .getDrawable(simSdnIconRes[i]);
                            } else {
                                iconDrawable = mContext.getResources()
                                        .getDrawable(simIconRes[i]);
                            }
                            iconDrawable.setTint(SimIconTint);
                            return iconDrawable;
                        }
                    }
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException when load sim icon : " + e.getMessage());
            }
            /**
             * @}
             */
        }
        Log.e(TAG, "getSimIcon,set default icon because accountType or accountName not match!");
        return mContext.getResources().getDrawable(R.drawable.icon);
    }

    @Override
    public Drawable getAccountIcon(com.android.contacts.model.account.AccountWithDataSet account, boolean isSdn) {

        if (account == null) {
            return null;
        }
        com.android.contacts.model.account.AccountType accountType = getAccountType(account.type, null);
        Drawable ret = accountType.getDisplayIcon(mContext);

        if (!isSimAccount(account)) {
            Log.d(TAG, "is not a sim account");
            return ret;
        }

        Bitmap origBmp = ((BitmapDrawable) ret).getBitmap();
        Bitmap newBmp = Bitmap.createBitmap(origBmp.getWidth(), origBmp.getHeight(),
                origBmp.getConfig());
        Canvas canvas = new Canvas(newBmp);
        Paint paint = new Paint();
        // paint.setColorFilter(new
        // PorterDuffColorFilter(0xff00ffff,PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(origBmp, 0f, 0f, paint);

        String slot = AccountManager.get(mContext).getUserData(
                new Account(account.name, account.type), "identifier");
        if (slot == null) {
            return ret;
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(20);
        paint.setColor(0xFF32b4e4);

        canvas.drawText(slot, origBmp.getWidth(), 20, paint);
        return new BitmapDrawable(newBmp);
    }

    @Override
    public boolean isSimAccount(com.android.contacts.model.account.AccountWithDataSet account) {
        //ensureAccountsLoaded();
        if (account == null) {
            return false;
        }
        /**
         * SPRD :482982 DUT not show which SIM the contact is
         * Original code :
         for (AccountWithDataSet a : mSimAccounts) {
         if (a.equals(account)) {
         return true;
         }
         }
         return false;
         *
         */
        if (account.type.matches("^sprd.*sim$")) {
            return true;
        } else {
            return false;
        }
        /**
         * @}
         */
    }

    /*
     * @}
     */
        //SPRD: add for bug617830, add fdn feature
    @Override
    public Drawable getListFdnIcon(int phoneId) {
        Log.d(TAG, "phoneId = " + phoneId);
        TelephonyManager mTelphonyManagerInstance = (TelephonyManager)(mContext.getSystemService(Context.TELEPHONY_SERVICE));
        int simNum = mTelphonyManagerInstance.getPhoneCount();
        Drawable iconDrawable = null;
        Log.d(TAG, "AccountTypeManager simNum is " + simNum);
        if (simNum == 1) {
            iconDrawable =  mContext.getResources().getDrawable(R.drawable.ic_sim_card_fdn);
            return iconDrawable;
        } else {
            try {
                if (SubscriptionManager.from(mContext) != null && SubscriptionManager.from(mContext)
                        .getActiveSubscriptionInfoForSimSlotIndex(phoneId) != null) {
                    int SimIconTint = SubscriptionManager.from(mContext)
                            .getActiveSubscriptionInfoForSimSlotIndex(phoneId)
                            .getIconTint();
                    Log.d(TAG, "AccountTypeManager SimIconTint is "
                            + SimIconTint);
                    iconDrawable = mContext.getResources()
                            .getDrawable(simFdnIconRes[phoneId]);
                    iconDrawable.setTint(SimIconTint);
                    return iconDrawable;
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException when load sim icon : " + e.getMessage());
            }
        }
        return null;
    }

}
