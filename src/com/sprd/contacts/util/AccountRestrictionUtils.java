/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.sprd.contacts.util;

import android.text.TextUtils;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.PhoneAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.NameConverter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.android.internal.telephony.GsmAlphabetEx;
import com.android.internal.telephony.EncodeException;
import com.sprd.providers.contacts.ContactProxyManager;
import com.sprd.providers.contacts.IContactProxy;
import com.sprd.contacts.NameSplitter;
import com.sprd.contacts.LocaleSet;

import android.provider.ContactsContract.Data;
import android.content.ContentValues;
import android.database.Cursor;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import java.util.ArrayList;
import java.util.Map;

import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.accounts.Account;
import android.content.ContentResolver;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.FullNameStyle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.Telephony.Mms;

import java.util.HashMap;

public class AccountRestrictionUtils {
    static final String TAG = "AccountRestrictionUtils";
    private static AccountRestrictionUtils sInstance;
    private AccountManager mAm;
    private ContactProxyManager mContactProxyManager;
    private Context mContext;
    final static String SIM_ACCOUNT_TYPE = "sprd.com.android.account.sim";
    final static String USIM_ACCOUNT_TYPE = "sprd.com.android.account.usim";

    public static AccountRestrictionUtils get(Context context) {
        if (sInstance == null) {
            sInstance = new AccountRestrictionUtils(context);
        }
        return sInstance;

    }

    private AccountRestrictionUtils(Context context) {
        mContext = context;
        mContactProxyManager = ContactProxyManager.getInstance(context);
        mAm = AccountManager.get(mContext);
    }

    public int mimeToRes(String mimeType) {
        if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
            return R.string.res_name;
        }
        if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
            return R.string.res_phone;
        }
        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
            return R.string.res_email;
        }
        return R.string.res_field;
    }

/**
 * get typeOverAllMax from account
 *
 * @param account
 * @param mimeType
 *
 * @return
 * 1. >0, means we should override the default value specified
 *    in xml or AccountType.
 * 2. =0, means we should not override the default value (since a valid
 *    values should not be 0?)
 * 3. =-1, means we should reset the value to infinity
 */
    public int getTypeOverallMax(AccountWithDataSet account, String mimeType) {
        String tmp = getUserData(new Account(account.name, account.type), mimeType
                + "_typeoverallmax");
        int ret = 0;
        if (tmp != null) {
            ret = Integer.parseInt(tmp);
        }
        return ret;
    }

    public int getCapacity(AccountWithDataSet account, String mimeType){
        String tmp = getUserData(new Account(account.name, account.type), mimeType
                + "_capacity");
        int ret = 0;
        if (tmp != null) {
            ret = Integer.parseInt(tmp);
        }
        return ret;
    }

    /**
     * UNISOC: add for bug1012869, add for orange_ef anr/aas/sne feature
     * @{
     */
    public int getSneSize(AccountWithDataSet account) {
        int ret = 0;
        String tmp = getUserData(new Account(account.name, account.type),Nickname.CONTENT_ITEM_TYPE + "_size");
        if (tmp != null) {
            ret = Integer.parseInt(tmp);
        }
        return ret;
    }
    /*
     * @}
     */


    public boolean violateCapacityRestriction(AccountWithDataSet account) {
        return violateCapacityRestriction(account, 0);
    }

    public boolean violateCapacityRestriction(AccountWithDataSet account, int padding) {
        if (account == null) {
            return false;
        }
        // check if SIM is full
        int currentLength = 0;
        int maxLength = -1;
        String simCardLengthStr = AccountManager.get(mContext).getUserData(
                new Account(account.name, account.type), "capacity");
        if (simCardLengthStr != null) {
            maxLength = Integer.parseInt(simCardLengthStr);
        } else {
            return false;
        }
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                .build(),
                null, "deleted=0", null, null
                );

        if (cursor != null) {
            currentLength = cursor.getCount();
            cursor.close();
        }

        if (maxLength != -1 && maxLength <= currentLength + padding) {
            return true;
        }
        return false;
    }

    final static Pattern sPhoneNumPattern = Pattern.compile("[^0-9\\+,;N\\*#]");

    public String violateFieldLengthRestriction(RawContactDeltaList set) {
        String ret = null;
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        AccountManager am = AccountManager.get(mContext);
        for (RawContactDelta state : set) {
            ValuesDelta values = state.getValues();
            final String account_name = values.getAsString(RawContacts.ACCOUNT_NAME);
            final String account_type = values.getAsString(RawContacts.ACCOUNT_TYPE);
            if (TextUtils.isEmpty(account_name) || TextUtils.isEmpty(account_type)) {
                continue;
            }
            Account account = new Account(account_name, account_type);

            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType accountType = accountTypes.getAccountType(account_type, dataSet);

            for (DataKind kind : accountType.getSortedDataKinds()) {
                String mimeType = kind.mimeType;
                /**
                 * UNISOC: bug1151983, Upper limit of Chinese name length for sim contacts is less than actual length
                 * @{
                 */
                if ((GroupMembership.CONTENT_ITEM_TYPE).equals(mimeType)) {
                    continue;
                }

                final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                if (entries == null)
                    continue;

                String tmp = null;
                if (USIM_ACCOUNT_TYPE.equals(account.type) || SIM_ACCOUNT_TYPE.equals(account.type)){
                    tmp = am.getUserData(account, mimeType + "_length");
                }
                int max = -1;
                if (tmp != null) {
                    max = Integer.parseInt(tmp);
                }
                if (max != -1) {
                    for (ValuesDelta entry : entries) {
                        String value = null;
                        if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                            /**
                             * UNISOC: bug708604  modify contact's name in sim account fail
                             * @{
                             */
                            String givenName = entry.getAsString(StructuredName.GIVEN_NAME);
                            String familyName = entry.getAsString(StructuredName.FAMILY_NAME);

                            if (givenName != null || familyName != null) {
                                Map<String, String> structuredNameMap = NameConverter
                                        .displayNameToStructuredName(
                                                mContext, givenName, familyName);
                                for (String field : structuredNameMap.keySet()) {
                                    entry.put(field, structuredNameMap.get(field));
                                }
                            }
                        }

                        ContentValues cv = entry.getAfter();
                        // FIXME: assume the data is always in DATA1
                        if (cv == null) {
                            continue;
                        }

                        if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                            String data2 = cv.getAsString(StructuredName.GIVEN_NAME);
                            String data3 = cv.getAsString(StructuredName.FAMILY_NAME);

                            LocaleSet currentLocales = LocaleSet.newDefault();
                            NameSplitter nameSplitter = new NameSplitter(currentLocales.getPrimaryLocale());
                            NameSplitter.Name name = new NameSplitter.Name(data2, data3);
                            name.fullNameStyle = FullNameStyle.UNDEFINED;
                            nameSplitter.guessNameStyle(name);
                            name.fullNameStyle = nameSplitter.getAdjustedFullNameStyle(name.fullNameStyle);
                            value = nameSplitter.join(name);
                        } else {
                            value = cv.getAsString(Data.DATA1);
                        }

                        android.util.Log.d(TAG, "violateFieldLengthRestriction: value = " + value);
                        /**
                         * @}
                         */

                        if (value == null) {
                            continue;
                        }

                        if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            value = sPhoneNumPattern.matcher(value).replaceAll("");
                            if (value != null && value.length() > 0
                                    && value.charAt(0) == '+') {
                                max = max + 1;
                            }
                        }

                        if (value != null && getGsmAlphabetBytes(value).length > max) {
                            return mimeType;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public String violateFieldCapacityRestriction(RawContactDeltaList set) {
        String ret = null;
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(mContext);
        AccountManager am = AccountManager.get(mContext);
        ContentResolver cr = mContext.getContentResolver();
        for (RawContactDelta state : set) {
            ValuesDelta values = state.getValues();
            final String type = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String name = values.getAsString(RawContacts.ACCOUNT_NAME);
            if (name == null) {
                continue;
            }
            Account account = new Account(name, type);

            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType accountType = accountTypes.getAccountType(type, dataSet);

            for (DataKind kind : accountType.getSortedDataKinds()) {
                final String mimeType = kind.mimeType;
                final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
                if (entries == null)
                    continue;

                String tmp = am.getUserData(account, mimeType + "_capacity");
                int max = -1;
                if (tmp != null) {
                    max = Integer.parseInt(tmp);
                }

                if (max != -1) {
                    Cursor cursor = cr.query(Data.CONTENT_URI.buildUpon()
                            .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                            .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                            .build(),
                            null, Data.MIMETYPE + "='" + mimeType + "'", null, null
                            );
                    int current = cursor.getCount();
                    cursor.close();
                    int aboutToInsert = 0;
                    for (ValuesDelta entry : entries) {
                        ContentValues cv = entry.getAfter();
                        if (cv == null) {
                            continue;
                        }
                        if (entry.isInsert() && !TextUtils.isEmpty(cv.getAsString(Data.DATA1))) {
                            aboutToInsert++;
                        } else if (entry.isDelete()) {
                            aboutToInsert--;
                        }
                    }
                    if (current + aboutToInsert > max) {
                        return mimeType;
                    }
                }
            }
        }
        return ret;
    }

    public static byte[] getGsmAlphabetBytes(String record) {
        byte[] bytes = new byte[0];
        if (record == null) {
            record = "";
        }
        try {
            bytes = GsmAlphabetEx.stringToGsmAlphaSS(record);
        } catch (EncodeException e) {
            try {
                bytes = record.getBytes("utf-16be");
                 /* SPRD: Bug 979503 When utf-16be encoding is used, a byte flag bit is reserved. @{ */
                if (bytes != null) {
                    byte[] alphabytes = new byte[bytes.length + 1];
                    System.arraycopy(bytes, 0, alphabytes, 1, bytes.length);
                    bytes = alphabytes;
                }
                /* @} */
            } catch (Exception e1) {
                e1.printStackTrace();
           }
        }
        return bytes;
    }

    public static class AccountFullException extends Exception {
    }

    public boolean violateEmailFormatRestriction(RawContactDeltaList set) {
        if (set == null) {
            return true;
        }
        for (RawContactDelta state : set) {
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(Email.CONTENT_ITEM_TYPE);
            if (entries == null) {
                continue;
            }
            for (ValuesDelta entry : entries) {
                ContentValues cv = entry.getAfter();
                if (cv == null) {
                    continue;
                }
                String value = null;
                value = cv.getAsString(Email.DATA);
                if (value == null || TextUtils.isEmpty(value)) {
                    continue;
                }
                if (!Mms.isEmailAddress(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int violatePhoneNumberType(RawContactDeltaList set) {
        if (set == null) {
            return -1;
        }
        String valueType = null;
        String mimeType = null;
        String[] FixedNumberTpye = new String[2];
        String[] CustomNumberType = new String[2];
        String[] numberType = new String[2];
        int FixedNumberIndex = 0;
        int CustomNumberIndex = 0;
        int numberIndex = 0;
        int toastId = -1;
        boolean hasName = false;
        for (RawContactDelta state : set) {
            ArrayList<ContentValues> ret = state.getContentValues();
            if (ret == null) {
                continue;
            }
            for (ContentValues cv : ret) {
                mimeType = cv.getAsString(Data.MIMETYPE);
                /**
                 * SPRD:Bug660560 the toast is unreasonable when enter blank name and other phone type
                 * @{
                 */
                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)
                        && (( cv.getAsString(Data.DATA2) != null && !cv.getAsString(Data.DATA2).trim().equals(""))
                        || ( cv.getAsString(Data.DATA3) != null && !cv.getAsString(Data.DATA3).trim().equals("")))) {
                    hasName = true;
                }
                /**
                 * @}
                 */
                if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)
                        && cv.getAsString(Data.DATA1) != null
                        && !cv.getAsString(Data.DATA1).equals("")) {
                    valueType = cv.getAsString(Data.DATA2);
                    /* SPRD: 443449 orange_ef anr/aas/sne @{ */
                    if (valueType.equals(String
                            .valueOf(Phone.TYPE_FIXED_NUMBER))) {
                        FixedNumberTpye[FixedNumberIndex++] = valueType;
                    } else if (valueType.equals(String
                            .valueOf(Phone.TYPE_CUSTOM))) {
                        CustomNumberType[CustomNumberIndex++] = valueType;
                    } else {
                        numberType[numberIndex++] = valueType;
                    }
                }
            }
            /* SPRD: 443449 orange_ef anr/aas/sne @{ */
            if (!TextUtils.isEmpty(FixedNumberTpye[1])
                    || (!TextUtils.isEmpty(numberType[1]))
                    || (!TextUtils.isEmpty(CustomNumberType[1]))) {
                toastId = R.string.dup_phone_number_type;
                return toastId;
            }

            /**
             * SPRD:Bug 539814 If there's only a fixed number,the contacts can't
             * saved
             */
            /**
             * @}
             */
            if ((TextUtils.isEmpty(numberType[0])) && !hasName) {
                toastId = R.string.only_fixed_number_type;
            }
        }
        return toastId;
    }

    public ContentValues tryInsert(RawContactDelta delta) {
        ValuesDelta valuesDelta = delta.getValues();
        String accountName = valuesDelta.getAsString(RawContacts.ACCOUNT_NAME);
        String accountType = valuesDelta.getAsString(RawContacts.ACCOUNT_TYPE);
        if (accountName == null || accountType == null) {
            return null;
        }
        Account account = new Account(accountName, accountType);
        IContactProxy proxy = mContactProxyManager.getProxyForAccount(account);
        if (proxy == null) {
            return null;
        }
        for (ContentValues values : delta.getContentValues()) {
            String mimeType = values.getAsString(Data.MIMETYPE);
            if (mimeType == null) {
                continue;
            }
            proxy.onDataUpdate(-1, values, mimeType);
        }
        return proxy.insert(-1, account);
    }

    /**
     * SPRD:Bug 539814 If there is only a fixed number,the contacts can't be saved
     * @{
     */
    /**
     * @}
     */
    public boolean violatePhoneFormatRestriction(RawContactDeltaList set) {
        if(set == null){
            return true;
        }
        for (RawContactDelta state : set) {
            ValuesDelta values = state.getValues();
            final String type = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(Phone.CONTENT_ITEM_TYPE);
            if(entries == null){
                continue;
            }
            for (ValuesDelta entry : entries) {
                ContentValues cv=entry.getAfter();
                if (cv==null) {
                    continue;
                }
                String value=null;
                value=cv.getAsString(Phone.DATA);
                if(value == null || TextUtils.isEmpty(value)){
                    continue;
                }
                if (PhoneAccountType.ACCOUNT_TYPE.equals(type)) {
                    return true;
                } else if(!isPhoneNumberValid(value)){
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isPhoneNumberValid(String number) {
        String tempFirstNumber = null;
        String tempSecondNumber = null;
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        tempFirstNumber = number.replaceAll(" ", "");
        if (TextUtils.isEmpty(tempFirstNumber)) {
            return false;
        }
        tempSecondNumber = tempFirstNumber.replaceAll("\\+", "");
        if (TextUtils.isEmpty(tempSecondNumber)) {
            return false;
        }
        return true;
    }

    public boolean isValid(RawContactDeltaList set) {
        if (violateFieldLengthRestriction(set) != null) {
            return false;
        }
        if (violateFieldCapacityRestriction(set) != null) {
            return false;
        }
        return true;
    }

    /* UNISOC: modify for bug1138289, sim card userData get error  @{ */
    //private static final Map<Account, Map<String, String>> sUserDataCache = new HashMap<Account, Map<String, String>>();

    public String getUserData(Account account, String key) {
        String ret = null;
        if (USIM_ACCOUNT_TYPE.equals(account.type) || SIM_ACCOUNT_TYPE.equals(account.type)){
            ret = mAm.getUserData(account, key);
        }
        return ret;
    }
    /* @} */

}
