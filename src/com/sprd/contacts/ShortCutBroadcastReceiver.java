package com.sprd.contacts;

import android.content.BroadcastReceiver;
import com.android.contacts.quickcontact.QuickContactActivity;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;


public class ShortCutBroadcastReceiver extends BroadcastReceiver{

    private static QuickContactActivity mQuickContactActivity;

    public static void setContext(QuickContactActivity activity){
        mQuickContactActivity = activity;
    }

    public void onReceive(Context context,Intent intent){
        String action = intent.getAction();
        /** UNISOC: add for bug 933389 @{ */
        if (!TextUtils.isEmpty(action) && action.equals("com.android.contacts.quickContactsShortcutPinned")){
        /** @} */
            String stringExtra = intent.getStringExtra("shortcutName");
            Context context2 = this.mQuickContactActivity;
            if(context2 == null){
                 return ;
            }
            mQuickContactActivity.makeShortcutToast(stringExtra,1);
        }
    }
}
