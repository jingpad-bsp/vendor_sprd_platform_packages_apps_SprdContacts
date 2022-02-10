package com.sprd.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.Constants;
import com.android.contacts.list.IndexerListAdapter.Placement;
import com.android.contacts.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.model.AccountTypeManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.contacts.R;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;

public class AllInOneDataListAdapter extends MultiSelectEntryContactListAdapter {
    private static final String TAG = AllInOneDataListAdapter.class.getSimpleName();
    public static class DataQuery {
        private static final String[] PROJECTION_PRIMARY = new String[] {
                Data._ID,
                Data.DATA1,
                Data.CONTACT_ID,
                Data.LOOKUP_KEY,
                Data.PHOTO_ID,
                Data.PHOTO_URI,
                Data.DISPLAY_NAME_PRIMARY,
                Data.MIMETYPE,
                Contacts.DISPLAY_NAME_SOURCE,
                /**
                 * SPRD:Bug 693198 Support sdn numbers read in Contacts.
                 * @{
                 */
                RawContacts.SYNC1,
                /**
                 * @}
                 */
        };

        public static final int DATA_ID = 0;
        public static final int DATA_DATA1 = 1;
        public static final int DATA_CONTACT_ID = 2;
        public static final int DATA_LOOKUP_KEY = 3;
        public static final int DATA_PHOTO_ID = 4;
        public static final int DATA_PHOTO_URI = 5;
        public static final int DATA_DISPLAY_NAME = 6;
        public static final int DATA_MIMETYPE = 7;
        public static final int DISPLAY_NAME_SOURCE = 8;
        /**
         * SPRD:Bug 693198 Support sdn numbers read in Contacts.
         * @{
         */
        public static final int RAWCONTACTS_SYNC1 = 11;
        /**
         * @}
         */
    }

    private final CharSequence mUnknownNameText;
    private List<String> mCascadingData;
    private String mDataSelection;
    public AllInOneDataListAdapter(Context context) {
        super(context, DataQuery.DATA_ID);
        mUnknownNameText = context.getText(android.R.string.unknownName);
    }
    protected CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        Uri uri;
        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "AllInOneDataListAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }
        if (isSearchMode()) {
            String query = getQueryString();
            if (!TextUtils.isEmpty(query)) {
                uri = Data.CONTENT_URI.buildUpon()
                        .appendPath("filter")
                        .appendPath(query)
                        .build();
            } else {
                uri = Data.CONTENT_URI.buildUpon()
                        .appendPath("filter")
                        .build();
            }
        } else {
            uri = Data.CONTENT_URI.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                    .build();
        }
        if (isSectionHeaderDisplayEnabled()) {
            uri = buildSectionIndexerUri(uri);
        }
        configureSelection(loader, directoryId, getFilter());
        uri = uri.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();
        loader.setUri(uri);

        // TODO a projection that includes the search snippet
        loader.setProjection(DataQuery.PROJECTION_PRIMARY);

        /**
         * Bug449857,Bug509397 Set CursorLoader's sort order.
         * Bug643637 Contact arrangement disorder,when adding text from message
         * @{
         */
        if (getSortOrder() == ContactsPreferences.SORT_ORDER_PRIMARY) {
            loader.setSortOrder(Phone.SORT_KEY_PRIMARY);
        } else {
            loader.setSortOrder(Phone.SORT_KEY_ALTERNATIVE);
        }
        /**
         * @}
         */
    }
    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        Log.d(TAG, "configureSelection");
        if (filter == null || directoryId != Directory.DEFAULT) {
            return;
        }
        final StringBuilder selection = new StringBuilder();
        final List<String> selectionArgs = new ArrayList<String>();
        selection.append(Data.MIMETYPE + " in (");
        boolean isInit = true;
        for (String mimeType : mCascadingData) {
            if (!isInit) {
                selection.append(",");
            }
            isInit = false;
            selection.append(" \"" + mimeType + "\"");
        }
        selection.append(")");
        if ("star".equals(mDataSelection)) {
            selection.append(" AND ");
            selection.append(Contacts.STARRED + "!=0");
        }
        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_GROUP_MEMBER_ONLY_PHONE_NUMBER:
                selection.append(" AND " +
                        Data.RAW_CONTACT_ID + " IN ("
                        + "SELECT " + "data." + Data.RAW_CONTACT_ID
                        + " FROM " + "data "
                        + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
                        + " WHERE " + Data.MIMETYPE + "='"
                        + GroupMembership.CONTENT_ITEM_TYPE
                        + "' AND " + Contacts.HAS_PHONE_NUMBER + "=1"
                        + " AND " + GroupMembership.GROUP_ROW_ID +
                        "=?)");
                selectionArgs.add(String.valueOf(filter.groupId));
                break;
            case ContactListFilter.FILTER_TYPE_GROUP_MEMBER:
                selection.append(" AND " +
                        Data.RAW_CONTACT_ID + " IN ("
                        + "SELECT " + "data." + Data.RAW_CONTACT_ID
                        + " FROM " + "data "
                        + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
                        + " WHERE " + Data.MIMETYPE + "='"
                        + GroupMembership.CONTENT_ITEM_TYPE
                        + "' AND " + GroupMembership.GROUP_ROW_ID +
                        "=?)");
                selectionArgs.add(String.valueOf(filter.groupId));
                break;
            /**
             * SPRD: Bug712613 it still show contact and group of email account when disable the email  @{
             */
            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS:
                AccountTypeManager am = AccountTypeManager.getInstance(getContext());
                selection.append(" AND ");
                selection.append("(");
                List<AccountWithDataSet> accounts = am.getAccounts(false);
                for (int i = 0; i < accounts.size(); i++) {
                    selection.append("(" + RawContacts.ACCOUNT_TYPE + "=?" + " AND "
                            + RawContacts.ACCOUNT_NAME + "=?" + ")");
                    if (i != (accounts.size() - 1)) {
                        selection.append(" or ");
                    }
                    selectionArgs.add(accounts.get(i).type);
                    selectionArgs.add(accounts.get(i).name);
                }
                selection.append(")");
                break;
            /**
             * @}
             */
            default:
                Log.w(TAG, "Unsupported filter type came " +
                        "(type: " + filter.filterType + ", toString: " + filter + ")" +
                        " showing all contacts.");
                // No selection.
                break;
        }
        if (Constants.DEBUG) {
            Log.d(TAG, "selection: " + selection.toString());
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true").build();
    }
    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(DataQuery.DATA_DISPLAY_NAME);
    }
    /**
     * Builds a {@link Data#CONTENT_URI} for the given cursor position.
     *
     * @return Uri for the data. may be null if the cursor is not ready.
     */
    public Uri getDataUri(int position) {
        Cursor cursor = ((Cursor) getItem(position));
        if (cursor != null) {
            long id = cursor.getLong(DataQuery.DATA_ID);
            return ContentUris.withAppendedId(Data.CONTENT_URI, id);
        } else {
            Log.w(TAG, "Cursor was null in getDataUri() call. Returning null instead.");
            return null;
        }
    }
    /**
     * Builds a {@link Data#CONTENT_URI} for the given cursor position.
     *
     * @return Uri for the contact. may be null if the cursor is not ready.
     */
    public Uri getContactUri(int position) {
        Cursor cursor = ((Cursor) getItem(position));
        if (cursor != null) {
            int partitionIndex = getPartitionForPosition(position);
            return getContactUri(partitionIndex, cursor, DataQuery.DATA_CONTACT_ID,
                    DataQuery.DATA_LOOKUP_KEY);
        } else {
            Log.w(TAG, "Cursor was null in getContactUri() call. Returning null instead.");
            return null;
        }
    }

    @Override
    protected ContactListItemView newView(Context context, int partition, Cursor cursor, int position,ViewGroup parent) {
        final ContactListItemView view = super.newView(context, partition, cursor, position, parent);
        view.setUnknownNameText(mUnknownNameText);
        view.setQuickContactEnabled(isQuickContactEnabled());
        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        bindSectionHeaderAndDivider(view, position);
        cursor.moveToPosition(position);
        boolean isFirstEntry = true;
        final long currentContactId = cursor.getLong(DataQuery.DATA_CONTACT_ID);
        if (cursor.moveToPrevious() && !cursor.isBeforeFirst()) {
            final long previousContactId = cursor.getLong(DataQuery.DATA_CONTACT_ID);
            if (currentContactId == previousContactId) {
                isFirstEntry = false;
            }
        }
        cursor.moveToPosition(position);
        String data = cursor.getString(DataQuery.DATA_DISPLAY_NAME);
        String data1 = cursor.getString(DataQuery.DATA_DATA1);
        if (isFirstEntry) {
            bindName(view, cursor);
            bindPhoto(view, cursor, DataQuery.DATA_PHOTO_ID, DataQuery.DATA_LOOKUP_KEY,
                    DataQuery.DATA_DISPLAY_NAME);
        } else {
            unbindName(view);
            view.removePhotoView(true, false);
        }
        bindAllInOneData(view, cursor);
    }

    protected void bindAllInOneData(ContactListItemView view, Cursor cursor) {
        // TODO: bind mime type to label
        String mimeType = cursor.getString(DataQuery.DATA_MIMETYPE);
        int resId = -1;
        if (mimeType != null) {
            resId = mimeToRes(mimeType);
        }
        if (resId != -1) {
            view.setLabel(getContext().getText(resId));
        }
        view.showData(cursor, DataQuery.DATA_DATA1);
    }

    public int mimeToRes(String mimeType) {
        if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
            return R.string.res_name;
        }
        if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
            return R.string.res_phone;
        }
        if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
            return R.string.res_email;
        }
        return R.string.res_field;
    }

    protected void bindSectionHeaderAndDivider(final ContactListItemView view, int position) {
        if (isSectionHeaderDisplayEnabled()) {
            Placement placement = getItemPlacementInSection(position);
            view.setSectionHeader(placement.firstInSection ? placement.sectionHeader : null);
        } else {
            view.setSectionHeader(null);
        }
    }

    protected void bindName(final ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, DataQuery.DATA_DISPLAY_NAME, getContactNameDisplayOrder());
        // Note: we don't show phonetic names any more (see issue 5265330)
    }

    protected void unbindName(final ContactListItemView view) {
        view.hideDisplayName();
    }

    void setCascadingData(List<String> data) {
        mCascadingData = data;
    }

    public void setLoaderSelection(String selection) {
        mDataSelection = selection;
    }

}
