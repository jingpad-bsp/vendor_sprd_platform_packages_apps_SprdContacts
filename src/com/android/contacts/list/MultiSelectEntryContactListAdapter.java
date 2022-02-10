/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.CheckBox;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.group.GroupUtil;

import java.util.TreeSet;
import java.util.Iterator;
/**
 * androido-poting bug474752 Add features with multiSelection activity in Contacts.
 *
 * @{
 */
import java.util.ArrayList;
import android.util.Log;
import java.util.HashMap;
import com.sprd.contacts.util.MultiContactDataCacheUtils;
/**
 * @}
 */
/**
 * An extension of the default contact adapter that adds checkboxes and the ability
 * to select multiple contacts.
 */
public abstract class MultiSelectEntryContactListAdapter extends ContactEntryListAdapter {

    private SelectedContactsListener mSelectedContactsListener;
    private DeleteContactListener mDeleteContactListener;
    private TreeSet<Long> mSelectedContactIds = new TreeSet<>();
    private boolean mDisplayCheckBoxes;
    private final int mContactIdColumnIndex;
    public interface SelectedContactsListener {
        void onSelectedContactsChanged();
    }

    public interface DeleteContactListener {
        void onContactDeleteClicked(int position);
    }

    /**
     * @param contactIdColumnIndex the column index of the contact ID in the underlying cursor;
     *         it is passed in so that this adapter can support different kinds of contact
     *         lists (e.g. aggregate contacts or raw contacts).
     */
    public MultiSelectEntryContactListAdapter(Context context, int contactIdColumnIndex) {
        super(context);
        mContactIdColumnIndex = contactIdColumnIndex;
    }

    /**
     * Returns the column index of the contact ID in the underlying cursor; the contact ID
     * retrieved using this index is the value that is selected by this adapter (and returned
     * by {@link #getSelectedContactIds}).
     */
    public int getContactColumnIdIndex() {
        return mContactIdColumnIndex;
    }

    public DeleteContactListener getDeleteContactListener() {
        return mDeleteContactListener;
    }

    public void setDeleteContactListener(DeleteContactListener deleteContactListener) {
        mDeleteContactListener = deleteContactListener;
    }

    public void setSelectedContactsListener(SelectedContactsListener listener) {
        mSelectedContactsListener = listener;
    }

    /**
     * Returns set of selected contacts.
     */
    public TreeSet<Long> getSelectedContactIds() {
        return mSelectedContactIds;
    }

    public boolean hasSelectedItems() {
        return mSelectedContactIds.size() > 0;
    }

    /**
     * Returns the selected contacts as an array.
     */
    public long[] getSelectedContactIdsArray() {
        return GroupUtil.convertLongSetToLongArray(mSelectedContactIds);
    }

    /**
     * androido-poting bug474752 Add features with multiSelection activity in Contacts.
     *
     * @{
     */
    private HashMap<Long, Cursor> mSelectedContactCursors = new HashMap<Long, Cursor>();
    private MultiContactDataCacheUtils mContactCache = new MultiContactDataCacheUtils();

    public ArrayList<String> getSelectedContactIdsArrayList() {
        long[] mSelectedContactIds = getSelectedContactIdsArray();
        ArrayList<String> contactIds = new ArrayList<String>();
        for (int i=0; i< mSelectedContactIds.length; i++) {
            contactIds.add(String.valueOf(mSelectedContactIds[i]));
        }
        return contactIds;
    }

    public boolean isAllContactSelected() {
        final TreeSet<Long> mAllContactIds = getAllContactIds();
        if (mSelectedContactIds.size() == mAllContactIds.size()) {
            return true;
        }
        return false;
    }

    public TreeSet<Long> getAllContactIds() {
        TreeSet<Long> mAllContactIds = new TreeSet<Long>();
        int count = getCount();
        for(int i=0; i<count; i++) {
            final Cursor cursor = (Cursor) getItem(i);
            if (cursor != null) {
                if (cursor.getColumnCount() > mContactIdColumnIndex) {
                    long contactId = cursor.getLong(mContactIdColumnIndex);
                    if (!mAllContactIds.contains(contactId)) {
                        mAllContactIds.add(contactId);
                    }
                }
            }
        }
        return mAllContactIds;
    }

    public void cancelAllContactSelected() {
        mSelectedContactIds.clear();
        mContactCache.clear();
        notifyDataSetChanged();
      if (mSelectedContactsListener != null) {
          mSelectedContactsListener.onSelectedContactsChanged();
      }
    }

    public void setContactDataCache(MultiContactDataCacheUtils cache) {
        mContactCache = cache;
    }

    /**
     * SPRD BUG:728697 The presence of 3000+ contacts, the main interface - select all,
     * click the home button, the contacts crash
     * @{
     **/
    public void setContactDataCache(TreeSet<Long> selectedContactIds) {
        mContactCache.clear();
        int mContactDataSize = 0;
        if (selectedContactIds != null) {
            int count = getCount();
            for (int i=0; i<count; i++) {
                Cursor cursor = (Cursor) getItem(i);
                if (cursor != null && cursor.getColumnCount() > mContactIdColumnIndex) {
                    long contactId = cursor.getLong(mContactIdColumnIndex);
                    if (selectedContactIds.contains(contactId)) {
                        mContactCache.addCacheItem(cursor, contactId);
                        mContactDataSize += 1;
                    }
                }
                if (mContactDataSize == selectedContactIds.size()) {
                    break;
                }
            }
        }
    }
    /**
     * @}
     *
     **/

    public void clearContactDataCache() {
        mContactCache.clear();
    }

    public MultiContactDataCacheUtils getContactDataCache() {
        return mContactCache;
    }

    public void setAllContactSelected() {
        int count = getCount();
        for(int i=0; i<count; i++) {
            final Cursor cursor = (Cursor) getItem(i);
            if (cursor != null) {
                if (cursor.getColumnCount() > mContactIdColumnIndex) {
                    Long contactId = cursor.getLong(mContactIdColumnIndex);
                    if (!mSelectedContactIds.contains(contactId)) {
                        mSelectedContactIds.add(contactId);
                    }
                    if (!mContactCache.containsCache(contactId)) {
                        mContactCache.addCacheItem(cursor, contactId);
                    }
                }
            }
        }
        notifyDataSetChanged();
        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }
    }
    /**
     * @}
     */

    /**
     * UNISOC: Bug938459/1015715 During conference meeting, when choose all contact,show the tip
     * @{
     */
    public void setAllContactSelected(int count) {
        mSelectedContactIds.clear();
        mContactCache.clear();
        /**
         * UNISOC: Bug1064874 can not delete 3500 contacts when has favorites @{
         */
        int index = 0;
        int maxCount = getCount();
        while (mSelectedContactIds.size() < count && index < maxCount) {
            final Cursor cursor = (Cursor) getItem(index);
            if (cursor != null && cursor.getColumnCount() > mContactIdColumnIndex) {
                Long contactId = cursor.getLong(mContactIdColumnIndex);
                if (!mSelectedContactIds.contains(contactId)) {
                    mSelectedContactIds.add(contactId);
                    mContactCache.addCacheItem(cursor, contactId);
                }

            }
            index++;
        }
        /**
         * @}
         */
        notifyDataSetChanged();
        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }
    }
    /**
     * @}
     */

    /**
     * Update set of selected contacts. This changes which checkboxes are set.
     */
    public void setSelectedContactIds(TreeSet<Long> selectedContactIds) {
        this.mSelectedContactIds = selectedContactIds;
        notifyDataSetChanged();
        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    /**
     * Shows checkboxes beside contacts if {@param displayCheckBoxes} is {@code TRUE}.
     * Not guaranteed to work with all configurations of this adapter.
     */
    public void setDisplayCheckBoxes(boolean showCheckBoxes) {
        mDisplayCheckBoxes = showCheckBoxes;
        notifyDataSetChanged();
        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    /**
     * Checkboxes are being displayed beside contacts.
     */
    public boolean isDisplayingCheckBoxes() {
        return mDisplayCheckBoxes;
    }

    /**
     * Toggle the checkbox beside the contact for {@param contactId}.
     */
    public void toggleSelectionOfContactId(long contactId, int position) {
        if (mSelectedContactIds.contains(contactId)) {
            mSelectedContactIds.remove(contactId);
            mContactCache.remove((Long)contactId);
        } else {
            mSelectedContactIds.add(contactId);
            Cursor cursor = (Cursor) getItem(position);
            mContactCache.addCacheItem(cursor,(Long)contactId);
        }
        notifyDataSetChanged();
        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor = (Cursor) getItem(position);
        if (cursor != null) {
            return cursor.getLong(getContactColumnIdIndex());
        }
        return 0;
    }
    /**
     * SPRD:Bug 505144 Adding a default people  androidO_porting.
     * @{
     */
    public boolean isContactReadOnly(int position, Cursor cursor) {
        if (cursor == null) {
            cursor = (Cursor) getItem(position);
        }
        if (cursor != null) {
            int index = cursor.getColumnIndex("is_read_only");
            if (index != -1) {
                return 1 == cursor.getInt(index);
            }
        }
        return false;
    }
    /**
     * @}
     */
    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        final ContactListItemView view = (ContactListItemView) itemView;
        bindViewId(view, cursor, getContactColumnIdIndex());
        bindCheckBox(view, cursor, partition == ContactsContract.Directory.DEFAULT);
    }

    /**
      * Loads the photo for the photo view.
      * @param photoIdColumn Index of the photo id column
      * @param lookUpKeyColumn Index of the lookup key column
      * @param displayNameColumn Index of the display name column
      */
    protected void bindPhoto(final ContactListItemView view, final Cursor cursor,
           final int photoIdColumn, final int lookUpKeyColumn, final int displayNameColumn) {
        final long photoId = cursor.isNull(photoIdColumn)
            ? 0 : cursor.getLong(photoIdColumn);
        final ContactPhotoManager.DefaultImageRequest imageRequest = photoId == 0
            ? getDefaultImageRequestFromCursor(cursor, displayNameColumn,
            lookUpKeyColumn)
            : null;
        getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false, getCircularPhotos(),
                imageRequest);
    }

    private void bindCheckBox(ContactListItemView view, Cursor cursor, boolean isLocalDirectory) {
        // Disable clicking on all contacts from remote directories when showing check boxes. We do
        // this by telling the view to handle clicking itself.
        view.setClickable(!isLocalDirectory && mDisplayCheckBoxes);
        // Only show checkboxes if mDisplayCheckBoxes is enabled. Also, never show the
        // checkbox for other directory contacts except local directory.
        if (!mDisplayCheckBoxes || !isLocalDirectory) {
            view.hideCheckBox();
            return;
        }
        final CheckBox checkBox = view.getCheckBox();
        final long contactId = cursor.getLong(mContactIdColumnIndex);
        checkBox.setChecked(mSelectedContactIds.contains(contactId));
        checkBox.setClickable(false);
        checkBox.setTag(contactId);
    }

    /**
     * Bug 712274 In the MultiSelectActivity when the select items changes,
     * the textView which shows the number of selected items on the the upper left corner will non-synchronous update.
     *
     * @{
     */
    public void updateChecked() {

        ArrayList<Long> list = getCurrentItems();
        ArrayList<Long> delList = new ArrayList<>();

        for (Long checkedId : mSelectedContactIds) {
            if (!list.contains(checkedId)) {
                delList.add(checkedId);
                mContactCache.remove(checkedId);
            }
        }
        mSelectedContactIds.removeAll(delList);
        notifyDataSetChanged();

        if (mSelectedContactsListener != null) {
            mSelectedContactsListener.onSelectedContactsChanged();
        }

    }

    public ArrayList<Long> getCurrentItems() {
        int count = getCount();
        ArrayList<Long> viewItems = new ArrayList<Long>();
        for (int i = 0; i < count; ++i) {
            Long contactId = getContactId(i);
            if (contactId == 0) {
                continue;
            }
            viewItems.add(contactId);

        }
        return viewItems;
    }

    public long getContactId(int position) {
        Cursor item = (Cursor) getItem(position);
        if (item != null) {
            return item.getLong(0);
        }
        return (long) 0;
    }
    /**
     * @}
     */
}
