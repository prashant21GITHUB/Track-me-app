

package com.pyb.trackme.selectMultipleContacts.picture;

import android.provider.ContactsContract;

/**
 * Some constans used in the ContactBadge and the ContactQueryHandler.
 */
class Constants {

    static final int TOKEN_EMAIL_LOOKUP = 0;
    static final int TOKEN_PHONE_LOOKUP = 1;
    static final int TOKEN_EMAIL_LOOKUP_AND_TRIGGER = 2;
    static final int TOKEN_PHONE_LOOKUP_AND_TRIGGER = 3;

    static final String EXTRA_URI_CONTENT = "uri_content";

    static final String[] EMAIL_LOOKUP_PROJECTION = new String[]{
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.Contacts.LOOKUP_KEY,
    };
    static final int EMAIL_ID_COLUMN_INDEX = 0;
    static final int EMAIL_LOOKUP_STRING_COLUMN_INDEX = 1;

    static final String[] PHONE_LOOKUP_PROJECTION = new String[]{
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
    };
    static final int PHONE_ID_COLUMN_INDEX = 0;
    static final int PHONE_LOOKUP_STRING_COLUMN_INDEX = 1;

}
