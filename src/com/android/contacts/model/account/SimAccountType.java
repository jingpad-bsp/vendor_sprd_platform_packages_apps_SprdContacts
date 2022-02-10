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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.contacts.R;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.PermissionsUtil;
import com.android.internal.telephony.GsmAlphabetEx;
import com.android.internal.telephony.IccPBForEncodeException;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import android.text.TextUtils;
import android.util.Log;

/**
 * Account type for SIM card contacts
 */
public class SimAccountType extends BaseAccountType {

    public static final String ACCOUNT_TYPE = "sprd.com.android.account.sim";

    public SimAccountType(Context context) {
        this.titleRes = R.string.account_sim;
        this.iconRes = R.drawable.quantum_ic_sim_card_vd_theme_24;
        this.accountType = ACCOUNT_TYPE;

        try {
            addDataKindStructuredName(context);
            addDataKindName(context);
            final DataKind phoneKind = addDataKindPhone(context);
            //phoneKind.typeOverallMax = 1;
            // SIM card contacts don't necessarily support separate types (based on data exposed
            // in Samsung and LG Contacts Apps.
            //phoneKind.typeList = Collections.emptyList();

            mIsInitialized = true;
        } catch (DefinitionException e) {
            // Just fail fast. Because we're explicitly adding the fields in this class this
            // exception should only happen in case of a bug.
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }

    @Override
    public void initializeFieldsFromAuthenticator(AuthenticatorDescription authenticator) {
        // Do nothing. We want to use our local icon and title
    }

    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        final DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, Weight.NONE, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;


        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.GIVEN_NAME, R.string.name_given,
                FLAGS_PERSON_NAME));
        kind.fieldList.add(new EditField(StructuredName.FAMILY_NAME, R.string.name_family,
                FLAGS_PERSON_NAME));

        return kind;
    }

    @Override
    protected DataKind addDataKindName(Context context) throws DefinitionException {
        final DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_NAME,
                R.string.nameLabelsGroup, Weight.NONE, true));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        final boolean displayOrderPrimary =
                context.getResources().getBoolean(R.bool.config_editor_field_order_primary);

        kind.fieldList = Lists.newArrayList();
        if (!displayOrderPrimary) {
            kind.fieldList.add(new EditField(StructuredName.FAMILY_NAME, R.string.name_family,
                    FLAGS_PERSON_NAME));
            kind.fieldList.add(new EditField(StructuredName.GIVEN_NAME, R.string.name_given,
                    FLAGS_PERSON_NAME));
        } else {
            kind.fieldList.add(new EditField(StructuredName.GIVEN_NAME, R.string.name_given,
                    FLAGS_PERSON_NAME));
            kind.fieldList.add(new EditField(StructuredName.FAMILY_NAME, R.string.name_family,
                    FLAGS_PERSON_NAME));
        }

        return kind;
    }

    @Override
    public AccountInfo wrapAccount(Context context, AccountWithDataSet account) {
        // Use the "SIM" type label for the name as well because on OEM phones the "name" is
        // not always user-friendly
        //SPRD: add for bug708442, distinct SIM accounts in account selection view
        if (account == null) {
            return null;
        }
        return new AccountInfo(
                new AccountDisplayInfo(account, account.name, getDisplayLabel(context),
                        getDisplayIcon(context, account), true), this);
    }

    /**
     * SPRD: add for bug474772, display SIM capacity
     * @{
     */
    protected static int[] simIconRes = {
        R.drawable.ic_sim_card_multi_sim1_account, R.drawable.ic_sim_card_multi_sim2_account,
        R.drawable.ic_sim_card_multi_sim3_account, R.drawable.ic_sim_card_multi_sim4_account,
        R.drawable.ic_sim_card_multi_sim5_account
    };

    @Override
    public Drawable getDisplayIcon(Context context) {
        final Drawable icon = ResourcesCompat.getDrawable(context.getResources(), iconRes, null);
        icon.mutate().setColorFilter(ContextCompat.getColor(context,
                R.color.actionbar_icon_color_grey), PorterDuff.Mode.SRC_ATOP);
        return icon;
    }

    @Override
    public Drawable getDisplayIcon(Context context, AccountWithDataSet account) {
        boolean isSingleSim = TelephonyManager.from(context).getPhoneCount() == 1 ? true : false;
        // SPRD: check phone permission for bug588583
        if (account != null && !isSingleSim && PermissionsUtil.hasPermission(context, android.Manifest.permission.READ_PHONE_STATE)) {
            int phoneId = Integer.parseInt(account.name.substring(3)) - 1;
            //add for Sprd:Bug 611707 Crash after disable sim cards
            int SimIconTint = 0;
            Drawable iconDrawable = context.getResources().getDrawable(simIconRes[phoneId]);
            if (SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(phoneId) !=null ) {
                SimIconTint = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(phoneId).getIconTint();
            }
            iconDrawable.setTint(SimIconTint);
            return iconDrawable;
        } else {
            return context.getResources().getDrawable(R.drawable.icon_sim_account);
        }
    }

    @Override
    public String getDisplayName(Context context, AccountWithDataSet account) {
        boolean isSingleSim = TelephonyManager.from(context).getPhoneCount() == 1 ? true : false;
        String displayName;
        // check phone permission for bug602218
        if (account != null && PermissionsUtil.hasPermission(context, android.Manifest.permission.READ_PHONE_STATE)) {
            if (!isSingleSim) {
                int phoneId = Integer.parseInt(account.name.substring(3)) - 1;
                //add for Sprd:Bug 611707 Crash after disable sim cards
                if (SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(phoneId) !=null ) {
                     displayName = SubscriptionManager.from(context)
                            .getActiveSubscriptionInfoForSimSlotIndex(phoneId).getDisplayName().toString();
                } else {
                        displayName = account.name;
                }
            } else {
                   if (SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(0) !=null ) {
                       displayName = SubscriptionManager.from(context)
                                    .getActiveSubscriptionInfoForSimSlotIndex(0).getDisplayName().toString();
                   } else {
                          displayName = account.name;
                   }
            }
            if (!TextUtils.isEmpty(displayName)) {
                return displayName;
            } else {
                return account.name;
            }

        } else {
            return null;
        }
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindPhone(context);

        kind.typeColumn = Phone.TYPE;
        kind.typeOverallMax = 1;
        kind.typeList = new ArrayList();
        // kind.typeList.add(buildPhoneType(Phone.TYPE_HOME).setSpecificMax(1));
        kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(1));

        kind.fieldList = new ArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }
    /**
     * @}
     */

    /**
     * @return length of string by bytes, for Sim and Usim.
     */
    @Override
    public int getAccountTypeFieldsLength(Context context, Account account,
            String mimeType) {
        if (account == null || account.type == null || mimeType == null
                || context == null) {
            return -1;
        }
        AccountManager am = AccountManager.get(context);
        if (am == null) {
            return -1;
        }
        if (mimeType.equals(FIELDS_STRUCTUREDNAME)
                || mimeType.equals(FIELDS_PHONETICNAME)) {
            mimeType = StructuredName.CONTENT_ITEM_TYPE;
        }
        String tmpLen =am.getUserData(account, mimeType + "_length");
        if (tmpLen != null) {
            return Integer.parseInt(tmpLen);
        } else {
            return -1;
        }
    }

    @Override
    public int getTextFieldsEditorLength(String txtString, int maxLength) {
        if (txtString == null || maxLength <= 0) {
            return -1;
        }
        if (getGsmAlphabetBytes(txtString).length > maxLength) {
            int end = 1;
            int txtLen = txtString.length();
            String tmpStr = txtString.substring(0, end);
            if (txtLen <= 0 || tmpStr == null) {
                return -1;
            }
            while (getGsmAlphabetBytes(tmpStr).length <= maxLength) {
                end++;
                if (end > txtLen) {
                    break;
                }
                tmpStr = txtString.substring(0, end);
            }
            end--;
            return end;
        } else {
            return -1;
        }
    }

    private static byte[] getGsmAlphabetBytes(String txtString) {
        byte[] bytes = new byte[0];
        if (txtString == null) {
            txtString = "";
        }
        try {
            bytes = GsmAlphabetEx.stringToGsmAlphaSSForDialer(txtString);
        } catch (IccPBForEncodeException e) {
            try {
                bytes = txtString.getBytes("utf-16be");
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
}
