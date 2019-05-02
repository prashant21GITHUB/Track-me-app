package com.pyb.trackme.db;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TrackDetailsDB {

    private static TrackDetailsDB INSTANCE;

    private Set<String> sharingContactsList;
    private Set<String> trackingContactsList;

    private TrackDetailsDB() {
        sharingContactsList = new LinkedHashSet<>();
        trackingContactsList = new LinkedHashSet<>();
    }

    public void addContactsToShareLocation(Collection<String> contacts) {
        sharingContactsList.addAll(contacts);
    }

    public void addContactToShareLocation(String contact) {
        sharingContactsList.add(contact);
    }

    public void addContactsToTrackLocation(Collection<String> contacts) {
        trackingContactsList.addAll(contacts);
    }

    public void clear() {
        sharingContactsList.clear();
        trackingContactsList.clear();
    }

    public static TrackDetailsDB db() {
        if(INSTANCE == null) {
            INSTANCE = new TrackDetailsDB();
        }
        return INSTANCE;
    }

    public Set<String> getContactsToShareLocation() {
        return Collections.unmodifiableSet(sharingContactsList);
    }

    public Set<String> getContactsToTrackLocation() {
        return Collections.unmodifiableSet(trackingContactsList);
    }

    public void deleteContactFromSharingList(String contact) {
        sharingContactsList.remove(contact);
    }

    public void deleteContactFromTrackingList(String contact) {
        trackingContactsList.remove(contact);
    }

    public void addContactToTrackLocation(String contact) {
        trackingContactsList.add(contact);
    }
}
