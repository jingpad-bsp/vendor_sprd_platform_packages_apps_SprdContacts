package com.sprd.contacts.activities;

import android.R.anim;
import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.R;
import com.sprd.contacts.DeduplicationCandidate;
import com.sprd.contacts.list.GroupCheckAdapter;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class GroupCheckActivity<T> extends Activity implements OnItemClickListener {

    public static final int REQUEST_QUERY_ITEMS = 1;
    public static final int RESPONSE_QUERY_ITEMS = 50;
    public static final int REQUEST_EXTEND_BASE = 100;
    public static final int RESPONSE_EXTEND_BASE = 150;
    private static final String KEY_CHECKED_ITEM_POSITIONS = "key_checked_item_postions"; //bug370284

    GroupCheckAdapter<T> mAdapter;
    DataAccessHander mDataAccessHander;
    HandlerThread mDataAccessThread;
    ListView mList;
    TextView mLoadingText;
    TextView mEmpty;// bug 370543

    int mDoneResId;
    int mCheckResId;
    int mUncheckResId;
    int mTittleId;
    int mEmptyTextResId;// bug 370543
    int mCheckedItems[];// bug 370284

    private Menu mMenu = null;
    // SPRD: Bug713384 it flash merge button and menu when no merged contacts
    private Activity mActivity;

    // SPRD: Bug725171 hide the clearMenu when there is no checked item
    private MenuItem clearMenuItem;

    Handler mHander = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == RESPONSE_QUERY_ITEMS) {
                ArrayList<ArrayList<T>> itemGroupArray = (ArrayList<ArrayList<T>>) msg.obj;
                mAdapter.changeDataSource(itemGroupArray);
                // bug 370543 begin
                if (itemGroupArray == null || itemGroupArray.size() == 0) {
                    mList.setVisibility(View.GONE);
                    // SPRD: Bug713384 it flash merge button and menu when no merged contacts
                    if (mActivity != null) {
                        mActivity.invalidateOptionsMenu();
                    }
                    showEmptyText(mEmptyTextResId);
                } else {
                    // SPRD: Bug713384 it flash merge button and menu when no merged contacts
                    if (mActivity != null) {
                        mActivity.invalidateOptionsMenu();
                    }
                    showLoadingView(false);
                    mList.setVisibility(View.VISIBLE);
                    // bug 370284 begin
                    if (mCheckedItems != null) {
                        mAdapter.setAllCheckedItem(mCheckedItems);
                        boolean isAllChecked = mAdapter.isAllCheckd();
                        boolean isAllUnchecked = mAdapter.getCheckedItems().size() == 0;
                        mCheckedItems = null;
                    }
                    // bug 370284 end
                }
                // bug 370543 end
            } else {
                handleResponse(msg.what);
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_check_activity_layout);
        // SPRD: Bug713384 it flash merge button and menu when no merged contacts
        mActivity = this;
        mAdapter = createListAdapter();
        mList = (ListView) findViewById(android.R.id.list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mLoadingText = (TextView) findViewById(android.R.id.empty);
        showLoadingView(true);
        mDataAccessThread = new HandlerThread("DataAccess") {
            @Override
            protected void onLooperPrepared() {
                mDataAccessHander = new DataAccessHander(mDataAccessThread.getLooper());
                onDataAccessReady();
            }
        };
        mDoneResId = getDoneResId();
        mCheckResId = getCheckResId();
        mUncheckResId = getUncheckResId();
        mEmptyTextResId = getEmptyResId();// bug 370543
        mDataAccessThread.start();

        // bug 370284 begin
        mCheckedItems = null;
        if (savedInstanceState != null) {
            mCheckedItems = savedInstanceState.getIntArray(KEY_CHECKED_ITEM_POSITIONS);
        }
        // bug 370284 end
    }

    class DataAccessHander extends Handler {

        public DataAccessHander(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REQUEST_QUERY_ITEMS) {
                ArrayList<ArrayList<T>> array = getItems();
                onDataAcquired(array);
            } else {
                handleRequest(msg.what, msg.obj);
            }
        }
    }

    protected final void requestDataAccessDelay(int what, Object obj, int delay) {
        if (mDataAccessHander != null) {
            mDataAccessHander.sendMessageDelayed(mDataAccessHander.obtainMessage(what, obj), delay);
        }
    }

    protected final void responseForDataAccess(int what) {
        mHander.sendEmptyMessage(what);
    }

    protected final void responseForDataAccessDelay(int what, int delay) {
        mHander.sendEmptyMessageDelayed(what, delay);
    }

    protected int getDoneResId() {
        return R.string.menu_done;
    }

    protected int getCheckResId() {
        return R.string.menu_select_all;
    }

    protected int getUncheckResId() {
        return R.string.menu_select_none;
    }

    // bug 370543 begin
    protected int getEmptyResId() {
        return R.string.no_duplicate_contact;
    }

    // bug 370543 end

    protected GroupCheckAdapter<T> getAdapter() {
        return mAdapter;
    }

    protected void handleRequest(int what, Object obj) {

    }

    protected void handleResponse(int what) {

    }

    protected void onDataAccessReady() {

    }

    public void showLoadingView(boolean isVisiable) {
        mLoadingText.setVisibility(isVisiable ? View.VISIBLE : View.GONE);
    }

    // bug 370543 begin
    public void showEmptyText(int resId) {
        mLoadingText.setVisibility(View.VISIBLE);
        mLoadingText.setText(resId);
    }

    // bug 370543 end

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        boolean isChecked = mAdapter.isChecked(position);
        mAdapter.setChecked(position, !isChecked);
        // bug 369275 begin
        boolean isAllChecked = mAdapter.isAllCheckd();
        boolean isAllUnchecked = mAdapter.getCheckedItems().size() == 0;
        // bug 369275 end
        // SPRD: Bug725171 hide the clearMenu when there is no checked item
        refreshClearMenuItem();
        mAdapter.notifyDataSetChanged();
    }

    private void onDataAcquired(ArrayList<ArrayList<T>> itemGroupArray) {
        mHander.sendMessage(mHander.obtainMessage(RESPONSE_QUERY_ITEMS, itemGroupArray));
    }

    protected final void requestDataAccess(int what, Object obj) {
        if (mDataAccessHander != null) {
            mDataAccessHander.sendMessage(mDataAccessHander.obtainMessage(what, obj));
        }
    }

    private void doCheckAll() {
        boolean isAllChecked = mAdapter.isAllCheckd();
        mAdapter.checkAll(!isAllChecked);
        // SPRD: Bug725171 hide the clearMenu when there is no checked item
        refreshClearMenuItem();
        mAdapter.notifyDataSetChanged();
    }

    private void doCancelAll(){
        boolean isAllChecked = mAdapter.isAllCheckd();
        mAdapter.checkAll(!isAllChecked);
        // SPRD: Bug725171 hide the clearMenu when there is no checked item
        refreshClearMenuItem();
        mAdapter.notifyDataSetChanged();
    }

    // bug 370284 begin
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        int size = mAdapter.getCheckedItems().size();
        int posiTions[] = new int[size];
        for (int i = 0; i < size; i++) {
            posiTions[i] = mAdapter.getCheckedItems().keyAt(i);
        }
        outState.putIntArray(KEY_CHECKED_ITEM_POSITIONS, posiTions);
    }

    // bug 370284 end

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clear_view_contact, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        clearMenuItem = menu.findItem(R.id.menu_clear);
        final MenuItem doCheckAllMenuItem = menu.findItem(R.id.menu_contacts_select_all);
        final MenuItem doCancelAllMenuItem = menu.findItem(R.id.menu_contacts_cancel_all);
        // SPRD: Bug713384 it flash merge button and menu when no merged contacts
        if(mAdapter != null && mAdapter.getCount() > 0){
            // SPRD: Bug725171 hide the clearMenu when there is no checked item
            refreshClearMenuItem();
            boolean flag = mAdapter.isAllCheckd();
            doCheckAllMenuItem.setVisible(!flag);
            doCancelAllMenuItem.setVisible(flag);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                onDonePressed(mAdapter.getCheckedItems());
                return true;
            case R.id.menu_contacts_select_all:
                doCheckAll();
                return true;
            case R.id.menu_contacts_cancel_all:
                doCancelAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    // SPRD: Bug725171 hide the clearMenu when there is no checked item
    public void refreshClearMenuItem() {
        if (clearMenuItem != null) {
            if (mAdapter.getCheckedItems().size() > 0) {
                clearMenuItem.setVisible(true);
            } else {
                clearMenuItem.setVisible(false);
            }
        }
    }

    protected abstract GroupCheckAdapter<T> createListAdapter();

    protected abstract ArrayList<ArrayList<T>> getItems();

    protected abstract void onDonePressed(SparseArray<ArrayList<Object>> sparseArray);

}