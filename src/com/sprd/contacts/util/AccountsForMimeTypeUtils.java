
package com.sprd.contacts.util;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.android.collect.Lists;

public class AccountsForMimeTypeUtils {
    private static final String TAG = AccountsForMimeTypeUtils.class.getSimpleName();

    public static ArrayList<AccountWithDataSet> getAccountsForMimeType(Context context,
            Bundle extras) {
        AccountTypeManager accountManager = AccountTypeManager.getInstance(context);
        ArrayList<AccountWithDataSet> ret = new ArrayList<AccountWithDataSet>();
        ArrayList<AccountWithDataSet> accounts = new ArrayList<AccountWithDataSet>();
        for (AccountWithDataSet accountWithDataSet : accountManager.getAccounts(true)) {
            accounts.add(accountWithDataSet);
        }
        Log.i(TAG, "getAccountsForMimeType:" + (extras == null ? " null" : extras.toString())
                + " accounts = " + accounts);
        if (extras == null || extras.size() == 0) {
            return accounts;
        }
        for (AccountWithDataSet account : accounts) {
            final ContentValues values = new ContentValues();
            RawContactDelta insert = new RawContactDelta(ValuesDelta.fromAfter(values));
            RawContactModifier.parseExtras(context,
                    accountManager.getAccountTypeForAccount(account), insert, extras);
            Set<String> supportedMimeTypes = insert.getMimeTypes();
            Log.i(TAG, "supportedMimeTypes:" + supportedMimeTypes);
            supportedMimeTypes.remove(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            if (!supportedMimeTypes.isEmpty()) {
                ret.add(account);
            }
        }
        Log.i(TAG, "getAccountsForMimeType: the result accounts obtained after treatment is "
                + ret);
        return ret;
    }
}
