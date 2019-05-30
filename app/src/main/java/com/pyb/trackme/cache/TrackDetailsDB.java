package com.pyb.trackme.cache;

import android.content.Context;
import android.content.SharedPreferences;

import com.pyb.trackme.adapter.GroupInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class TrackDetailsDB {

    private static TrackDetailsDB INSTANCE;

    private Set<String> sharingContactsList;
    private Set<String> trackingContactsList;
    private Map<String, Boolean> trackingContactStatus;
    private Map<String, Boolean> sharingContactStatus;
    private Map<String, GroupInfo> groupNameToGroupInfoMap;

    private TrackDetailsDB() {
        sharingContactsList = new LinkedHashSet<>();
        trackingContactsList = new LinkedHashSet<>();
        trackingContactStatus = new HashMap<>();
        sharingContactStatus = new HashMap<>();
        groupNameToGroupInfoMap = new LinkedHashMap<>();
    }

    public boolean isGroupAlreadyExists(String groupName) {
        return groupNameToGroupInfoMap.containsKey(groupName);
    }

    public void addNewGroup(String groupName, GroupInfo groupInfo) {
        groupNameToGroupInfoMap.put(groupName, groupInfo);
    }

    public void deleteGroup(String groupName, GroupInfo groupInfo) {
        groupNameToGroupInfoMap.put(groupName, groupInfo);
    }

    public boolean deleteContactFromGroup(String groupName, String contactToDelete, String groupAdmin) {
        if(groupNameToGroupInfoMap.containsKey(groupName)) {
            return groupNameToGroupInfoMap.get(groupName).deleteContact(contactToDelete, groupAdmin);
        }
        return false;
    }

    public boolean addContactFromGroup(String groupName, String contactToDelete, String groupAdmin) {
        if(groupNameToGroupInfoMap.containsKey(groupName)) {
            return groupNameToGroupInfoMap.get(groupName).addContact(contactToDelete, groupAdmin);
        }
        return false;
    }

    public void addContactsToShareLocation(Collection<String> contacts) {
        sharingContactsList.clear();
        sharingContactsList.addAll(contacts);
    }

    public void addContactToShareLocation(String contact) {
        sharingContactsList.add(contact);
    }

    public void addContactsToTrackLocation(Collection<String> contacts) {
        trackingContactsList.clear();
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
        sharingContactStatus.remove(contact);
    }

    public void deleteContactFromTrackingList(String contact) {
        trackingContactsList.remove(contact);
        trackingContactStatus.remove(contact);
    }

    public void addContactToTrackLocation(String contact) {
        trackingContactsList.add(contact);
    }

    public void updateTrackingStatus(String contact, boolean trackingStatus) {
        trackingContactStatus.put(contact, trackingStatus);
    }

    public void updateSharingStatus(String contact, boolean sharingStatus) {
        sharingContactStatus.put(contact, sharingStatus);
    }

    public void saveDataInPref(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getApplicationInfo().packageName + "_Login", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        JSONObject jsonObject = new JSONObject(trackingContactStatus);
        String trackingStatusStr = jsonObject.toString();
        editor.putString("trackingContactStatus", trackingStatusStr);
        editor.putString("sharingContactStatus", new JSONObject(sharingContactStatus).toString());
        editor.putStringSet("trackingContacts", trackingContactsList);
        editor.putStringSet("sharingContacts", sharingContactsList);
        editor.commit();
    }

    public boolean readDataFromPref(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getApplicationInfo().packageName + "_Login", MODE_PRIVATE);
        if(preferences.contains("sharingContacts") && preferences.contains("trackingContacts")) {
            sharingContactsList = preferences.getStringSet("sharingContacts", Collections.EMPTY_SET);
            trackingContactsList = preferences.getStringSet("trackingContacts", Collections.EMPTY_SET);
            String trackingStatusStr = preferences.getString("trackingContactStatus", new JSONObject().toString());
            trackingContactStatus = loadMap(trackingStatusStr);
            String sharingStatusStr = preferences.getString("sharingContactStatus", new JSONObject().toString());
            sharingContactStatus = loadMap(sharingStatusStr);
            return true;
        }
        return false;
    }

    private Map<String, Boolean> loadMap(String jsonStr) {
        Map<String, Boolean> outputMap = new HashMap<>();
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonStr);
            Iterator<String> keysItr = jsonObject.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                Boolean value = (Boolean) jsonObject.get(key);
                outputMap.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return outputMap;
    }

    public boolean getTrackingStatus(String contact) {
        if(trackingContactStatus.containsKey(contact)) {
            return trackingContactStatus.get(contact);
        }
        return false;
    }

    public boolean getSharingStatus(String contact) {
        if(sharingContactStatus.containsKey(contact)) {
            return sharingContactStatus.get(contact);
        }
        return false;
    }

    public boolean isSharingOnForAtLeastOneContact() {
        for(Boolean status : sharingContactStatus.values()) {
            if(status) {
                return true;
            }
        }
        return false;
    }

    public List<String> getCurrentContactsGettingTracked() {
        List<String> subscribers = new ArrayList<>();
        for(Map.Entry<String, Boolean> entry : trackingContactStatus.entrySet()) {
            if(entry.getValue()) {
                subscribers.add(entry.getKey());
            }
        }
        return subscribers;
    }
}
