

package com.pyb.trackme.selectMultipleContacts.contact;

import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.selectMultipleContacts.Helper;
import com.pyb.trackme.selectMultipleContacts.picture.ContactBadge;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureManager;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureType;

public class ContactViewHolder extends RecyclerView.ViewHolder {

    private View mRoot;
    private TextView mName;
    private TextView mDescription;
    private ContactBadge mBadge;
    private CheckBox mSelect;

    final private ContactPictureType mContactPictureType;
    final private ContactDescription mContactDescription;
    final private int mContactDescriptionType;
    final private ContactPictureManager mContactPictureLoader;

    ContactViewHolder(View root, ContactPictureManager contactPictureLoader, ContactPictureType contactPictureType,
                      ContactDescription contactDescription, int contactDescriptionType) {
        super(root);

        mRoot = root;
        mName = (TextView) root.findViewById(R.id.name);
        mDescription = (TextView) root.findViewById(R.id.description);
        mBadge = (ContactBadge) root.findViewById(R.id.contact_badge);
        mSelect = (CheckBox) root.findViewById(R.id.select);

        mContactPictureType = contactPictureType;
        mContactDescription = contactDescription;
        mContactDescriptionType = contactDescriptionType;
        mContactPictureLoader = contactPictureLoader;

        mBadge.setBadgeType(mContactPictureType);
    }

    void bind(final Contact contact) {
        mRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelect.toggle();
            }
        });

        // main text / title
        mName.setText(contact.getDisplayName());

        // description
        String description = "";
        switch (mContactDescription) {
            case EMAIL:
                description = contact.getEmail(mContactDescriptionType);
                break;
            case PHONE:
                description = contact.getPhone(mContactDescriptionType);
                break;
            case ADDRESS:
                description = contact.getAddress(mContactDescriptionType);
                break;
        }
        mDescription.setText(description);
        mDescription.setVisibility( Helper.isNullOrEmpty(description) ? View.GONE : View.VISIBLE );

        // contact picture
        if (mContactPictureType == ContactPictureType.NONE) {
            mBadge.setVisibility(View.GONE);
        }
        else {
            mContactPictureLoader.loadContactPicture(contact, mBadge);
            mBadge.setVisibility(View.VISIBLE);

            String lookupKey = contact.getLookupKey();
            if (lookupKey != null) {
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                mBadge.assignContactUri(contactUri);
            }
        }

        // check box
        mSelect.setOnCheckedChangeListener(null);
        mSelect.setChecked( contact.isChecked() );
        mSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                contact.setChecked(isChecked, false);
            }
        });
    }

    void onRecycled() {
        mBadge.onDestroy();
    }

}
