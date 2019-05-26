

package com.pyb.trackme.selectMultipleContacts.core;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.pyb.trackme.selectMultipleContacts.contact.ContactDescription;
import com.pyb.trackme.selectMultipleContacts.contact.ContactFragment;
import com.pyb.trackme.selectMultipleContacts.contact.ContactSortOrder;
import com.pyb.trackme.selectMultipleContacts.group.GroupFragment;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureType;

public class PagerAdapter extends FragmentStatePagerAdapter {

    final private int mNumOfTabs;

    final private ContactSortOrder mSortOrder;
    final private ContactPictureType mBadgeType;
    final private ContactDescription mDescription;
    final private int mDescriptionType;

    public PagerAdapter(FragmentManager fm, int numOfTabs, ContactSortOrder sortOrder,
                        ContactPictureType badgeType, ContactDescription description, int descriptionType) {
        super(fm);

        mNumOfTabs = numOfTabs;
        mSortOrder = sortOrder;
        mBadgeType = badgeType;
        mDescription = description;
        mDescriptionType = descriptionType;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return ContactFragment.newInstance(mSortOrder, mBadgeType, mDescription, mDescriptionType);
            case 1:
                return GroupFragment.newInstance();
            default: return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

}
