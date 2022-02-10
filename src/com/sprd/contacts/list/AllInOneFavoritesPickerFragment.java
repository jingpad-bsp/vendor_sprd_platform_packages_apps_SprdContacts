package com.sprd.contacts.list;

import java.util.List;
import android.os.Bundle;
import java.util.ArrayList;
import com.android.contacts.R;
import android.view.Menu;
import com.sprd.contacts.activities.ContactSelectionMultiTabActivity;

public class AllInOneFavoritesPickerFragment extends AllInOneDataPickerFragment {
    private static final String TAG = AllInOneFavoritesPickerFragment.class.getSimpleName();

    private static final String KEY_DATA_SELECTION = "data_selection";
    private static final String KEY_CASCADING_DATA = "cascadingData";

    private static final String EMIAL_TYPE_DATA = "vnd.android.cursor.item/email_v2";

    private String mDataSelection;
    private List<String> mCascadingData;

    public AllInOneFavoritesPickerFragment() {
        super();
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }
        mCascadingData = savedState.getStringArrayList(KEY_CASCADING_DATA);
        mDataSelection = savedState.getString(KEY_DATA_SELECTION);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCascadingData != null) {
            outState.putStringArrayList(KEY_CASCADING_DATA, new ArrayList<String>(mCascadingData));
        }
        if (mDataSelection != null) {
            outState.putString(KEY_DATA_SELECTION, mDataSelection);
        }
    }

    public void setSelection(String data) {
        mDataSelection = data;
    }

    public void setCascadingData(List<String> data) {
        super.setCascadingData(data);
        mCascadingData = data;
    }

    @Override
    public void onStart() {
        super.onStart();
        displayCheckBoxes(true);
    }

    @Override
    protected AllInOneDataListAdapter createListAdapter() {
        final AllInOneDataListAdapter adapter = new AllInOneDataListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        if (mDataSelection != null) {
            adapter.setLoaderSelection(mDataSelection);
        }
        if (mCascadingData != null) {
            adapter.setCascadingData(mCascadingData);
        }
        return adapter;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setVisible(menu, R.id.menu_select, false);
        final boolean isSearchMode = ((ContactSelectionMultiTabActivity)getActivity()).isSearchMode();
        final boolean showSelectAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) && (!getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_select_all, showSelectAllOption);
        final boolean showCancelAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) && (getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_cancel_all, showCancelAllOption);
        /**SPRD: Bug742181 The prompt is error when search no result for share by sms.
         * @{
         */
        setVisible(menu, R.id.menu_search, (!isEmptyAccountViewVisibility()) && !isSearchMode);
        /**
         * @}
         */
    }

}
