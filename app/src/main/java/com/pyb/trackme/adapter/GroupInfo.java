package com.pyb.trackme.adapter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GroupInfo {

    private final Set<String> contacts;
    private final String groupAdmin;
    private final String groupName;
    private final int ownerIndex;

    public GroupInfo(Collection<String> contacts, String groupAdmin, String groupName, int ownerIndex) {
        this.contacts = new HashSet<>(contacts);
        this.groupAdmin = groupAdmin;
        this.groupName = groupName;
        this.ownerIndex = ownerIndex;
    }

    public Collection<String> getContacts() {
        return contacts;
    }

    public boolean deleteContact(String contactToDelete, String admin) {
        if(this.groupAdmin.equals(admin)) {
            contacts.remove(contactToDelete);
            return true;
        }
        return false;
    }

    public boolean addContact(String contactToAdd, String admin) {
        if(this.groupAdmin.equals(admin)) {
            contacts.remove(contactToAdd);
            return true;
        }
        return false;
    }

    public String getGroupAdmin() {
        return groupAdmin;
    }

    public int getOwnerIndex() {
        return ownerIndex;
    }
}
