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

package com.android.contacts.list;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * SPRD:Bug693204 Display contacts only with phone number.
 * @{
 */
import com.android.contacts.preference.ContactsPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.text.TextUtils.TruncateAt;
import com.android.contacts.activities.RequestPermissionsActivity;
import android.util.Log;
/**
 * @}
 */

/**
 * Shows a list of all available accounts, letting the user select under which account to view
 * contacts.
 */
public class AccountFilterActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = "AccountFilterActivity";
    private static final int SUBACTIVITY_CUSTOMIZE_FILTER = 0;

    public static final String EXTRA_CONTACT_LIST_FILTER = "contactListFilter";

    private ListView mListView;

    // The default contact list type, it should be either FILTER_TYPE_ALL_ACCOUNTS or
    // FILTER_TYPE_CUSTOM, since those are the only two options we give the user.
    private int mCurrentFilterType;

    private ContactListFilterView mCustomFilterView; // the "Customize" filter

    private boolean mIsCustomFilterViewSelected;

    /**
     * SPRD: Bug693204 Display contacts only with phone number.
     * @{
     */
    public static final String KEY_EXTRA_CURRENT_FILTER = "currentFilter";
    private static SharedPreferences mSharedPreferences;
    private CheckBox mOnlyPhones;
    private View mHeaderPhones;
    private Boolean isChecked;
    private ContactListFilter mCurrentFilter;
    /**
     * @}
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.contact_list_filter);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        /**
         * SPRD: Bug693204 Display contacts only with phone number.
         * @{
         */
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        createWithPhonesOnlyView();
        mCurrentFilter = getIntent().getParcelableExtra(KEY_EXTRA_CURRENT_FILTER);
        /**
         * @}
         */

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mCurrentFilterType = ContactListFilterController.getInstance(this).isCustomFilterPersisted()
                ? ContactListFilter.FILTER_TYPE_CUSTOM
                : ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS;

        // We don't need to use AccountFilterUtil.FilterLoader since we only want to show
        // the "All contacts" and "Customize" options.
        final List<ContactListFilter> filters = new ArrayList<>();
        filters.add(ContactListFilter.createFilterWithType(
                ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        filters.add(ContactListFilter.createFilterWithType(
                ContactListFilter.FILTER_TYPE_CUSTOM));
        mListView.setAdapter(new FilterListAdapter(this, filters, mCurrentFilterType));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ContactListFilterView listFilterView = (ContactListFilterView) view;
        final ContactListFilter filter = (ContactListFilter) view.getTag();
        if (filter == null) return; // Just in case

        //SPRD: Bug693204 Display contacts only with phone number.
        filter.onlyPhonesChanged = mCurrentFilter.onlyPhonesChanged;

        if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
            mCustomFilterView = listFilterView;
            mIsCustomFilterViewSelected = listFilterView.isChecked();
            final Intent intent = new Intent(this, CustomContactListFilterActivity.class)
                    .putExtra(CustomContactListFilterActivity.EXTRA_CURRENT_LIST_FILTER_TYPE,
                            mCurrentFilterType);
            listFilterView.setActivated(true);
            // Switching activity has the highest priority. So when we open another activity, the
            // announcement that indicates an account is checked will be interrupted. This is the
            // way to overcome -- View.announceForAccessibility(CharSequence text);
            listFilterView.announceForAccessibility(listFilterView.generateContentDescription());
            startActivityForResult(intent, SUBACTIVITY_CUSTOMIZE_FILTER);
        } else {
            listFilterView.setActivated(true);
            listFilterView.announceForAccessibility(listFilterView.generateContentDescription());
            final Intent intent = new Intent();
            intent.putExtra(EXTRA_CONTACT_LIST_FILTER, filter);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED && mCustomFilterView != null &&
                !mIsCustomFilterViewSelected) {
            mCustomFilterView.setActivated(false);
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case SUBACTIVITY_CUSTOMIZE_FILTER: {
                final Intent intent = new Intent();
                ContactListFilter filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_CUSTOM);
                intent.putExtra(EXTRA_CONTACT_LIST_FILTER, filter);
                setResult(Activity.RESULT_OK, intent);
                finish();
                break;
            }
        }
    }

    private static class FilterListAdapter extends BaseAdapter {
        private final List<ContactListFilter> mFilters;
        private final LayoutInflater mLayoutInflater;
        private final AccountTypeManager mAccountTypes;
        private final int mCurrentFilter;

        public FilterListAdapter(
                Context context, List<ContactListFilter> filters, int current) {
            mLayoutInflater = (LayoutInflater) context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            mFilters = filters;
            mCurrentFilter = current;
            mAccountTypes = AccountTypeManager.getInstance(context);
        }

        @Override
        public int getCount() {
            return mFilters.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ContactListFilter getItem(int position) {
            return mFilters.get(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ContactListFilterView view;
            if (convertView != null) {
                view = (ContactListFilterView) convertView;
            } else {
                view = (ContactListFilterView) mLayoutInflater.inflate(
                        R.layout.contact_list_filter_item, parent, false);
            }
            view.setSingleAccount(mFilters.size() == 1);
            final ContactListFilter filter = mFilters.get(position);
            view.setContactListFilter(filter);
            view.bindView(mAccountTypes);
            view.setTag(filter);
            view.setActivated(filter.filterType == mCurrentFilter);
            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // We have two logical "up" Activities: People and Phone.
                // Instead of having one static "up" direction, behave like back as an
                // exceptional case.
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * SPRD: Bug693204 Display contacts only with phone number.
     * @{
     */
    private void createWithPhonesOnlyView() {
        // Add the "Only contacts with phones" header modifier.
        mHeaderPhones = findViewById(R.id.only_phone);
        mOnlyPhones = (CheckBox) mHeaderPhones.findViewById(R.id.checkbox);
        isChecked = mSharedPreferences.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);

        mOnlyPhones.setChecked(isChecked);
        {
            final TextView title = (TextView) mHeaderPhones.findViewById(R.id.title);
            final TextView describe = (TextView) mHeaderPhones.findViewById(R.id.describe);
            title.setSingleLine(true);
            title.setEllipsize(TruncateAt.END);
            title.setText(R.string.list_contact_phone);
            describe.setText(R.string.list_filter_phones);
        }

        mHeaderPhones.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mOnlyPhones.toggle();
                Editor editor = mSharedPreferences.edit();
                editor.putBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                        mOnlyPhones.isChecked());
                editor.apply();

                if (isChecked != mOnlyPhones.isChecked()) {
                    mCurrentFilter.onlyPhonesChanged = 1;
                } else {
                    mCurrentFilter.onlyPhonesChanged = 0;
                }
            }
        });
    }
    /**
     * @}
     */
    /**
     * SPRD: Bug693204 Display contacts only with phone number.
     * @{
     */
     @Override
     public void onBackPressed() {

         if (isChecked != mSharedPreferences.getBoolean(
                 ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                 ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT)) {
             Log.d(TAG, "onBackPressed mOnlyPhones's status is changed, onlyPhonesChanged is " + mCurrentFilter.onlyPhonesChanged);

             final Intent intent = new Intent();
             intent.putExtra(EXTRA_CONTACT_LIST_FILTER, mCurrentFilter);
             setResult(Activity.RESULT_OK, intent);
             finish();
         }

         super.onBackPressed();
     }
     /**
     * @}
     */
}
