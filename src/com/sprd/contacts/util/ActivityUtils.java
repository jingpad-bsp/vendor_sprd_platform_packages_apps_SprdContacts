package com.sprd.contacts.util;

import android.app.Activity;
import java.util.Map;
import java.util.HashMap;
import java.util.*;
import android.util.Log;

public class ActivityUtils {

    private static ActivityUtils mActivityUtils = null;

    private LinkedList<Activity> mActivityQueue = new LinkedList<Activity>();

    private ActivityUtils() {
    }

    public synchronized static ActivityUtils getInstance() {
        if (mActivityUtils == null) {
            mActivityUtils = new ActivityUtils();
        }
        return mActivityUtils;
    }

    public Activity removeActivity() {
        Activity activity = null;
        if (mActivityQueue != null) {
           mActivityQueue.remove();
        }
        return activity;
    }

    public Activity getTopActivity() {
        Activity activity = null;
        if (mActivityQueue != null) {
           activity = mActivityQueue.peek();
        }
        return activity;
    }

    public boolean activityHasStart(Activity activity) {
        boolean flag = false;
        for(int i = 0; i < mActivityQueue.size(); i++){
             if(mActivityQueue.get(i).getLocalClassName()
                  .equals(activity.getLocalClassName())){
                 flag = true;
                 break;
             }
        }
        return flag;
    }

    public void addActivityToQueue(Activity activity) {
        if (mActivityQueue != null) {
            mActivityQueue.add(activity);
        }
    }
}