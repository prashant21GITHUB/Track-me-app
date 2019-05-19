package com.pyb.trackme.activities;

public interface IPerContactSwitchListener {

    void onSharingContactSwitchClick(int position, boolean isChecked);

    void onTrackingContactSwitchClick(int position, boolean isChecked);
}
