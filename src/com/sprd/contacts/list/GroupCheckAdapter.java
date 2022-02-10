package com.sprd.contacts.list;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.R;
import com.sprd.contacts.DeduplicationCandidate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class GroupCheckAdapter<T> extends BaseAdapter {

    ArrayList<ArrayList<T>> mArray = new ArrayList<ArrayList<T>>();

    SparseArray<ArrayList<Object>> mCheckedItems = new SparseArray<ArrayList<Object>>();
    Context mContext;

    public GroupCheckAdapter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public int getCount() {
        if (mArray != null) {
            return mArray.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (mArray != null && position < mArray.size()) {
            return mArray.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {

        if (mArray != null && position < mArray.size()) {
            return position;
        } else {
            return 0;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        List<T> items = mArray.get(position);
        final LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View groupCheckView;
        LinearLayout itemContainer;
        if (convertView != null) {
            groupCheckView = convertView;
        } else {
            groupCheckView = inflater.inflate(R.layout.group_check_item_layout, parent, false);
        }
        itemContainer = (LinearLayout) groupCheckView.findViewById(R.id.item_container);
        itemContainer.removeAllViews();
        int i = 0;
        TextView headerText = (TextView) groupCheckView.findViewById(R.id.group_check_header);
        for (T item : items) {
            View itemView = newGroupItem(parent, item, inflater);
            itemContainer.addView(itemView);
            if (i == 0) {
                setUpHeaderText(headerText, item);
                i++;
            }

        }

        CheckBox checkBox = (CheckBox) groupCheckView.findViewById(R.id.group_check);
        checkBox.setChecked(isChecked(position));

        return groupCheckView;
    }

    public void changeDataSource(ArrayList<ArrayList<T>> array) {
        //SPRD: add for bug956106，checkbox will be clear when gmail sync contacts
        if (array.size() == mArray.size()) {
            return;
        }
        mCheckedItems.clear();
        mArray = array;
        notifyDataSetChanged();
    }

    public void checkAll(boolean isCheck) {
        if (isCheck) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                ArrayList<T> itemGroup = (ArrayList<T>) getItem(i);
                ArrayList<Object> subItemGroup = new ArrayList<Object>();
                for (T t : itemGroup) {
                    subItemGroup.add(getSubItem(t));
                }
                mCheckedItems.put(i, subItemGroup);
            }
        } else {
            mCheckedItems.clear();
        }
    }

    public boolean isAllCheckd() {
        int count = getCount();
        for (int i = 0; i < count; i++) {
            if (!isChecked(i))
                return false;
        }
        return true;
    }

    public SparseArray<ArrayList<Object>> getCheckedItems() {
        return mCheckedItems;
    }

    public void setChecked(int position, boolean isChecked) {
        if (position < getCount()) {
            if (isChecked) {
                ArrayList<T> itemGroup = (ArrayList<T>) getItem(position);
                ArrayList<Object> subIdGroup = new ArrayList<Object>();
                for (T t : itemGroup) {
                    subIdGroup.add(getSubItem(t));
                }
                mCheckedItems.put(position, subIdGroup);
            } else {
                mCheckedItems.remove(position);
            }
        }
    }

    public boolean isChecked(int position) {
        return mCheckedItems.indexOfKey(position) >= 0;
    }

    // bug 370284 begin
    public void setAllCheckedItem(int[] allCheckedItem) {
        if (allCheckedItem != null) {
            for (int i : allCheckedItem) {
                ArrayList<T> itemGroup = (ArrayList<T>) getItem(i);
                ArrayList<Object> subIdGroup = new ArrayList<Object>();
                if (itemGroup != null) {
                    for (T t : itemGroup) {
                        subIdGroup.add(getSubItem(t));
                    }
                }
                mCheckedItems.put(i, subIdGroup);
            }
        }
    }
    // bug 370284 end

    protected abstract View newGroupItem(ViewGroup parent, T item, LayoutInflater inflater);

    protected abstract Object getSubItem(T t);

    protected abstract void setUpHeaderText(TextView text, T t);
}