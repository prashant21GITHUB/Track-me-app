

package com.pyb.trackme.selectMultipleContacts.contact;

import org.greenrobot.eventbus.EventBus;

/**
 * The contact selection has changed.
 *
 * We need to:
 * - recalculate the number of selected contacts
 * - deselect groups if no contact is selected
 *
 * We could just use the regular listener mechanism to propagate changes for checked/un-checked
 * contacts but if the user selects "Check All / Un-check All" this would trigger a call for each
 * contact. Therefore the listener call is suppressed and a ContactSelectionChanged fired once all
 * contacts are checked / un-checked.
 *
 * Publisher: ContactFragment
 * Subscriber: ContactPickerActivity
 */
public class ContactSelectionChanged {

    private static final ContactSelectionChanged sEvent = new ContactSelectionChanged();

    static void post() {
        EventBus.getDefault().post( sEvent );
    }

}
