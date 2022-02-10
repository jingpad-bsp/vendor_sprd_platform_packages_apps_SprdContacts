package com.sprd.contacts.list;

import android.app.Activity;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.contacts.R;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.list.ContactListAdapter.ContactQuery;
import com.android.contacts.list.GroupMemberPickerFragment.Listener;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.GroupMemberPickerFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.DefaultContactListAdapter;
import com.sprd.contacts.util.MultiContactDataCacheUtils.CacheMode;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ImageView;
import android.view.Gravity;
import android.widget.Button;
import android.view.View;
import com.sprd.contacts.util.MultiContactDataCacheUtils.CacheMode;
import com.android.contacts.activities.ActionBarAdapter;

public class MultiContactsPickerFragment extends
        MultiSelectContactsListFragment<DefaultContactListAdapter> {

    public static final String TAG = "MultiContactsPickerFragment";

    private static final String KEY_PERMANENT_FILTER = "permanentAccountFilter";
    private static final String KEY_EXCLUDE_READONLY = "excludereadonly";
    private static final String KEY_LIST_REQUEST_MODE_SELECTION = "listRequestModeSelection";
    private static final String KEY_FILTER = "filter";
    private boolean mPermanentAccountFilter = false;
    private boolean mExcludeReadOnly = false;
    private String mListRequestModeSelection;
    private ContactListFilter mFilter;
    private View mEmptyAccountView;
    // SPRD: add for bug 474264,693201 contacts count display feature
    private View mAccountFilterContainer;
    private String mMode = null;
    /** Callbacks for host of {@link MultiContactsPickerFragment}. */
    public interface Listener {

            /** Invoked when a contact is selected. */
            void onContactClicked(Cursor cursor);

            /** Invoked when user has initiated multiple selection mode. */
            void onSelectContacts();

            /**Invoked when user click the multiple finish button. */
            void onPickAllInOneDataAction(ArrayList<String> data);
        }

    public MultiContactsPickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setHasOptionsMenu(true);
        setDisplayDirectoryHeader(false);
        setContactCacheModel(CacheMode.SINGLE, Contacts.LOOKUP_KEY, Contacts.LOOKUP_KEY, Contacts.LOOKUP_KEY);
    }
    //SPRD: and for Bug1004407 Sending MMS and adding vcard contacts will not intercept the first 3,500 contacts.
    private int mLimitCount = 3500;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mPermanentAccountFilter = savedState.getBoolean(KEY_PERMANENT_FILTER);
            mExcludeReadOnly = savedState.getBoolean(KEY_EXCLUDE_READONLY);
            mFilter = savedState.getParcelable(KEY_FILTER);
            mListRequestModeSelection = savedState.getString(KEY_LIST_REQUEST_MODE_SELECTION);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PERMANENT_FILTER, mPermanentAccountFilter);
        outState.putBoolean(KEY_EXCLUDE_READONLY, mExcludeReadOnly);
        outState.putParcelable(KEY_FILTER, mFilter);
        outState.putString(KEY_LIST_REQUEST_MODE_SELECTION, mListRequestModeSelection);
    }

    private Listener mListener;
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.contact_list_content, null);
        final FrameLayout contactListLayout = (FrameLayout) view.findViewById(R.id.contact_list);
        mEmptyAccountView = getEmptyAccountView(inflater);
        // SPRD: add for bug 474264,693201 contacts count display feature
        mAccountFilterContainer = view.findViewById(R.id.account_filter_header_container);
        contactListLayout.addView(mEmptyAccountView);
        return view;
    }

    private View getEmptyAccountView(LayoutInflater inflater) {
        final View emptyAccountView = inflater.inflate(R.layout.empty_account_view, null);
        final ImageView image = (ImageView) emptyAccountView.findViewById(R.id.empty_account_image);
        image.setVisibility(View.GONE);
        final TextView textView = emptyAccountView.findViewById(R.id.empty_account_view_text);
        textView.setText(R.string.listFoundAllContactsZero);
        final Button addContactButton =
                (Button) emptyAccountView.findViewById(R.id.add_contact_button);
        addContactButton.setVisibility(View.GONE);
        return emptyAccountView;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            setVisibleScrollbarEnabled(true);
            if (data.getCount() < 1) {
                mEmptyAccountView.setVisibility(View.VISIBLE);
            } else {
                mEmptyAccountView.setVisibility(View.GONE);
            }
            // SPRD: add for bug 474264,693201 contacts count display feature
            bindListHeaderForAllAccounts(getContext(), getListView(), mAccountFilterContainer,
                    data.getCount());
            invalidateOptionsMenu();
            /**
             * SPRD: Bug709697 color of actionBar menu not update when rotate screen
             */
            final ContactSelectionActivity activity = getContactSelectionActivity();
            if (activity != null) {
                final ActionBarAdapter actionBarAdapter = activity.getActionBarAdapter();
                if (actionBarAdapter != null) {
                    actionBarAdapter.updateOverflowButtonColor();
                }
            }
            /**
             * @}
             */
            super.onLoadFinished(loader, data);
        }
    }

    @Override
    protected DefaultContactListAdapter createListAdapter() {
        final DefaultContactListAdapter adapter = new DefaultContactListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        final DefaultContactListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (!isSearchMode() && mFilter != null) {
            adapter.setFilter(mFilter);
        }
        adapter.setExcludeReadOnly(mExcludeReadOnly);
        adapter.setListRequestModeSelection(mListRequestModeSelection);
    }

    @Override
    protected void onItemClick(int position, long id) {
        if (getAdapter().isDisplayingCheckBoxes()) {
            super.onItemClick(position, id);
            return;
        }
        if (mListener != null) {
            final Cursor cursor = (Cursor)getAdapter().getItem(position);
            mListener.onContactClicked(cursor);
        }
    }

    public void setExcludeReadOnly(boolean excludeReadOnly){
        mExcludeReadOnly = excludeReadOnly;
    }

    public void setPermanentFilter(ContactListFilter filter) {
        mPermanentAccountFilter = true;
        setFilter(filter);
    }

    public void setFilter(ContactListFilter filter) {
        if (filter == null) {
            return;
        }
        if (mFilter == null || !mFilter.toString().equals(filter.toString())) {
            mFilter = filter;
        }
    }

    public void setListRequestModeSelection(String selection) {
        mListRequestModeSelection = selection;
    }

    public void setMode(String mode) {
        mMode = mode;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.multi_contacts_picker, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final ContactSelectionActivity activity = getContactSelectionActivity();
        final boolean isSearchMode = activity == null ? false : activity.isSearchMode();
        final boolean isSelectionMode = activity == null ? false : activity.isSelectionMode();
        /**SPRD: Bug742181 The prompt is error when search no result for share by sms.
         * @{
         */
        setVisible(menu, R.id.menu_search, (mEmptyAccountView != null && mEmptyAccountView.getVisibility() == View.GONE) && !isSearchMode && !isSelectionMode);
        /**
         * @}
         */
        setVisible(menu, R.id.menu_select, (mEmptyAccountView != null && mEmptyAccountView.getVisibility() == View.GONE) && !isSearchMode && !isSelectionMode);
        /**
         * SPRD:bug 736015 search mode, add multiSelection function @{
         **/
        //final boolean showSelectAllOption = (!isSearchMode) && isSelectionMode && (!getAdapter().isAllContactSelected());
        final boolean showSelectAllOption = isSelectionMode && (!getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_select_all, showSelectAllOption);
        //final boolean showCancelAllOption = (!isSearchMode) && isSelectionMode && (getAdapter().isAllContactSelected());
        final boolean showCancelAllOption = isSelectionMode && (getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_cancel_all, showCancelAllOption);
        /**
         * @}
         **/

    }

    private void invalidateOptionsMenu() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    private ContactSelectionActivity getContactSelectionActivity() {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof ContactSelectionActivity) {
            return (ContactSelectionActivity) activity;
        }
        return null;
    }

    private static void setVisible(Menu menu, int id, boolean visible) {
        final MenuItem menuItem = menu.findItem(id);
        if (menuItem != null) {
            menuItem.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
            return true;
        } else if (id == R.id.menu_select) {
            if (mListener != null) {
                mListener.onSelectContacts();
            }
            return true;
        } else if (id == R.id.menu_contacts_select_all) {
            if (!TextUtils.isEmpty(mMode)) {
                SelectAllContacts();
            } else {
                /* SPRD: Bug1004407 Sending MMS and adding vcard contacts will not intercept the first 3,500 contacts. @{ */
                SelectAllContacts(mLimitCount);
                /* @} */
            }
            return true;
        } else if (id == R.id.menu_contacts_cancel_all) {
            CancelAllContacts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMultiPickerSelected() {
        HashMap<Long, String> temp = getContactCache().getCache();
        if (temp == null || temp.entrySet() == null) {
            return;
        }
        ArrayList<String> data = new ArrayList<String>();
        Iterator it = temp.entrySet().iterator();
        int i=0;
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (entry.getValue() != null) {
                data.add((String)entry.getValue());
            }
        }
        mListener.onPickAllInOneDataAction(data);
    }
}
