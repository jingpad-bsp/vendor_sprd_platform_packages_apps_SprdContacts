/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.contacts.model.account;

import java.util.ArrayList;

import android.content.Context;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;

import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.R;

import android.util.Log;
import com.android.internal.telephony.TelePhonebookUtils;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType.EditType;
import com.sprd.contacts.util.Features;
/**
 * Account type for USIM card contacts
 */
public class USimAccountType extends SimAccountType {

    private static final String TAG = "USimAccountType";
    public static final String ACCOUNT_TYPE = "sprd.com.android.account.usim";
    private Context mContext = null;
    private AccountTypeProvider typeProvider = null;
    public USimAccountType(Context context) {
        super(context);

        mContext = context;
        this.accountType = ACCOUNT_TYPE;

        try {
            /**
             * SPRD:bug 693286,490245,1012869 add for orange_ef anr/aas/sne feature
             * @{
             */
            if (Features.supportEFDisplayFeature()) {
                addDataKindNickname(context);
            }
            /**
             * @}
             */
            //if (isGroupMembershipEditable()) {
            addDataKindGroupMembership(context);
            //}
            //if (isEmaiFieldEditable()) {
            addDataKindEmail(context);
            //}
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    @Override
    public boolean isGroupMembershipEditable() {
        AccountManager am = AccountManager.get(mContext);
        Account[] accountMatch = am.getAccountsByType(this.accountType);
        /* Bug458472 Add accountMatch length judgement to avoid exception throwing. @{ */
        if (accountMatch.length > 0) {
        /* @} */
            String length = am.getUserData(accountMatch[0], GroupMembership.CONTENT_ITEM_TYPE
                    + "_capacity");
            /* SPRD: modify for bug441633 @{ */
            if (null != length) {
                int groupCapacity = Integer.parseInt(length);
                if (groupCapacity > 0) {
                    return true;
                }
            }
            /* @} */
        }

        return false;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    public boolean isEmaiFieldEditable() {
        AccountManager am = AccountManager.get(mContext);
        Account[] accountMatch = am.getAccountsByType(this.accountType);
        if (accountMatch.length > 0) {
            String length = am.getUserData(accountMatch[0], Email.CONTENT_ITEM_TYPE + "_capacity");
            if (null != length) {
                int emailCapacity = Integer.parseInt(length);
                if (emailCapacity > 0) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindPhone(context);

        kind.typeColumn = Phone.TYPE;
        kind.typeOverallMax = 2;
        kind.typeList = new ArrayList();

        kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(1));

        /**
         * SPRD:bug 693286,490245 add for orange_ef anr/aas/sne feature
         * @{
         */
        if (TelePhonebookUtils.isSupportOrange()) {
            kind.typeList.add(buildPhoneType(Phone.TYPE_CUSTOM).setSecondary(
                    true).setCustomColumn(Phone.LABEL));
        } else {
            kind.typeList.add(buildPhoneType(Phone.TYPE_FIXED_NUMBER));
        }
        /**
         * @}
         */

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    @Override
    protected DataKind addDataKindEmail(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindEmail(context);

        kind.typeOverallMax = 1;
        kind.typeList = new ArrayList();

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));

        return kind;
    }
}
