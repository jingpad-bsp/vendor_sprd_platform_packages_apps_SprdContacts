package com.android.contacts.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.contacts.R;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.model.account.AccountWithDataSet;
import android.telephony.TelephonyManager;
import com.android.contacts.model.account.USimAccountType;
import com.android.contacts.model.account.SimAccountType;
import android.util.Log;

/**
 * An {@link ImageView} designed to display QuickContact's contact photo. When requested to draw
 * {@link LetterTileDrawable}'s, this class instead draws a different default avatar drawable.
 *
 * In addition to supporting {@link ImageView#setColorFilter} this also supports a {@link #setTint}
 * method.
 *
 * This entire class can be deleted once use of LetterTileDrawable is no longer used
 * inside QuickContactsActivity at all.
 */
public class QuickContactImageView extends ImageView {

    private Drawable mOriginalDrawable;
    private Drawable mLocalDrawbale;
    private int mTintColor;
    private boolean mIsBusiness;

    /*
     * SPRD:AndroidN porting add sim icon feature.
     * SPRD: add for bug621379, add for fdn feature bugfix
     * @{
     */
    private AccountWithDataSet mAccount;
    private boolean mIsSdnContact;
    private boolean mIsFdnContact;
    private int mFdnPhoneId;
    protected int[] mSimBitmapRes = {
            R.drawable.ic_person_white_540dp_sim1, R.drawable.ic_person_white_540dp_sim2,
            R.drawable.ic_person_white_540dp_sim3, R.drawable.ic_person_white_540dp_sim4,
            R.drawable.ic_person_white_540dp_sim5
    };
    protected int[] mSimSdnBitmapRes = {
            R.drawable.ic_person_white_540dp_sim1_sdn, R.drawable.ic_person_white_540dp_sim2_sdn,
            R.drawable.ic_person_white_540dp_sim3_sdn, R.drawable.ic_person_white_540dp_sim4_sdn,
            R.drawable.ic_person_white_540dp_sim5_sdn
    };
    protected int[] mSimFdnBitmapRes = {
            R.drawable.ic_person_white_540dp_sim1_fdn, R.drawable.ic_person_white_540dp_sim2_fdn
    };
    /*
     * @}
     */

    public QuickContactImageView(Context context) {
        this(context, null);
    }

    public QuickContactImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickContactImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTint(int color) {
        if (mLocalDrawbale == null || mLocalDrawbale.getAlpha() == (1<<8)-1) {
            setBackgroundColor(color);
        } else {
            setBackground(null);
        }
        mTintColor = color;
        postInvalidate();
    }

    public boolean isBasedOffLetterTile() {
        return mOriginalDrawable instanceof LetterTileDrawable;
    }

    public void setIsBusiness(boolean isBusiness) {
        mIsBusiness = isBusiness;
    }

    /*
     * SPRD:AndroidN porting add sim icon feature.
     * SPRD: add for bug621379, add for fdn feature bugfix
     * @{
     */
    public void setAccount(AccountWithDataSet account) {
        mAccount = account;
    }

    public void setIsSdnContact(boolean isSdnContact) {
        mIsSdnContact = isSdnContact;
    }

    public void setIsFdnContact(boolean isFdnContact) {
        mIsFdnContact = isFdnContact;
    }

    public void setFdnPhoneId(int fdnPhoneId) {
        mFdnPhoneId = fdnPhoneId;
    }
    /*
     * @}
     */

    @Override
    public void setImageDrawable(Drawable drawable) {
        // There is no way to avoid all this casting. Blending modes aren't equally
        // supported for all drawable types.
        Drawable iconDrawable = null;
        boolean isSingleSim = ((TelephonyManager) TelephonyManager.from(mContext)).getPhoneCount() == 1 ? true
                : false;
        if (drawable == null || drawable instanceof BitmapDrawable) {
            iconDrawable = (BitmapDrawable) drawable;
            /** UNISOC: add for bug1195039/1378860, Edit contact profile in landscape mode,abnormal color of the picture. @{ */
            clearColorFilter();
            /** @｝ */
        } else if (drawable instanceof LetterTileDrawable) {
            if (mAccount != null && (mAccount.type.equals(USimAccountType.ACCOUNT_TYPE) ||
                    mAccount.type.equals(SimAccountType.ACCOUNT_TYPE))) {
                String simAccountName = mAccount.name;
                int phoneId = 0;
                /*
                 * SPRD: 863757 SDN contact details page icon is incorrectly displayed
                 * @{
                */

                if (isSingleSim) {
                    if (mIsSdnContact) {
                        iconDrawable = getResources().getDrawable(R.drawable.ic_person_white_540dp_sdn);
                    } else {
                        iconDrawable = getResources().getDrawable(R.drawable.ic_person_white_540dp_sim);
                    }
                } else {
                    phoneId = Integer.parseInt(simAccountName.substring(3))-1;
                    if (mIsSdnContact) {
                        iconDrawable = getResources().getDrawable(mSimSdnBitmapRes[phoneId]);
                    } else {
                        iconDrawable = getResources().getDrawable(mSimBitmapRes[phoneId]);
                    }
                }

                /* @} */

            } else if (!mIsBusiness) {
            /*
             * @}
             */
                /*
                 * SPRD: add for bug621379, add for fdn feature bugfix
                 * SPRD: 861226 FDN contact details page icon is incorrectly displayed
                 * @{
                 */
                    if (mIsFdnContact) {
                        if (isSingleSim){
                            iconDrawable = getResources().getDrawable(R.drawable.ic_person_white_540dp_fdn);
                        }else {
                            iconDrawable = getResources().getDrawable(mSimFdnBitmapRes[mFdnPhoneId]);
                        }
                    } else {
                        iconDrawable = getResources().getDrawable(R.drawable.ic_person_white_540dp);
                    }
                /*
                 * @}
                 */
            } else {
                iconDrawable = getResources().getDrawable(
                        R.drawable.ic_generic_business_white_540dp);
            }
        } else {
            throw new IllegalArgumentException("Does not support this type of drawable");
        }

        mOriginalDrawable = drawable;
        mLocalDrawbale = iconDrawable;
        setTint(mTintColor);
        super.setImageDrawable(mLocalDrawbale);
    }

    @Override
    public Drawable getDrawable() {
        return mOriginalDrawable;
    }
}
