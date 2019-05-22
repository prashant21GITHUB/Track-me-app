package com.pyb.trackme.restclient;

public class AddTokenRequest {
    private final String mobile;
    private final String token;

    public AddTokenRequest(String mobile, String token) {
        this.mobile = mobile;
        this.token = token;
    }

    public String getMobile() {
        return mobile;
    }

    public String getToken() {
        return token;
    }
}
