
package com.sprd.contacts.list;

import android.accounts.AccountManager;
import android.accounts.Account;
import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract.RawContacts;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.IIccPhoneBookEx;

import com.android.contacts.R;

public class ContactMemoryListView extends LinearLayout {

    private static final String TAG = ContactMemoryListView.class.getSimpleName();

    private ImageView mIcon;
    private TextView mAccountType;
    private TextView mAccountUserName;
    private TextView mSimUsage;
    private ContactListFilter mFilter;
    private boolean mSingleAccount;

    public ContactMemoryListView(Context context) {
        super(context);
    }

    public ContactMemoryListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContactListFilter(ContactListFilter filter) {
        mFilter = filter;
    }

    public void bindView(AccountTypeManager accountTypes) {
        if (mAccountType == null) {
            mIcon = (ImageView) findViewById(R.id.icon);
            mAccountType = (TextView) findViewById(R.id.accountType);
            mAccountUserName = (TextView) findViewById(R.id.accountUserName);
            mSimUsage = (TextView) findViewById(R.id.simUsage);
        }

        if (mFilter == null) {
            mAccountType.setText(R.string.contactsList);
            return;
        }
        if (mFilter.icon != null) {
            mIcon.setImageDrawable(mFilter.icon);
        } else {
            mIcon.setImageResource(R.drawable.unknown_source);
        }
        final AccountType accountType = accountTypes.getAccountType(mFilter.accountType,
                mFilter.dataSet);
        mAccountUserName.setText(accountType.getDisplayName(getContext(),
                new AccountWithDataSet(mFilter.accountName, mFilter.accountType, null)));
        mAccountType.setText(accountType.getDisplayLabel(getContext()));

        mAsyncTask = new AsyncTask<Void, Void, Void>() {

            private int mCapacity;
            private int mVacancies;
            @Override
            protected Void doInBackground(Void... params) {
                mCapacity = getAccountCapacity(getContext(), mFilter);
                mVacancies = getAccountUsage(getContext(), mFilter);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mSimUsage != null) {
                    mSimUsage.setText(mVacancies + "/" + mCapacity);
                }
                super.onPostExecute(result);
            }
        };
        mAsyncTask.execute();

    }

    private AsyncTask<Void, Void, Void> mAsyncTask;

    private static int getAccountCapacity(Context context, ContactListFilter filter) {
        if (filter.filterType != ContactListFilter.FILTER_TYPE_ACCOUNT) {
            return -1;
        }
        Account account = new Account(filter.accountName, filter.accountType);
        String tmp = AccountManager.get(context).getUserData(account, "capacity");
        if (tmp == null) {
            return -1;
        }

        //UNISOC: add for bug685538,1380687: SIM Capacity is 0 after STK refresh
        if (tmp.equals("0")) {
            int phoneId = 0;
            if (filter.accountName != null && filter.accountName.length() > 3) {
                phoneId = Integer.parseInt(filter.accountName.substring(3))-1;
            }
            Log.d(TAG, "getAccountCapacity phoneId = " + phoneId);
            SubscriptionManager subScriptionManager = new SubscriptionManager(context);
            SubscriptionInfo subscriptionInfo = subScriptionManager.getActiveSubscriptionInfoForSimSlotIndex(phoneId);
            int subId = 0;
            int tmpCapacity = -1;
            if (subscriptionInfo != null) {
                subId = subscriptionInfo.getSubscriptionId();
                tmpCapacity = getSimCardLength(subId);
            }
            Log.d(TAG, "getAccountCapacity subId = " + subId + ", tmpCapacity = " + tmpCapacity);
            if (tmpCapacity == -1) {
                return -1;
            } else {
                AccountManager.get(context).setUserData(account, "capacity", String.valueOf(tmpCapacity));
                return tmpCapacity;
            }
        }
        return Integer.parseInt(tmp);
    }

    private int getAccountUsage(Context context, ContactListFilter filter) {
        ContentResolver cr = context.getContentResolver();
        int providerCapacity = -1;
        Cursor cursor = cr.query(RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, filter.accountName)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, filter.accountType)
                .build(),
                /**
                 * SPRD:Bug 693198 Support sdn numbers read in Contacts.
                 * @{
                 */
                null, "deleted=0 and sync1 != 'sdn'", null, null
                );

        if (cursor != null) {
            providerCapacity = cursor.getCount();
            cursor.close();
        }
        return providerCapacity;
    }

    //SPRD: add for bug685538: SIM Capacity is 0 after STK refresh
    public static int getSimCardLength(int subId) {
        int ret = -1;
        try {
            IIccPhoneBookEx iccIpb = IIccPhoneBookEx.Stub.asInterface(
                    ServiceManager.getService("simphonebookEx"));
            if (iccIpb != null) {
                int[] sizes = iccIpb.getAdnRecordsSizeForSubscriber(subId, IccConstants.EF_ADN);
                if (sizes != null) {
                    if (sizes.length == 3) {
                        ret = sizes[2];
                    } else if (sizes.length == 2) {
                        ret = sizes[1] / sizes[0];
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
