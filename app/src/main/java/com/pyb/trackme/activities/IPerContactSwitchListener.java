package com.pyb.trackme.activities;

import android.widget.Switch;

public interface IPerContactSwitchListener {

    void onSharingContactSwitchClick(int position, boolean isChecked);

    void onTrackingContactSwitchClick(int position, boolean isChecked);
}
