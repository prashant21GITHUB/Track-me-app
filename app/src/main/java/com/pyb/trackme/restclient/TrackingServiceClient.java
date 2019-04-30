package com.pyb.trackme.restclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;

public interface TrackingServiceClient {

    @PUT("user/track/details")
    Call<TrackingDetailsResponse> getTrackingDetails(@Body MobileRequest mobile);

    @PUT("user/location/share")
    Call<ServiceResponse> addContactsForSharingLocation(@Body ShareLocationRequest shareLocationRequest);

}
