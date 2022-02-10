/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.sprd.contacts.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.group.GroupMembersAdapter;
import com.android.contacts.group.GroupMembersAdapter.GroupMembersQuery;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.list.ContactsSectionIndexer;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.UiIntentActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fragment containing raw contacts for moving group memebers.
 */
public class GroupMemberMoveFragmentSprd extends
        MultiSelectContactsListFragment<GroupMembersAdapter> {

    public static final String TAG = "GroupMemberMove";

    private static final String KEY_GROUP_METADAT = "group_metadata";
    private static final String KEY_OTHER_GROUP_IDS = "other_group_ids";
    private static final String KEY_OTHER_GROUP_TITLES = "other_group_titles";

    private static final String ARG_GROUP_METADATA = "group_metadata";
    private static final String ARG_CONTACT_IDS = "contactIds";
    private static final String ARG_OTHER_GROUP_IDS = "other_group_ids";
    private static final String ARG_OTHER_GROUP_TITLES = "other_group_titles";

    private GroupMetaData mGroupMetaData;
    private ArrayList<String> mOtherGroupIds;
    private ArrayList<String> mOtherGroupTitles;

    public static GroupMemberMoveFragmentSprd newInstance(
            GroupMetaData groupMetaData, ArrayList<String> groupMembersIds,
            ArrayList<String> otherGroupIds, ArrayList<String> otherGroupTitles) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_GROUP_METADATA, groupMetaData);
        args.putStringArrayList(ARG_CONTACT_IDS, groupMembersIds);
        args.putStringArrayList(ARG_OTHER_GROUP_IDS, otherGroupIds);
        args.putStringArrayList(ARG_OTHER_GROUP_TITLES, otherGroupTitles);

        final GroupMemberMoveFragmentSprd fragment = new GroupMemberMoveFragmentSprd();
        fragment.setArguments(args);
        return fragment;
    }

    public GroupMemberMoveFragmentSprd() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setHasOptionsMenu(true);
        setSearchMode(false);
        setDisplayDirectoryHeader(false);
    }

    @Override
    public void onCreate(Bundle savedState) {
        if (savedState == null) {
            mGroupMetaData = getArguments().getParcelable(ARG_GROUP_METADATA);
            mOtherGroupIds = getArguments().getStringArrayList(ARG_OTHER_GROUP_IDS);
            mOtherGroupTitles = getArguments().getStringArrayList(ARG_OTHER_GROUP_TITLES);
        } else {
            mGroupMetaData = savedState.getParcelable(KEY_GROUP_METADAT);
            mOtherGroupIds = savedState.getStringArrayList(KEY_OTHER_GROUP_IDS);
            mOtherGroupTitles = savedState.getStringArrayList(KEY_OTHER_GROUP_TITLES);
        }
        super.onCreate(savedState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_GROUP_METADAT, mGroupMetaData);
        outState.putStringArrayList(KEY_OTHER_GROUP_IDS, mOtherGroupIds);
        outState.putStringArrayList(KEY_OTHER_GROUP_TITLES, mOtherGroupTitles);
    }

    private Set<String> mGroupMemberContactIds = new HashSet();
    /**
     * Filters out duplicate contacts.
     */
    private class FilterCursorWrapper extends CursorWrapper {

        private int[] mIndex;
        private int mCount = 0;
        private int mPos = 0;

        public FilterCursorWrapper(Cursor cursor) {
            super(cursor);

            mCount = super.getCount();
            mIndex = new int[mCount];

            final List<Integer> indicesToFilter = new ArrayList<>();

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Group members CursorWrapper start: " + mCount);
            }

            final Bundle bundle = cursor.getExtras();
            final String sections[] = bundle.getStringArray(Contacts
                    .EXTRA_ADDRESS_BOOK_INDEX_TITLES);
            final int counts[] = bundle.getIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
            final ContactsSectionIndexer indexer = (sections == null || counts == null)
                    ? null : new ContactsSectionIndexer(sections, counts);

            mGroupMemberContactIds.clear();
            for (int i = 0; i < mCount; i++) {
                super.moveToPosition(i);
                final String contactId = getString(GroupMembersQuery.CONTACT_ID);
                if (!mGroupMemberContactIds.contains(contactId)) {
                    mIndex[mPos++] = i;
                    mGroupMemberContactIds.add(contactId);
                } else {
                    indicesToFilter.add(i);
                }
            }

            if (indexer != null && GroupUtil.needTrimming(mCount, counts, indexer.getPositions())) {
                GroupUtil.updateBundle(bundle, indexer, indicesToFilter, sections, counts);
            }

            mCount = mPos;
            mPos = 0;
            super.moveToFirst();

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Group members CursorWrapper end: " + mCount);
            }
        }

        @Override
        public boolean move(int offset) {
            return moveToPosition(mPos + offset);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(mPos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(mPos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(mCount - 1);
        }

        @Override
        public boolean moveToPosition(int position) {
            if (position >= mCount) {
                mPos = mCount;
                return false;
            } else if (position < 0) {
                mPos = -1;
                return false;
            }
            mPos = mIndex[position];
            return super.moveToPosition(mPos);
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int getPosition() {
            return mPos;
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(com.android.contacts.R.layout.contact_list_content, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            // Wait until contacts are loaded before showing the scrollbar
            setVisibleScrollbarEnabled(true);

            final FilterCursorWrapper cursorWrapper = new FilterCursorWrapper(data);
            super.onLoadFinished(loader, cursorWrapper);
        }
    }

    @Override
    protected GroupMembersAdapter createListAdapter() {
        final GroupMembersAdapter adapter = new GroupMembersAdapter(getContext());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        if (mGroupMetaData != null) {
            getAdapter().setGroupId(mGroupMetaData.groupId);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.group_member_move_picker, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setVisible(menu, R.id.menu_search, false);
        setVisible(menu, R.id.menu_done, getAdapter().hasSelectedItems());
        setVisible(menu, R.id.menu_contacts_select_all,!getAdapter().isAllContactSelected());
        setVisible(menu, R.id.menu_contacts_cancel_all, getAdapter().isAllContactSelected());
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
        } else if (id == R.id.menu_done) {
            showTargetGroupDialogFragment();
            return true;
        } else if (id == R.id.menu_contacts_select_all) {
            SelectAllContacts();
            return true;
        } else if (id == R.id.menu_contacts_cancel_all) {
            CancelAllContacts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final long[] selectedIds = getActivity().getIntent().getLongArrayExtra(
                UiIntentActions.SELECTION_DEFAULT_SELECTION);
        if (selectedIds != null && selectedIds.length != 0) {
            final TreeSet<Long> selectedIdsTree = new TreeSet<>();
            for (int i = 0; i < selectedIds.length; i++) {
                selectedIdsTree.add(selectedIds[i]);
            }
            getAdapter().setSelectedContactIds(selectedIdsTree);
            onSelectedContactsChanged();
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        displayCheckBoxes(true);
    }

    @Override
    protected boolean onItemLongClick(int position, long id) {
        return true;
    }

    /**
     * SPRD: Bug721602 move group members fail occasionally
     * @{
     */
    private long[] getRawContactIds(long[] contactIds) {
        if(contactIds == null || contactIds.length == 0) {
            return null;
        }
        final Uri.Builder builder = RawContacts.CONTENT_URI.buildUpon();
        String accountName = null;
        String accountType = null;
        String dataSet = null;

        if (mGroupMetaData != null) {
            accountName = mGroupMetaData.accountName;
            accountType = mGroupMetaData.accountType;
            dataSet = mGroupMetaData.dataSet;
        }
        if (accountName != null) {
            builder.appendQueryParameter(RawContacts.ACCOUNT_NAME, accountName);
        }
        if (accountType != null) {
            builder.appendQueryParameter(RawContacts.ACCOUNT_TYPE, accountType);
        }
        if (dataSet != null) {
            builder.appendQueryParameter(RawContacts.DATA_SET, dataSet);
        }
        final Uri rawContactUri = builder.build();
        final String[] projection = new String[]{ContactsContract.RawContacts._ID};
        final StringBuilder selection = new StringBuilder();
        final String[] selectionArgs = new String[contactIds.length];
        selection.append(ContactsContract.RawContacts.CONTACT_ID).append(" IN ");
        String strContactIds = Arrays.toString(contactIds).replace("[", "(").replace("]", ")");
        selection.append(strContactIds);
        final Cursor cursor = getContext().getContentResolver().query(
                rawContactUri, projection, selection.toString(), null, null, null);
        final long[] rawContactIds = new long[cursor.getCount()];
        try {
            int i = 0;
            while (cursor.moveToNext()) {
                rawContactIds[i] = cursor.getLong(0);
                i++;
            }
        } finally {
            cursor.close();
        }
        return rawContactIds;
    }
    /**
     * @}
     */

    private void showTargetGroupDialogFragment() {
        TargetGroupDialogFragment fragment = new TargetGroupDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("srcGroupId", mGroupMetaData.groupId);
        bundle.putStringArrayList("groupId", mOtherGroupIds);
        bundle.putStringArrayList("groupTitle", mOtherGroupTitles);
        // SPRD: Bug721602 move group members fail occasionally
        bundle.putLongArray("memberToMove", getRawContactIds(getAdapter().getSelectedContactIdsArray()));
        fragment.setArguments(bundle);
        fragment.show(getFragmentManager(), "moveGroupMembers");
    }

    public static class TargetGroupDialogFragment extends DialogFragment {
        private static final String TARGET_GROUP_ID = "targetGroupId";
        private static final String SRC_GROUP_ID = "srcGroupId";
        private static long mTargetGroupId = -1;
        private static long mSrcGroupId;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mTargetGroupId = savedInstanceState.getLong(TARGET_GROUP_ID);
                mSrcGroupId = savedInstanceState.getLong(SRC_GROUP_ID);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mSrcGroupId = getArguments().getLong("srcGroupId");
            ArrayList<String> groupIds = getArguments().getStringArrayList("groupId");
            ArrayList<String> groupTitles = getArguments().getStringArrayList("groupTitle");
            final long[] member = getArguments().getLongArray("memberToMove");
            final String[] titles = (String[]) groupTitles.toArray(new String[groupTitles.size()]);
            if (groupIds.size() > 0) {
                /* SPRD:Bug 406295 @{ */
                // mTargetGroupId = Long.valueOf(ids[0]);
                mTargetGroupId = (mTargetGroupId == -1) ? Long.valueOf(groupIds.get(0)) : mTargetGroupId;
                /* @} */
            }

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.moveGroupMemberDialogTitle)
                    .setSingleChoiceItems(titles, 0,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which >= 0) {
                                        mTargetGroupId = Long.valueOf(groupIds.get(which));
                                    }
                                }
                            })
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (getActivity() != null) {
                                        Intent serviceIntent = ContactSaveService
                                                .createGroupMoveIntent(
                                                        getActivity(), mTargetGroupId,
                                                        mSrcGroupId, member,
                                                        ContactSelectionActivity.class,
                                                        GroupUtil.ACTION_MOVE_GROUP_COMPLETE);
                                        getContext().startService(serviceIntent);
                                    }
                                }
                                })
                    .setNegativeButton(android.R.string.cancel, null).create();
            return dialog;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putLong(TARGET_GROUP_ID, mTargetGroupId);
            outState.putLong(SRC_GROUP_ID, mSrcGroupId);
        }

        /**
         * SPRD: Bug718837 not recovery mTargetGroupId on onStop when lock screen
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            mTargetGroupId = -1;
        }
        /**
         * @}
         */
    }
}