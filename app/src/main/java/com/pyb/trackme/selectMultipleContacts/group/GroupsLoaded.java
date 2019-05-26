

package com.pyb.trackme.selectMultipleContacts.group;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * All groups have been loaded.
 *
 * Publisher: ContactPickerActivity
 * Subscriber: GroupFragment
 */
public class GroupsLoaded {

    public static void post(List<? extends Group> groups) {
        GroupsLoaded event = new GroupsLoaded(groups);
        EventBus.getDefault().postSticky(event);
    }

    final private List<? extends Group> mGroups;

    private GroupsLoaded(List<? extends Group> groups) {
        mGroups = groups;
    }

    public List<? extends Group> getGroups() {
        return mGroups;
    }

}
