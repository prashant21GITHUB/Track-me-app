// ILocationSharingService.aidl
package com.pyb.trackme.services;

// Declare any non-default types here with import statements

interface ILocationSharingService {

    void startLocationSharing();

    void stopLocationSharing();

    boolean isLocationSharingOn();
}
