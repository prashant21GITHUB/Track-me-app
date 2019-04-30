package com.pyb.trackme.restclient;

import java.util.ArrayList;
import java.util.List;

public class TrackingDetailsResponse {

    private boolean success;
    private ArrayList<String> sharingWith;
    private ArrayList<String> tracking;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ArrayList<String> getSharingWith() {
        return sharingWith;
    }

    public void setSharingWith(ArrayList<String> sharingWith) {
        this.sharingWith = sharingWith;
    }

    public ArrayList<String> getTracking() {
        return tracking;
    }

    public void setTracking(ArrayList<String> tracking) {
        this.tracking = tracking;
    }
}
