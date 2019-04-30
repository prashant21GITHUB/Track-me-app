package com.pyb.trackme.restclient;

public class LoginRequest {
    private final String mobile;
    private final String password;
    private final String name;

    public LoginRequest(String mobile, String password, String name) {
        this.mobile = mobile;
        this.password = password;
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }
}
