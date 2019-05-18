package com.pyb.trackme.restclient;

import com.google.gson.annotations.SerializedName;

public class AddRemoveContactRequest {

    @SerializedName(value = "loggedInMobile")
    private final String mobile;
    @SerializedName(value = "contact")
    private final String contactToAdd;
    @SerializedName(value = "to_name")
    private final String contactName;


    public AddRemoveContactRequest(String fromMobile, String toMobile, String toName) {
        this.mobile = fromMobile;
        this.contactToAdd = toMobile;
        this.contactName = toName;
    }

    public String getMobile() {
        return mobile;
    }

    public String getContactToAdd() {
        return contactToAdd;
    }

    public String getContactName() {
        return contactName;
    }
}
