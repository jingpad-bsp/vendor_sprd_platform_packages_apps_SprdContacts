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
package com.android.contacts.test.mocks;

import android.accounts.Account;
import android.graphics.drawable.Drawable;

import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountTypeWithDataSet;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.BaseAccountType;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;
import android.net.Uri;
import java.util.ArrayList;
import android.content.Context;
import android.graphics.drawable.Drawable;
/**
 * A mock {@link AccountTypeManager} class.
 */
public class MockAccountTypeManager extends AccountTypeManager {

    public AccountType[] mTypes;
    public AccountWithDataSet[] mAccounts;

    public MockAccountTypeManager(AccountType[] types, AccountWithDataSet[] accounts) {
        this.mTypes = types;
        this.mAccounts = accounts;
    }

    @Override
    public AccountType getAccountType(AccountTypeWithDataSet accountTypeWithDataSet) {
        // Add fallback accountType to mimic the behavior of AccountTypeManagerImpl
        AccountType mFallbackAccountType = new BaseAccountType() {
            @Override
            public boolean areContactsWritable() {
                return false;
            }
        };
        mFallbackAccountType.accountType = "fallback";
        for (AccountType type : mTypes) {
            if (Objects.equal(accountTypeWithDataSet.accountType, type.accountType)
                    && Objects.equal(accountTypeWithDataSet.dataSet, type.dataSet)) {
                return type;
            }
        }
        return mFallbackAccountType;
    }

    @Override
    public List<AccountWithDataSet> blockForWritableAccounts() {
        return Arrays.asList(mAccounts);
    }

    @Override
    public ListenableFuture<List<AccountInfo>> getAccountsAsync() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ListenableFuture<List<AccountInfo>> filterAccountsAsync(Predicate<AccountInfo> filter) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public AccountInfo getAccountInfoForAccount(AccountWithDataSet account) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Account getDefaultGoogleAccount() {
        return null;
    }

    /**
     * SPRD: add for bug474749, add Phone and SIM account for Contacts
     * @{
     */
    @Override
    public AccountWithDataSet getPhoneAccount() {
        return null;
    }

    @Override
    public List<AccountWithDataSet> getSimAccounts() {
        return null;
    }
    /**
     * @}
     */


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
        return -1;
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
     * SPRD:Bug 693198 Support sdn numbers read in Contacts.
     * @{
     */
    @Override
    public boolean isPhoneAccount(AccountWithDataSet account) {
        return false;
    }

    /**
     * @}
     */
    //SPRD: add for bug617830, add fdn feature
    @Override
    public Drawable getListFdnIcon(int phoneId) {
        return null;
    }
}
