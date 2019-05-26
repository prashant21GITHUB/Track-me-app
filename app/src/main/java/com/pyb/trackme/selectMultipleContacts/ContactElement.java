

package com.pyb.trackme.selectMultipleContacts;

import java.io.Serializable;

/**
 * A ContactElement (single contact or group) always has a unique id and a display name.
 * It also can be checked/unchecked and can be observer by attaching an OnContactCheckedListener.
 */
public interface ContactElement extends Serializable {

    long getId();

    String getDisplayName();

    boolean isChecked();

    void setChecked(boolean checked, boolean suppressListenerCall);

    void addOnContactCheckedListener(OnContactCheckedListener listener);

    boolean matchesQuery(String[] queryStrings);

}
