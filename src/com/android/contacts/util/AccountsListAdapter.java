/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.PhoneAccountType;
import com.android.contacts.model.account.SimAccountType;
import com.android.contacts.model.account.USimAccountType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private List<AccountInfo> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;
    private int mCustomLayout = -1;
    /*
     * SPRD:Bug653870 add cache for displayName & icon
     */
    private List<String> mDisplayNameCache = new ArrayList<String>();
    private List<Drawable> mIconCache = new ArrayList<Drawable>();

    public AccountsListAdapter(Context context) {
        this(context, Collections.<AccountInfo>emptyList(), null);
    }

    public AccountsListAdapter(Context context, List<AccountInfo> accounts) {
        this(context, accounts, null);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
    public AccountsListAdapter(Context context, List<AccountInfo> accounts,
            AccountWithDataSet currentAccount) {
        mInflater = LayoutInflater.from(context);

        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = new ArrayList<>(accounts.size());
        setAccounts(accounts, currentAccount);
    }

    public void setAccounts(List<AccountInfo> accounts, AccountWithDataSet currentAccount) {
        // If it's not empty use the previous "current" account (the first one in the list)
        final AccountInfo currentInfo = mAccounts.isEmpty()
                ? AccountInfo.getAccount(accounts, currentAccount)
                : AccountInfo.getAccount(accounts, mAccounts.get(0).getAccount());

        mAccounts.clear();
        mAccounts.addAll(accounts);

        if (currentInfo != null
                && !mAccounts.isEmpty()
                && !mAccounts.get(0).sameAccount(currentAccount)
                && mAccounts.remove(currentInfo)) {
            mAccounts.add(0, currentInfo);
        }

        /**
         * SPRD:Bug693214 Copy contacts in QuickContactActivity.
         *
         * @{
         **/
        if (currentAccount != null) {
            mAccounts.remove(AccountInfo.getAccount(accounts, currentAccount));
        }
        /**
         * @}
         */
        // SPRD: Bug719846 can not update accounts after modify primary sim cards
        updateCache();
        notifyDataSetChanged();
    }

    /**
     * SPRD: Bug719846 can not update accounts after modify primary sim cards
     * @{
     */
    private void updateCache() {
        int accontsSize = mAccounts.size();
        if (mDisplayNameCache.size() != accontsSize || mIconCache.size() != accontsSize) {
            mDisplayNameCache.clear();
            mIconCache.clear();
            AccountWithDataSet account;
            AccountType accountType;
            for (int i = 0; i < accontsSize ; i++) {
                mIconCache.add(mAccounts.get(i).getIcon());
                account = mAccounts.get(i).getAccount();
                accountType = mAccountTypes.getAccountType(account.type, account.dataSet);
                if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
                    mDisplayNameCache.add(mContext.getString(R.string.label_phone));
                } else if (SimAccountType.ACCOUNT_TYPE.equals(account.type)
                        || USimAccountType.ACCOUNT_TYPE.equals(account.type)) {
                    mDisplayNameCache.add(accountType.getDisplayName(mContext, account));
                } else {
                    mDisplayNameCache.add(account.name);
                }
            }
        }
    }
    /**
     * @}
     */

    public void setCustomLayout(int customLayout) {
        mCustomLayout = customLayout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View resultView = convertView != null ? convertView :
                mInflater.inflate(mCustomLayout > 0 ? mCustomLayout :
                        R.layout.account_selector_list_item_condensed, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

        //SPRD: add for bug708442, distinct SIM accounts in account selection view
        //text2.setText(mAccounts.get(position).getNameLabel());
        final AccountWithDataSet account = mAccounts.get(position).getAccount();
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        if (account.type != null && account.type.equals("sprd.com.android.account.phone")) {
            text1.setText(mContext.getString(R.string.label_phone));
        } else if (account.type != null && (account.type.equals("sprd.com.android.account.sim")
                || account.type.equals("sprd.com.android.account.usim"))) {
            text1.setText(mContext.getString(R.string.label_sim));
        } else {
            text1.setText(mAccounts.get(position).getTypeLabel());
        }
        text2.setText(mDisplayNameCache.get(position));

        /**
         * SPRD:AndroidN porting add sim icon feature.
         * Original code:
        icon.setImageDrawable(mAccounts.get(position).getIcon());
         * @{
         */
        icon.setImageDrawable(mIconCache.get(position));
        /**
         * @}
         */

        return resultView;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position).getAccount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     *UNISOC: Add for Bug1399391
     * @{ */
    public List<AccountInfo>  getAccounts() {
        return mAccounts;
    }
    /**
     * @}
     */
}

