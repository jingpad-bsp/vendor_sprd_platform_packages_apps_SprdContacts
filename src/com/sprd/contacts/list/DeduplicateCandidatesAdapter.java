package com.sprd.contacts.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.ContactPhotoManager;
import com.sprd.contacts.DeduplicationCandidate;
import com.android.contacts.R;

public class DeduplicateCandidatesAdapter extends
        GroupCheckAdapter<DeduplicationCandidate> {

    ContactPhotoManager mPhotoManager;

    public DeduplicateCandidatesAdapter(Context context) {
        super(context);
        mPhotoManager = ContactPhotoManager.getInstance(context);
    }

    @Override
    protected View newGroupItem(ViewGroup parent, DeduplicationCandidate item,
            LayoutInflater inflater) {
        View candidateView = inflater.inflate(R.layout.candidate_item_layout,
                parent, false);
        TextView nameText = (TextView) candidateView
                .findViewById(R.id.candidate_name);
        TextView numberText = (TextView) candidateView
                .findViewById(R.id.candidate_number);
        ImageView photo = (ImageView) candidateView
                .findViewById(R.id.candidate_photo);
        nameText.setText(item.mName);
        numberText.setText(item.mNumber);
        /**
         * SPRD:bug 720583 Forced left display.
         */
        numberText.setTextDirection(View.TEXT_DIRECTION_LTR);
        /**
         * @}
         */
        // accountNameText.setVisibility(isFirst ? View.VISIBLE : View.GONE);
        if (item.mPhotoId <= 0) {
            mPhotoManager.loadPhoto(photo, null, -1, false, true, null);
        } else {
            mPhotoManager.loadThumbnail(photo, item.mPhotoId, false, true, null);
        }

        return candidateView;
    }

    public class RawContactWitAccount {
        public long id;
        public long accountId;

        /*UNISOC:Bug1395127, photo is disappeared @{ */
        public long photoId;
        /* @} */

        public RawContactWitAccount(long id, long accountId, long photoId) {
            this.id = id;
            this.accountId = accountId;

            /*UNISOC:Bug1395127, photo is disappeared @{ */
            this.photoId = photoId;
            /* @} */

        }

    }

    @Override
    protected RawContactWitAccount getSubItem(DeduplicationCandidate t) {
        return new RawContactWitAccount(t.mRawContactId, t.mAccountId, t.mPhotoId);
    }

    @Override
    protected void setUpHeaderText(TextView text, DeduplicationCandidate item) {
        text.setText(item.mAccountName);
        text.setBackgroundResource(R.drawable.list_section_divider_holo_light_sprd);
    }

}
