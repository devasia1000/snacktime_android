package com.snacktime.devasia.snacktime.map;

import android.content.Intent;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.snacktime.devasia.snacktime.Constants;
import com.snacktime.devasia.snacktime.MainActivity;
import com.snacktime.devasia.snacktime.util.ReverseGeocoding;

/**
 * Created by devasia on 1/2/15.
 */

public class MarkerDragManager implements GoogleMap.OnMarkerDragListener {

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        final LatLng pos = marker.getPosition();

        Constants.lat = pos.latitude;
        Constants.lon = pos.longitude;

        new Thread(new Runnable() {
            public void run() {
                ReverseGeocoding geocoding = new ReverseGeocoding();
                geocoding.getAddress(pos.latitude, pos.longitude);

                if (geocoding.getAddress2().equals("")) {
                    Constants.streetAddress = geocoding.getAddress1();
                } else {
                    Constants.streetAddress = geocoding.getAddress1()
                            + ", " + geocoding.getAddress2();
                }
                Constants.cityAddress = geocoding.getCity() + ", " + geocoding.getState() +
                        " - " + geocoding.getPIN();

                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                        .putExtra("addressUpdate", ""));
            }
        }).start();
    }
}
