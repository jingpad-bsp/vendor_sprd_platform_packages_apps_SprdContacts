package com.sprd.contacts.util;

import com.android.internal.telephony.TelePhonebookUtils;

//for feature control
public class Features{

    public static final boolean supportVideoCallIcon(){
        return true;
    }

    public static final boolean supportFastScrollBar(){
        return true;
    }

    public static final boolean supportEFDisplayFeature(){
        return supportOrange();
    }

    public static final boolean supportOrange(){
        return TelePhonebookUtils.isSupportOrange();
    }
}
