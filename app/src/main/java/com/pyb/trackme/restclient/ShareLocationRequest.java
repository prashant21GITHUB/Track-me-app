package com.pyb.trackme.restclient;

import com.google.gson.annotations.SerializedName;

public class ShareLocationRequest {

    @SerializedName(value = "from_mobile")
    private final String fromMobile;
    @SerializedName(value = "to_mobile")
    private final String toMobile;
    @SerializedName(value = "to_name")
    private final String toName;


    public ShareLocationRequest(String fromMobile, String toMobile, String toName) {
        this.fromMobile = fromMobile;
        this.toMobile = toMobile;
        this.toName = toName;
    }

    public String getFromMobile() {
        return fromMobile;
    }

    public String getToMobile() {
        return toMobile;
    }

    public String getToName() {
        return toName;
    }
}
