

package com.pyb.trackme.selectMultipleContacts.core;

import android.database.Cursor;
import android.provider.ContactsContract;

import com.pyb.trackme.selectMultipleContacts.contact.Contact;
import com.pyb.trackme.selectMultipleContacts.group.Group;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * GroupImpl is the concrete GroupInfo implementation.
 * It can be instantiated and modified only within its own package to prevent modifications from
 * classes outside the package.
 */
class GroupImpl extends ContactElementImpl implements Group {

    static GroupImpl fromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Groups._ID));
        String title = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
        return new GroupImpl(id, title);
    }

    private Map<Long, Contact> mContacts = new HashMap<>();

    private GroupImpl(long id, String displayName) {
        super(id, displayName);
    }

    @Override
    public Collection<Contact> getContacts() {
        return mContacts.values();
    }

    void addContact(Contact contact) {
        long contactId = contact.getId();
        if (!mContacts.keySet().contains(contactId)) {
            mContacts.put(contact.getId(), contact);
        }
    }

    boolean hasContacts() {
        return mContacts.size() > 0;
    }

}
