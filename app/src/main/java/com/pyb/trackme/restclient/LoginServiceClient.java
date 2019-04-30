package com.pyb.trackme.restclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface LoginServiceClient {

    @PUT("user/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("user/register")
    Call<LoginResponse> register(@Body LoginRequest loginRequest);

    @PUT("user/isregistered")
    Call<ServiceResponse> isUserRegistered(@Body MobileRequest mobile);
}
