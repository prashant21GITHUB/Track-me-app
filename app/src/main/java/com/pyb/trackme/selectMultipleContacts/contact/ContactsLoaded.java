

package com.pyb.trackme.selectMultipleContacts.contact;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * All contacts have been loaded (including details).
 *
 * Publisher: ContactPickerActivity
 * Subscriber: ContactFragment
 */
public class ContactsLoaded {

    public static void post(List<? extends Contact> contacts) {
        ContactsLoaded event = new ContactsLoaded(contacts);
        EventBus.getDefault().postSticky(event);
    }

    final private List<? extends Contact> mContacts;

    private ContactsLoaded(List<? extends Contact> contacts) {
        mContacts = contacts;
    }

    public List<? extends Contact> getContacts() {
        return mContacts;
    }

}
