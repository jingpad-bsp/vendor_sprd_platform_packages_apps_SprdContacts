package com.sprd.contacts.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import android.util.Log;

/**
 * SPRD:Bug 680321 for speedup contactslist flush when scroll
 */
public class ActiveDataManager {

    private static ActiveDataManager self = null;
    private static final long CACHE_TIME_OUT = 3000;
    private Date cacheTime;
    private Map<String,Object> mMap;

    public static ActiveDataManager self(){
        if(self == null){
            self = new ActiveDataManager();
        }
        return self;
    }

    private ActiveDataManager(){
        mMap = new HashMap<String, Object>();
        cacheTime = new Date();
    }

    public void setUrl(String url,Object obj){
        mMap.put(url,obj);
    }

    public Object getObj(String url){
        if(mMap.containsKey(url)){
            return mMap.get(url);
        }
        return null;
    }

    public boolean isExist(String url){
        long timeout = (new Date()).getTime()-self.cacheTime.getTime();

        if(timeout < 0 || timeout > CACHE_TIME_OUT){
            self.clear();
        }

        if(mMap.containsKey(url)){
            return true;
        }

        return false;
    }

    public void clear(){
        mMap.clear();
        cacheTime = new Date();
    }

}
