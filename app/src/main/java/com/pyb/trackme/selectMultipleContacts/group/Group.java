

package com.pyb.trackme.selectMultipleContacts.group;

import com.pyb.trackme.selectMultipleContacts.ContactElement;
import com.pyb.trackme.selectMultipleContacts.contact.Contact;

import java.util.Collection;

/**
 * This interface describes a group contact.
 * It only provides read methods to make sure no class outside this package can modify it.
 * Write access is only possible through the GroupImpl class which has package access.
 */
public interface Group extends ContactElement {

    Collection<Contact> getContacts();

}
