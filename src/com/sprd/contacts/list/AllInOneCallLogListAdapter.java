package com.sprd.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Directory;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;


import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.sprd.contacts.util.AccountRestrictionUtils;
import com.android.contacts.list.IndexerListAdapter.Placement;
import com.android.contacts.ContactPhotoManager.DefaultImageRequest;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.os.UserManager;

public class AllInOneCallLogListAdapter extends AllInOneDataListAdapter {
    private static final String TAG = AllInOneCallLogListAdapter.class.getSimpleName();
    private Context mContext = null;
    protected static class CallQuery {
        private static final String[] CALL_LOG_PROJECTION_NEWUI = new String[] {
                Calls._ID,
                Calls.NUMBER,
                Calls.TYPE,
                Calls.CACHED_NAME,
                Calls.CACHED_NUMBER_TYPE,
                Calls.PHONE_ACCOUNT_ID,
                Calls.CACHED_PHOTO_ID,
                Calls.DATE
        };

        public static final int ID_COLUMN_INDEX = 0;

        public static final int NUMBER_COLUMN_INDEX = 1;

        public static final int CALL_TYPE_COLUMN_INDEX = 2;

        public static final int CALLER_NAME_COLUMN_INDEX = 3;

        public static final int CALLER_NUMBERTYPE_COLUMN_INDEX = 4;

        public static final int SIM_COLUMN_INDEX = 5;

        public static final int CALLER_PHOTO_COLUMN_INDEX = 6;

        public static final int CALLER_DATE_COLUMN_INDEX = 7;

    }

    public AllInOneCallLogListAdapter(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(CallQuery.CALLER_NAME_COLUMN_INDEX);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {

        Uri uri;

        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "AllInOneCallLogListAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }
        /*SPRD:811430 The call log is grouped by normalized_number @{*/
        if (isSearchMode()) {
            String query = getQueryString();
            if (!TextUtils.isEmpty(query)) {
                uri = Calls.CONTENT_URI.buildUpon()
                        .appendPath("calls_only_contacts")
                        .appendPath(query)
                        .build();
            } else {
                uri = Calls.CONTENT_URI.buildUpon()
                        .appendPath("calls_only_contacts")
                        .build();
            }

        } else {
            uri = Calls.CONTENT_URI.buildUpon()
                    .appendPath("calls_only_contacts")
                    .appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                    .build();
        }
        /* @} */
        configureSelection(loader, directoryId, null);

        // Remove duplicates when it is possible.
        uri = uri.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();
        loader.setUri(uri);

        loader.setProjection(CallQuery.CALL_LOG_PROJECTION_NEWUI);
        loader.setSortOrder("_id desc");
    }

    private void configureSelection(
            CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (directoryId != Directory.DEFAULT) {
            return;
        }
        /**UNISOC:bug 794767 "Is not null" will find out both content and content.
         * @{
         **/
        String selection = Calls.NUMBER + " !='' ";
        /**
         * @}
         **/
        String[] selectionArgs = null;

        /* UNISOC: modify for bug1195285, distinguish guest and owner in callLog @{ */
        UserManager userManager = (UserManager)mContext
                .getSystemService(Context.USER_SERVICE);
        if (!userManager.isSystemUser()) {
            selection += " AND users_id = " + Integer.toString(android.os.Process.myUserHandle()
                    .hashCode());
        }
        /* @} */
        loader.setSelection(selection);
        loader.setSelectionArgs(selectionArgs);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        ContactListItemView view = (ContactListItemView) itemView;
        cursor.moveToPosition(position);
        boolean isFirstEntry = true;
        boolean showBottomDivider = true;
        final long currentContactId = cursor.getLong(CallQuery.CALLER_DATE_COLUMN_INDEX);
        if (cursor.moveToPrevious() && !cursor.isBeforeFirst()) {
            final long previousContactId = cursor.getLong(CallQuery.CALLER_DATE_COLUMN_INDEX);
            if (currentContactId == previousContactId) {
                isFirstEntry = false;
            }
        }
        cursor.moveToPosition(position);
        if (cursor.moveToNext() && !cursor.isAfterLast()) {
            final long nextContactId = cursor.getLong(CallQuery.CALLER_DATE_COLUMN_INDEX);
            if (currentContactId == nextContactId) {
                showBottomDivider = false;
            }
        }
        cursor.moveToPosition(position);
        bindSectionHeaderAndDivider(view, position);
        if (isFirstEntry) {
            bindName(view, cursor);
            bindPhoto(view, cursor);
        } else {
            unbindName(view);
            view.removePhotoView(true, false);
        }
        bindAllInOneData(view, cursor);
    }

    protected void bindName(final ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, CallQuery.CALLER_NAME_COLUMN_INDEX,
                getContactNameDisplayOrder());
    }

    protected void bindPhoto(final ContactListItemView view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(CallQuery.CALLER_PHOTO_COLUMN_INDEX)) {
            photoId = cursor.getLong(CallQuery.CALLER_PHOTO_COLUMN_INDEX);
        }
        DefaultImageRequest request = null;
        if (photoId == 0) {
            request = getDefaultImageRequestFromCursor(cursor, CallQuery.CALLER_NAME_COLUMN_INDEX,
                    CallQuery.CALLER_DATE_COLUMN_INDEX );
        }
        getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false, true, request);
    }

    protected void bindAllInOneData(ContactListItemView view, Cursor cursor) {
        String mimeType = "vnd.android.cursor.item/phone_v2";
        int resId = -1;
        if (mimeType != null) {
            resId = AccountRestrictionUtils.get(getContext()).mimeToRes(mimeType);
        }
        if (resId != -1) {
            view.setLabel(getContext().getText(resId));
        }
        view.showData(cursor, CallQuery.NUMBER_COLUMN_INDEX);
    }
}
