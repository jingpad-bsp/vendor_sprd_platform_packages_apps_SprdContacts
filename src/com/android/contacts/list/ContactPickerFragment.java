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
package com.android.contacts.list;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;

import com.android.contacts.R;
import com.android.contacts.ShortcutIntentBuilder;
import com.android.contacts.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.accessibility.AccessibilityEvent;
import android.text.TextUtils;

/**
 * Fragment for the contact list used for browsing contacts (as compared to
 * picking a contact with one of the PICK or SHORTCUT intents).
 */
public class ContactPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter>
        implements OnShortcutIntentCreatedListener {

    private static final String KEY_EDIT_MODE = "editMode";
    private static final String KEY_CREATE_CONTACT_ENABLED = "createContactEnabled";
    private static final String KEY_SHORTCUT_REQUESTED = "shortcutRequested";
    private static final String KEY_EXCLUDE_READONLY = "excludereadonly";
    private static final String KEY_FILTER_ATTACHPHOTO = "filterforattachphoto";

    private OnContactPickerActionListener mListener;
    private boolean mCreateContactEnabled;
    private boolean mEditMode;
    private boolean mShortcutRequested;
    private boolean mFilterForAttachPhoto = false;
    private ContactListFilter mFilter;

    public ContactPickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_CONTACT_SHORTCUT);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        mListener = listener;
    }

    public void setCreateContactEnabled(boolean flag) {
        this.mCreateContactEnabled = flag;
    }

    public void setEditMode(boolean flag) {
        mEditMode = flag;
    }

    public void setShortcutRequested(boolean flag) {
        mShortcutRequested = flag;
    }

    public void setFilterForAttachPhoto(boolean filterForAttachPhoto, ContactListFilter filter){
        mFilterForAttachPhoto = filterForAttachPhoto;
        mFilter = filter;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_EDIT_MODE, mEditMode);
        outState.putBoolean(KEY_CREATE_CONTACT_ENABLED, mCreateContactEnabled);
        outState.putBoolean(KEY_SHORTCUT_REQUESTED, mShortcutRequested);
        outState.putBoolean(KEY_EXCLUDE_READONLY, mExcludeReadOnly);
        outState.putBoolean(KEY_FILTER_ATTACHPHOTO, mFilterForAttachPhoto);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mEditMode = savedState.getBoolean(KEY_EDIT_MODE);
        mCreateContactEnabled = savedState.getBoolean(KEY_CREATE_CONTACT_ENABLED);
        mShortcutRequested = savedState.getBoolean(KEY_SHORTCUT_REQUESTED);
        mExcludeReadOnly = savedState.getBoolean(KEY_EXCLUDE_READONLY);
        mFilterForAttachPhoto = savedState.getBoolean(KEY_FILTER_ATTACHPHOTO);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /**
         * SPRD: Bug 743020 it can not create new contact from dialer plate
         * Original Android code:
           if (position == 0 && mCreateContactEnabled && mListener != null);
         * @{
         */
        if (position == 1 && mCreateContactEnabled && mListener != null) {
            mListener.onCreateNewContactAction();
        } else {
            super.onItemClick(parent, view, position, id);
        }
        /**
         * @}
         */
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri uri;
        if (isLegacyCompatibilityMode()) {
            uri = ((LegacyContactListAdapter)getAdapter()).getPersonUri(position);
        } else {
            uri = ((ContactListAdapter)getAdapter()).getContactUri(position);
        }
        if (uri == null) {
            return;
        }
        if (mEditMode) {
            editContact(uri);
        } else  if (mShortcutRequested) {
            ShortcutIntentBuilder builder = new ShortcutIntentBuilder(getActivity(), this);
            builder.createContactShortcutIntent(uri);
        } else {
            pickContact(uri);
        }
    }

    public void editContact(Uri contactUri) {
        if (mListener != null) {
            mListener.onEditContactAction(contactUri);
        }
    }

    public void pickContact(Uri uri) {
        if (mListener != null) {
            mListener.onPickContactAction(uri);
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            HeaderEntryContactListAdapter adapter
                    = new HeaderEntryContactListAdapter(getActivity());
            /**
             * SPRD: Bug708932 should not support sim contacts for shortcut
             * @{
             */
            if (mShortcutRequested) {
                AccountTypeManager am = AccountTypeManager.getInstance(getContext());
                List<AccountWithDataSet> allAccounts = am.getAccounts(false);
                ArrayList<AccountWithDataSet> accounts = new ArrayList<AccountWithDataSet>();
                accounts.addAll(allAccounts);
                Iterator<AccountWithDataSet> iter = accounts.iterator();
                while (iter.hasNext()) {
                    AccountWithDataSet accountWithDataSet = iter.next();
                    if ("sprd.com.android.account.sim".equals(accountWithDataSet.type)
                            || "sprd.com.android.account.usim".equals(accountWithDataSet.type)) {
                        iter.remove();
                    }
                }
                adapter.setFilter(ContactListFilter.createAccountsFilter(accounts));
            } else if (mFilterForAttachPhoto) {
                adapter.setFilter(mFilter);
            } else {
                adapter.setFilter(ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
            }
            /**
             * @}
             */
            adapter.setExcludeReadOnly(mExcludeReadOnly);
            adapter.setSectionHeaderDisplayEnabled(true);
            adapter.setDisplayPhotos(true);
            adapter.setQuickContactEnabled(false);
            adapter.setShowCreateContact(mCreateContactEnabled);
            return adapter;
        } else {
            LegacyContactListAdapter adapter = new LegacyContactListAdapter(getActivity());
            adapter.setSectionHeaderDisplayEnabled(false);
            adapter.setDisplayPhotos(false);
            return adapter;
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_picker_content, null);
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
        if (mListener != null) {
            mListener.onShortcutIntentCreated(shortcutIntent);
        }
    }

    @Override
    public void onPickerResult(Intent data) {
        if (mListener != null) {
            mListener.onPickContactAction(data.getData());
        }
    }

    /**
     * SPRD: add for bug 729646 add empty Prompt when no contacts match in search mode
     * SPRD: Bug747296 Give hints when set picture as contact photo with no match contact
     * @{
     */
    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        final FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        checkHeaderViewVisibility();

        mSearchProgress = getView().findViewById(R.id.search_progress);
        mSearchProgressText = (TextView) mSearchHeaderView.findViewById(R.id.totalContactsText);
    }

    private void showSearchProgress(boolean show) {
        if (mSearchProgress != null) {
            mSearchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void checkHeaderViewVisibility() {
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
    }

    private View mSearchHeaderView;
    private View mSearchProgress;
    private TextView mSearchProgressText;

    private ContactSelectionActivity getContactSelectionActivity() {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof ContactSelectionActivity) {
            return (ContactSelectionActivity) activity;
        }
        return null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final ContactSelectionActivity activity = getContactSelectionActivity();
        final boolean isSearchMode = activity == null ? false : activity.isSearchMode();
        setVisible(menu, R.id.menu_search, (mSearchHeaderView != null && mSearchHeaderView.getVisibility() == View.GONE) && !isSearchMode);
    }

    public void setVisible(Menu menu, int id, boolean visible) {
        final MenuItem menuItem = menu.findItem(id);
        if (menuItem != null) {
            menuItem.setVisible(visible);
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setHasOptionsMenu(true);
    }

    @Override
    protected void setListHeader() {
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        if (!adapter.areAllPartitionsEmpty()) {
            mSearchHeaderView.setVisibility(View.GONE);
            showSearchProgress(false);
        } else {
            mSearchHeaderView.setVisibility(View.VISIBLE);
            if (adapter.isLoading()) {
                mSearchProgressText.setText(R.string.search_results_searching);
                showSearchProgress(true);
            } else {
                mSearchProgressText.setText(R.string.listFoundAllContactsZero);
                mSearchProgressText.sendAccessibilityEvent(
                        AccessibilityEvent.TYPE_VIEW_SELECTED);
                showSearchProgress(false);
            }
        }
        if(getContactSelectionActivity()!= null){
            getContactSelectionActivity().invalidateOptionsMenu();
        }
    }
    /**
     * @}
     */
}
