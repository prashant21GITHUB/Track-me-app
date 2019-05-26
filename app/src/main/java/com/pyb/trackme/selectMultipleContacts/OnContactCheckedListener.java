

package com.pyb.trackme.selectMultipleContacts;

public interface OnContactCheckedListener<E extends ContactElement> {

    void onContactChecked(E contact, boolean wasChecked, boolean isChecked);

}
