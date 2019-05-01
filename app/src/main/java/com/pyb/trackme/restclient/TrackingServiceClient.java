package com.pyb.trackme.restclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface TrackingServiceClient {

    @PUT("user/track/details")
    Call<TrackingDetailsResponse> getTrackingDetails(@Body MobileRequest mobile);

    @POST("user/location/share/addcontact")
    Call<ServiceResponse> addContactForSharingLocation(@Body ShareLocationRequest shareLocationRequest);

    @POST("user/location/share/deletecontact")
    Call<ServiceResponse> deleteContactFromSharingLocationList(@Body ShareLocationRequest shareLocationRequest);

}
