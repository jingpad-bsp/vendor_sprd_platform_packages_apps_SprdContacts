package com.sprd.contacts.editor;

import android.content.Context;
import android.content.DialogInterface;
import android.app.DialogFragment;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.R;
import com.sprd.contacts.util.AccountRestrictionUtils;
import com.android.contacts.model.account.SimAccountType;
import com.android.contacts.model.account.USimAccountType;
import com.android.contacts.model.account.PhoneAccountType;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.List;

import com.android.contacts.model.Contact;
import com.android.contacts.model.ContactLoader;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.account.AccountType.EditType;



public class ConfirmReplaceListDialogFragment extends DialogFragment {
    //Bug440418,597225 Save the selected sim number on replaceDialog  when rotating the screen.
    public static final String KEY_DATA_LIST = "dataList";
    public static final String KEY_DATA_ID_LIST = "dataIdList";
    private static final int SIM_PHONE_NUM_MAX_SIZE = 2;
    private static final String TAG = "ConfirmReplaceListDialogFragment";
    public static final int ID_CONFIRM_REPLACE_DIALOG_LIST = 2;

    private Long mSelectDataId ;
    private static int mSelectDataItem = 0;
    private Contact mContact;

    public static boolean isInSimOrUsimAcount(Context context, Contact contact){
        AccountWithDataSet account = contact.getAccount();
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(context);
        AccountType accountType = accountTypeManager.getAccountTypeForAccount(account);

        if(accountType != null
                && !SimAccountType.ACCOUNT_TYPE.equals(accountType.accountType)
                && !USimAccountType.ACCOUNT_TYPE.equals(accountType.accountType)){
            return false;
        }else{
            return true;
        }
    }

    public static int getPhoneCount(Context context,
                                    Contact contact ,
                                    List<String> existedDataIdList,
                                    List<String> existedDataList,
                                    String replacePhoneNum){
        RawContactDeltaList entityDeltaList = contact.createRawContactDeltaList();
        if(replacePhoneNum == null){//if not add phonenum from other app
            return 0;
        }

        if (!isInSimOrUsimAcount(context, contact)) {//if not sim/usim do insert
            return 0;
        }

        if (entityDeltaList == null) {
            return 0;
        }

        for (RawContactDelta entityDelta : entityDeltaList) {

            final RawContactDelta entity = entityDelta;
            final ValuesDelta values = entity.getValues();
            if (values == null || !values.isVisible()) {
                continue;
            }

            String strValue = null;
            for (String mimetype : entity.getMimeTypes()) {
                if (mimetype == null || !mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
                    continue;
                }
                for (ValuesDelta child : entity.getMimeEntries(mimetype)) {
                    if (child.containsKey("_id") && child.containsKey("data1")) {
                        existedDataList.add(child.getAsString("data1"));
                        existedDataIdList.add(child.getAsString("_id"));
                    } else {
                        continue;
                    }
                }
            }

            if (existedDataList.size() == SIM_PHONE_NUM_MAX_SIZE) {
                return ID_CONFIRM_REPLACE_DIALOG_LIST;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public static int getTypeOverallMaxForAccount(AccountType accountType, String mimeType) {
        DataKind dataKind = accountType.getKindForMimetype(mimeType);
        int typeOverallMax = -2;
        if (dataKind != null) {
            typeOverallMax = dataKind.typeOverallMax;
        }
        if ((typeOverallMax == -1) && (dataKind.typeList != null)) {
            int max = 0;
            int size = dataKind.typeList.size();
            for (int i = 0; i < size; i++) {
                EditType editType = dataKind.typeList.get(i);
                if (editType != null && editType.specificMax != -1) {
                    max = max + editType.specificMax;
                }
            }
            if (max != 0) {
                typeOverallMax = max;
            }
        }
        return typeOverallMax;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ArrayList<String> dataList = getArguments().getStringArrayList(KEY_DATA_LIST);
        ArrayList<String> dataIdList = getArguments().getStringArrayList(KEY_DATA_ID_LIST);
        String[] dataStrings = (String[]) dataList
                .toArray(new String[dataList.size()]);
        final String[] dataIds = (String[]) dataIdList.toArray(new String[dataIdList.size()]);
        /**
         * Bug440418,597225 Save the selected sim number on replaceDialog  when rotating the screen.
         * SPRD:Bug608343 Replace sim contact number from call log, can not save the info at the second time.
         * @{
         */
        if (dataIds.length > 0) {
            /**
             * SPRD:Bug610452 remember selected station and restore the
             * station when split-screen
             * @{
             */
            mSelectDataId = mSelectDataId == null ? Long.valueOf(dataIds[mSelectDataItem]) : mSelectDataId;
            /**
             * @}
             */
        }
        /**
         * @}
         */
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.replaceSelected)
                .setSingleChoiceItems(dataStrings, mSelectDataItem,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which >= 0) {
                                    mSelectDataItem = which;
                                    mSelectDataId = Long.valueOf(dataIds[which]);
                                }
                            }
                        })
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                /**
                                 * SPRD:Bug613963 it occured crash when click confirm button
                                 * twice under split-screen
                                 * SPRD:Bug856527 mContact is null leading to NPE
                                 * @{
                                 */
                                if(mSelectDataId == null){
                                    if (getActivity() != null) {
                                        Toast.makeText(getActivity(),R.string.replace_data_error,
                                                Toast.LENGTH_LONG).show();
                                        getActivity().finish();
                                    }
                                }else{
                                    ContactEditorFragment targetFragment =
                                            (ContactEditorFragment) getTargetFragment();
                                    if(targetFragment != null && mContact != null){
                                        if(targetFragment.mIntentExtras == null) {
                                            targetFragment.mIntentExtras = new Bundle();
                                        }
                                        targetFragment.mIntentExtras.putLong("replaceDataId",
                                                mSelectDataId);
                                        targetFragment.setStateForExistingContactSprd(false,
                                                mContact.getRawContacts());
                                    }
                                }
                                /**
                                 * @}
                                 */
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }
                            }
                        }).create();
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isConfirming = true;
        setRetainInstance(true);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    public void setContact(Contact contact){
        mContact = contact;
    }

    public static boolean isConfirming = false;

    @Override
    public void onDestroy(){
        super.onDestroy();
        isConfirming = false;

    }

    public static void resetSelectDataItem(){
        mSelectDataItem = 0;
    }


}
