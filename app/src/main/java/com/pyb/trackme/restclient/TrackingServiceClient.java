package com.pyb.trackme.restclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface TrackingServiceClient {

    @PUT("user/track/details")
    Call<TrackingDetailsResponse> getTrackingDetails(@Body MobileRequest mobile);

    @POST("user/location/share/addcontact")
    Call<ServiceResponse> addContactForSharingLocation(@Body AddRemoveContactRequest addRemoveContactRequest);

    @POST("user/location/share/deletecontact")
    Call<ServiceResponse> deleteContactFromSharingLocationList(@Body AddRemoveContactRequest addRemoveContactRequest);

    @POST("user/location/track/addcontact")
    Call<ServiceResponse> addTrackingContact(@Body AddRemoveContactRequest addRemoveContactRequest);

    @POST("user/location/track/deletecontact")
    Call<ServiceResponse> deleteTrackingContact(@Body AddRemoveContactRequest addRemoveContactRequest);

}
