package com.sprd.contacts.list;

import com.sprd.contacts.util.MultiContactDataCacheUtils;
import com.sprd.contacts.util.MultiContactDataCacheUtils.CacheMode;
import android.database.Cursor;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import com.android.contacts.R;
import android.view.Menu;
import com.sprd.contacts.activities.ContactSelectionMultiTabActivity;

public class AllInOneBrowserPickerFragment extends AllInOneDataPickerFragment {

    public AllInOneBrowserPickerFragment() {
        super();
    }

    @Override
    public void onStart() {
        super.onStart();
        displayCheckBoxes(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final boolean isSearchMode = ((ContactSelectionMultiTabActivity)getActivity()).isSearchMode();
        setVisible(menu, R.id.menu_select, false);
        final boolean showSelectAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) &&(!getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_select_all, showSelectAllOption);
        final boolean showCancelAllOption = (!isEmptyAccountViewVisibility()) && (!isSearchMode) && (getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_cancel_all, showCancelAllOption);
    }

}
