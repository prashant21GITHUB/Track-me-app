package com.pyb.trackme.fcm;

public enum MessageAction {

    STARTED_SHARING,
    TRACKING_REQUEST,
    UNKNOWN;

    public static MessageAction parse(String action) {
        if(STARTED_SHARING.name().equals(action)) {
            return STARTED_SHARING;
        }
        if(TRACKING_REQUEST.name().equals(action)) {
            return TRACKING_REQUEST;
        }
        return UNKNOWN;
    }
}
