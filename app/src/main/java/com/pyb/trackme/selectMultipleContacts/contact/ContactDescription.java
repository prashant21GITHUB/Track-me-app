

package com.pyb.trackme.selectMultipleContacts.contact;

import android.util.Log;

public enum ContactDescription {
    PHONE,
    EMAIL,
    ADDRESS;

    public static ContactDescription lookup(String name) {
        try {
            return ContactDescription.valueOf(name);
        }
        catch (IllegalArgumentException ignore) {
            Log.e(ContactDescription.class.getSimpleName(), ignore.getMessage());
            return ADDRESS;
        }
    }

}
