

package com.pyb.trackme.selectMultipleContacts.picture;

import android.graphics.Bitmap;

import org.greenrobot.eventbus.EventBus;

/**
 * This event is sent from the ContactPictureLoader to the ContactPictureManager.
 * The latter will then set the ContactBadge's contact picture (if the keys match).
 */
public class ContactPictureLoaded {

    private final String mKey;
    private final ContactBadge mBadge;
    private final Bitmap mBitmap;

    static void post(String key, ContactBadge badge, Bitmap bitmap) {
        ContactPictureLoaded event = new ContactPictureLoaded(key, badge, bitmap);
        EventBus.getDefault().post(event);
    }

    private ContactPictureLoaded(String key, ContactBadge badge, Bitmap bitmap) {
        mKey = key;
        mBadge = badge;
        mBitmap = bitmap;
    }

    ContactBadge getBadge() {
        return mBadge;
    }

    String getKey() {
        return mKey;
    }

    Bitmap getBitmap() {
        return mBitmap;
    }

}
