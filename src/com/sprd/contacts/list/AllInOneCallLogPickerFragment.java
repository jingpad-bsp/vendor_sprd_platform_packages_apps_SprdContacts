package com.sprd.contacts.list;

import com.sprd.contacts.util.MultiContactDataCacheUtils.CacheMode;
import android.provider.CallLog.Calls;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import android.widget.TextView;
import com.android.contacts.R;
import android.view.Menu;
import com.sprd.contacts.activities.ContactSelectionMultiTabActivity;
import android.database.Cursor;
import android.content.Loader;

public class AllInOneCallLogPickerFragment extends AllInOneDataPickerFragment {
    private static final String TAG = AllInOneCallLogPickerFragment.class.getSimpleName();

    public AllInOneCallLogPickerFragment() {
        super();
        setContactCacheModel(CacheMode.MULTI, Calls.NUMBER, Calls.CACHED_NAME, null);
        setSectionHeaderDisplayEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        displayCheckBoxes(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setVisible(menu, R.id.menu_select, false);
        final boolean isSearchMode = ((ContactSelectionMultiTabActivity)getActivity()).isSearchMode();
        final boolean showSelectAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) && (!getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_select_all, showSelectAllOption);
        final boolean showCancelAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) && (getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_cancel_all, showCancelAllOption);
    }

    @Override
    protected AllInOneCallLogListAdapter createListAdapter() {
        AllInOneCallLogListAdapter adapter = new AllInOneCallLogListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    public HashMap<String, String> getSelectedDatas() {
        HashMap<Long, HashMap<String, String>> hash = getContactCache().getMultiCache();
        HashMap<String, String> temp = new HashMap<String, String>();
        HashMap<String, String> ret = new HashMap<String, String>();
        Iterator it = hash.entrySet().iterator();
        while(it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (entry.getKey() != null) {
                temp = hash.get(entry.getKey());
                String number = temp.get(Calls.NUMBER);
                /** UNISOC: add for bug1113328, Ignore the space effect when judging the same contact number. @{ */
                number = number.replaceAll(" ", "");
                /** @｝ */
                String name = temp.get(Calls.CACHED_NAME);
                ret.put(number,name);
            }
        }
        return ret;
    }

    /**
     * SPRD: Bug729362 The style in the tab for ContactSelectionMultiTabActivity is different
     * @{
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        //UNISOC: modify for bug1201358, close calllog permission and add block number,calllog tab display abnormall.
        if (data == null || (data != null && data.getCount() < 1)) {
            TextView text = getEmptyView();
            if (text != null) {
                text.setText(R.string.listFoundAllCallLogZero);
            }
        }
    }
    /**
     * @}
     */
}
