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

import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;

import com.android.contacts.preference.ContactsPreferences;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A loader for use in the default contact list, which will also query for favorite contacts
 * if configured to do so.
 */
public class FavoritesAndContactsLoader extends CursorLoader {

    private boolean mLoadFavorites;

    private String[] mProjection;

    /**
     * SPRD: Bug746238 it should not calculate the count of stared contacts
     * @{
     */
    private int mFavoriteCount;

    public int getmFavoriteCount() {
        return mFavoriteCount;
    }
    /**
     * @}
     */

    public FavoritesAndContactsLoader(Context context) {
        super(context);
    }

    /** Whether to load favorites and merge results in before any other results. */
    public void setLoadFavorites(boolean flag) {
        mLoadFavorites = flag;
    }

    public void setProjection(String[] projection) {
        super.setProjection(projection);
        mProjection = projection;
    }

    @Override
    public Cursor loadInBackground() {
        List<Cursor> cursors = Lists.newArrayList();
        if (mLoadFavorites) {
            // SPRD: Bug746238 it should not calculate the count of stared contacts
            Cursor cursor = loadFavoritesContacts();
            if (cursor != null) {
                mFavoriteCount = cursor.getCount();
            }
            cursors.add(cursor);
        }
        final Cursor contactsCursor = loadContacts();
        cursors.add(contactsCursor);
        return new MergeCursor(cursors.toArray(new Cursor[cursors.size()])) {
            @Override
            public Bundle getExtras() {
                // Need to get the extras from the contacts cursor.
                return contactsCursor == null ? new Bundle() : contactsCursor.getExtras();
            }
        };
    }

    private Cursor loadContacts() {
        // ContactsCursor.loadInBackground() can return null; MergeCursor
        // correctly handles null cursors.
        try {
            return super.loadInBackground();

        } catch (NullPointerException | SQLiteException | SecurityException e) {
            // Ignore NPEs, SQLiteExceptions and SecurityExceptions thrown by providers
        }
        return null;
    }

    private Cursor loadFavoritesContacts() {
        final StringBuilder selection = new StringBuilder();
        selection.append(Contacts.STARRED + "=?");
        final ContactListFilter filter =
                ContactListFilterController.getInstance(getContext()).getFilter();
        // UNISOC: Bug1108216 Display contacts only with phone number not apply to favorite contacts
        if (isCustomFilterForPhoneNumbersOnly()) {
            selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
        }
        if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
            selection.append(" AND ").append(Contacts.IN_VISIBLE_GROUP + "=1");
        }
        return getContext().getContentResolver().query(
                Contacts.CONTENT_URI, mProjection, selection.toString(), new String[]{"1"},
                getSortOrder());
    }

    /**
     * UNISOC: Bug1108216 Display contacts only with phone number not apply to favorite contacts
     * @{
     */
    private boolean isCustomFilterForPhoneNumbersOnly() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean(ContactsPreferences.PREF_DISPLAY_ONLY_PHONES,
                ContactsPreferences.PREF_DISPLAY_ONLY_PHONES_DEFAULT);
    }
    /**
     * @}
     */
}
