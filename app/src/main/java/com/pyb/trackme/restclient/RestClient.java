package com.pyb.trackme.restclient;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestClient {

    private static Retrofit retrofit;
    private static LoginServiceClient loginServiceClient;
    private static TrackingServiceClient trackingServiceClient;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(ServiceURL.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static LoginServiceClient getLoginServiceClient() {
        if(loginServiceClient == null) {
            loginServiceClient = getRetrofitInstance().create(LoginServiceClient.class);
        }
        return loginServiceClient;
    }

    public static TrackingServiceClient getTrackingServiceClient() {
        if(trackingServiceClient == null) {
            trackingServiceClient = getRetrofitInstance().create(TrackingServiceClient.class);
        }
        return trackingServiceClient;
    }
}
