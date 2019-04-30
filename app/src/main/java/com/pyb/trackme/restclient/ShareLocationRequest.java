package com.pyb.trackme.restclient;

import java.util.List;

public class ShareLocationRequest {

    private final String mobile;
    private final List<String> contacts;


    public ShareLocationRequest(String mobile, List<String> contacts) {
        this.mobile = mobile;
        this.contacts = contacts;
    }

    public String getMobile() {
        return mobile;
    }

    public List<String> getContacts() {
        return contacts;
    }
}
