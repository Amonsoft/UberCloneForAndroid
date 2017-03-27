package com.parse.starter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class YourLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    LocationManager locationManager;
    String provider;
    Location location;
    Double lat;
    Double lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getCurrentLocation();


    }

    public void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = locationManager.getLastKnownLocation(provider);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        LatLng testLocation = new LatLng(0, 0);
        mMap.addMarker(new MarkerOptions().position(testLocation).title("Test Marker"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(testLocation, 12));

        if (location != null) {

            Log.i("Location Info", "Location acheived!");
            onLocationChanged(location);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    @Override
    public void onLocationChanged(Location location) {

        lat = location.getLatitude();
        lng = location.getLongitude();

        Log.i("Location lat", lat.toString());
        Log.i("Location lng", lng.toString());

        mMap.clear();

        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title("Your location"));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 12));


        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        try {

            List<Address> listAddresses = geocoder.getFromLocation(lat, lng, 1);

            if (listAddresses != null && listAddresses.size() > 0) {

                Log.i("Location Info:", listAddresses.get(0).toString());

            }

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
