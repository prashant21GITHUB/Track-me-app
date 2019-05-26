

package com.pyb.trackme.selectMultipleContacts.picture;

import android.util.Log;

public enum ContactPictureType {
    NONE,
    ROUND,
    SQUARE;


    public static ContactPictureType lookup(String name) {
        try {
            return ContactPictureType.valueOf(name);
        }
        catch (IllegalArgumentException ignore) {
            Log.e(ContactPictureType.class.getSimpleName(), ignore.getMessage());
            return ROUND;
        }
    }

}
