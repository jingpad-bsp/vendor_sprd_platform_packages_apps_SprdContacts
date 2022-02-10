package com.sprd.contacts.list;

import android.app.Activity;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
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
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.logging.ListEvent;
import com.sprd.contacts.list.AllInOneDataListAdapter;
import com.sprd.contacts.list.AllInOneDataListAdapter.DataQuery;

import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import com.sprd.contacts.util.MultiContactDataCacheUtils.CacheMode;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
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
import android.database.Cursor;
import com.android.contacts.activities.ActionBarAdapter;
import com.sprd.contacts.activities.ContactSelectionMultiTabActivity;
import android.widget.Toast;

public class AllInOneDataPickerFragment
        extends MultiSelectContactsListFragment<AllInOneDataListAdapter> {
    public static final String TAG = "AllInOneDataPickerFragment";

    private Listener mListener;
    private View mEmptyAccountView;
    private List<String> mCascadingData;
    private static final String KEY_CASCADING_DATA = "cascadingData";
    private static final String KEY_PERMANENT_FILTER = "permanentAccountFilter";
    private static final String KEY_FILTER = "filter";
    private boolean mPermanentAccountFilter = false;
    private boolean mExcludeReadOnly = false;
    private ContactListFilter mFilter;
    private TextView mEmptyView;
    // UNISOC: add for bug 474264,693201 contacts count display feature
    private View mAccountFilterContainer;

    /**
     * UNISOC: Bug873473 During conference meeting, when choose the sixth contact,show the tip.
     * @{
     */
    private static final String KEY_LIMIIT_COUNT = "checked_limit_count";
    private int mLimitCount = 3500;
    /**
     * @}
     */

    public interface Listener {

        /** Invoked when a contact is selected. */
        void onDataClicked(HashMap<String, String> data);

        /** Invoked when user has initiated multiple selection mode. */
        void onSelectDatas();

        /**Invoked when user click the multiple finish button. */
        void onPickAllInOneDataAction(HashMap<String, String> data);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public AllInOneDataPickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setDisplayDirectoryHeader(false);
        setSearchMode(false);
        setHasOptionsMenu(true);
        setContactCacheModel(CacheMode.MULTI, Data.DATA1, Phone.DISPLAY_NAME_PRIMARY, null);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState != null) {
            mCascadingData = savedState.getStringArrayList(KEY_CASCADING_DATA);
            mPermanentAccountFilter = savedState.getBoolean(KEY_PERMANENT_FILTER);
            mFilter = savedState.getParcelable(KEY_FILTER);
        }
    }

    /**
     * UNISOC: add for Bug873473/1114098，When the number of conference call members reaches the upper limit, the prompt shows error.
     * @{
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().getIntent() != null) {
            mLimitCount = getActivity().getIntent().getIntExtra(KEY_LIMIIT_COUNT, 3500);
        }
    }
    /**
     * @}
     */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_CASCADING_DATA, new ArrayList<String>(mCascadingData));
        outState.putBoolean(KEY_PERMANENT_FILTER, mPermanentAccountFilter);
        outState.putParcelable(KEY_FILTER, mFilter);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.multi_contacts_picker, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final ContactSelectionActivity activity = getContactSelectionActivity();
        final boolean isSearchMode = activity == null ? false : activity.isSearchMode();
        final boolean isSelectionMode = activity == null ? false : activity.isSelectionMode();
        /**UNISOC: Bug742181 The prompt is error when search no result for share by sms.
         * @{
         */
        setVisible(menu, R.id.menu_search, (mEmptyAccountView != null && mEmptyAccountView.getVisibility() == View.GONE) && !isSearchMode && !isSelectionMode);
        /**
         * @}
         */
        setVisible(menu, R.id.menu_select, (mEmptyAccountView != null && mEmptyAccountView.getVisibility() == View.GONE) && !isSearchMode && !isSelectionMode);
        /**
         * UNISOC: bug 736015 search mode, add multiSelection function @{
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
                mListener.onSelectDatas();
            }
            return true;
        } else if (id == R.id.menu_contacts_select_all) {
            /**
             * UNISOC: Bug938459/1015715 During conference meeting, when choose all contact,show the tip
             * @{
             */
            if (getActivity() != null && getActivity() instanceof ContactSelectionMultiTabActivity) {
                int mCheckedLimitCount = ((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount;
                if (mCheckedLimitCount <= 0) {
                    Toast.makeText(
                            getContext(),
                            getContext().getString(
                                    R.string.contacts_selection_for_mms_limit,
                                    mLimitCount), Toast.LENGTH_SHORT)
                            .show();
                    return true;
                } else {
                    SelectAllContacts(getSelectedContactIds().size()+mCheckedLimitCount,mLimitCount);
                }
            } else {
                SelectAllContacts(mLimitCount);
            }
            /**
             * @}
             */
            return true;
        } else if (id == R.id.menu_contacts_cancel_all) {
            CancelAllContacts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isEmptyAccountViewVisibility() {
        if (mEmptyAccountView != null && mEmptyAccountView.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onItemClick(int position, long id) {
        if (getAdapter().isDisplayingCheckBoxes()) {
            /**
             * UNISOC: Bug873473 During conference meeting, when choose the sixth contact,show the tip.
             * @{
             */
            HashMap<Long, HashMap<String, String>> hash = getContactCache().getMultiCache();
            final long contactId = getContactId(position);
            boolean isMultiTabCalling = getActivity().toString().contains(
                    "ContactSelectionMultiTabActivity");
            if(isMultiTabCalling) {
                if(!hash.containsKey(contactId)){
                    if (((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount <= 0) {
                        Toast.makeText(
                                getContext(),
                                getContext().getString(
                                        R.string.contacts_selection_for_mms_limit,
                                        mLimitCount), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    ((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount--;
                } else {
                    ((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount++;
                }
            } else {
                if(!hash.containsKey(contactId)){
                    int mCheckedCount = hash.size();
                    Log.d(TAG, "AllInOneDataPickerFragment onItemClick: mCheckedCount = " +mCheckedCount + ", mLimitCount = " + mLimitCount);
                    if (mCheckedCount >= mLimitCount) {
                        Toast.makeText(
                                getContext(),
                                getContext().getString(
                                        R.string.contacts_selection_for_mms_limit,
                                        mLimitCount), Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                }
            }
            /**
             * @}
             */
            super.onItemClick(position, id);
            return;
        }
        if (mListener != null) {
            final Cursor cursor = (Cursor)getAdapter().getItem(position);
            HashMap<String, String> datas = new HashMap<String, String>();
            if(cursor != null) {
                String data1 = cursor.getString(cursor.getColumnIndex(Data.DATA1));
                String name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY));
                datas.put(data1, name);
            }
            mListener.onDataClicked(datas);
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.contact_list_content, null);
        final FrameLayout contactListLayout = (FrameLayout) view.findViewById(R.id.contact_list);
        mEmptyAccountView = getEmptyAccountView(inflater);
        // UNISOC: add for bug 474264,693201 contacts count display feature
        mAccountFilterContainer = view.findViewById(R.id.account_filter_header_container);
        contactListLayout.addView(mEmptyAccountView);
        return view;
    }

    @Override
    public void onMultiPickerSelected() {
        HashMap<String, String> data = getSelectedDatas();
        mListener.onPickAllInOneDataAction(data);
    }

    public HashMap<String, String> getSelectedDatas() {
        HashMap<Long, HashMap<String, String>> hash = getContactCache().getMultiCache();
        HashMap<String, String> temp = new HashMap<String, String>();
        HashMap<String, String> ret = new HashMap<String, String>();
        Iterator it = hash.entrySet().iterator();
        while(it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (entry.getKey() != null) {
                temp = hash.get(entry.getKey());
                String data1 = temp.get(Data.DATA1);
                /** UNISOC: add for bug1113328, Ignore the space effect when judging the same contact number. @{ */
                data1 = data1.replaceAll(" ", "");
                /** @｝ */
                String name = temp.get(Phone.DISPLAY_NAME_PRIMARY);
                ret.put(data1,name);
            }
        }
        return ret;
    }

    /**
     * UNISOC: Bug729362 The style in the tab for ContactSelectionMultiTabActivity is different
     * @{
     */
    private View getEmptyAccountView(LayoutInflater inflater) {
        final View emptyAccountView = inflater.inflate(R.layout.empty_account_view, null);
        final ImageView image = (ImageView) emptyAccountView.findViewById(R.id.empty_account_image);
        image.setVisibility(View.GONE);
        mEmptyView = emptyAccountView.findViewById(R.id.empty_account_view_text);
        mEmptyView.setText(R.string.listTotalPhoneContactsZero);
        final Button addContactButton =
                (Button) emptyAccountView.findViewById(R.id.add_contact_button);
        addContactButton.setVisibility(View.GONE);
        return emptyAccountView;
    }

    public TextView getEmptyView() {
        return mEmptyView;
    }
    /**
     * @}
     */

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            setVisibleScrollbarEnabled(true);
            if (data.getCount() < 1) {
                mEmptyAccountView.setVisibility(View.VISIBLE);
                /**UNISOC: Bug742181 The prompt is error when search no result for share by sms.
                 * @{
                 */
                if (isSearchMode()) {
                    mEmptyView.setText(R.string.listFoundAllContactsZero);
                }
                /**
                 * @}
                 */
            } else {
                mEmptyAccountView.setVisibility(View.GONE);
            }
            // UNISOC: add for bug 474264,693201 contacts count display feature
            bindListHeaderForAllInOneData(getContext(), getListView(), mAccountFilterContainer,
                    data.getCount());
            invalidateOptionsMenu();
            /**
             * UNISOC: Bug709697 color of actionBar menu not update when rotate screen
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
        } else {
            //UNISOC: modify for bug1201358, close calllog permission and add block number,calllog tab display abnormall.
            mEmptyAccountView.setVisibility(View.VISIBLE);
            getView().setVisibility(View.VISIBLE);
        }
    }
    @Override
    protected AllInOneDataListAdapter createListAdapter() {
        final AllInOneDataListAdapter adapter = new AllInOneDataListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        final AllInOneDataListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        /**
         * UNISOC BUG 729973 must to set filter @{
         **/
        if (mFilter != null) {
            adapter.setFilter(mFilter);
        } else {
            adapter.setFilter(ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        }
        /**
         * @}
         **/
        adapter.setCascadingData(mCascadingData);
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

    public void setVisible(Menu menu, int id, boolean visible) {
        final MenuItem menuItem = menu.findItem(id);
        if (menuItem != null) {
            menuItem.setVisible(visible);
        }
    }
    public void setCascadingData(List<String> data) {
        mCascadingData = data;
    }

    /**
     * UNISOC: Bug1015715 During conference meeting, when choose all contact,show the tip
     *
     * @{
     */
    @Override
    protected boolean onItemLongClick(int position, long id) {
        if (getActivity() != null && getActivity() instanceof ContactSelectionMultiTabActivity) {
            if (getAdapter().isDisplayingCheckBoxes()) {
                HashMap<Long, HashMap<String, String>> hash = getContactCache().getMultiCache();
                final long contactId = getContactId(position);
                if (!hash.containsKey(contactId)) {
                    if (((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount <= 0) {
                        Toast.makeText(
                                getContext(),
                                getContext().getString(
                                        R.string.contacts_selection_for_mms_limit,
                                        mLimitCount), Toast.LENGTH_SHORT)
                                .show();
                        return true;
                    }
                    ((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount--;
                } else {
                    ((ContactSelectionMultiTabActivity) getActivity()).mCheckedLimitCount++;
                }
            }
            return super.onItemLongClick(position, id);
        }
        return super.onItemLongClick(position, id);
    }
    /**
     * @}
     */
}
