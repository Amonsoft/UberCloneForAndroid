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
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * A google maps class which allows the user to request a ride and will eventually show if a driver
 * has acdepted their request, and how far away they are.
 */
public class YourLocation extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;

    LocationManager locationManager;
    String provider;
    Location location;
    Double lat;
    Double lng;

    TextView infoTextView;
    Button requestUberButton;
    Boolean requestActive = false;
    String driverUsername = "";

    ParseGeoPoint driverLocation = new ParseGeoPoint(0, 0);

    Handler handler = new Handler();


    /**
     * Very similar to onCreate method in ViewRequests class.
     * Accesses device location services and retrieves current or last known location data if
     * permissions are given.
     * @param savedInstanceState Non-persistant data which is saved and passed to onCreate in
     *                           instances such as orientation change
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = locationManager.getLastKnownLocation(provider);

        infoTextView = (TextView) findViewById(R.id.infoTextView);
        requestUberButton = (Button) findViewById(R.id.requestUber);


    }


    /**
     * Updates latitude and longitude of device.
     * Updates google maps marker and camera.
     * If there is no active request by the user then then the database queries the Requests table
     * and returns any rows where riderUsername is the same as the current users username.
     * When the query returns if there are no exceptions and if at least one row is returned then
     * a request is marked as active. When the active request is accepted by a driver the user is alerted
     * and told the driver's name and shown their location.
     *
     * @param location The current device location
     */
    public void updateUserLocation(final Location location) {

        lat = location.getLatitude();
        lng = location.getLongitude();

        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title("Your location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 10));



        if (!requestActive) {

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                requestActive = true;
                                infoTextView.setText("Finding Uber driver...");
                                requestUberButton.setText("Cancel Uber");

                                if (object.get("driverUsername") != null) {

                                    driverUsername = object.getString("driverUsername");
                                    infoTextView.setText("Driver is on their way");

                                    Log.i("AppInfo", driverUsername);

                                }

                            }

                        }

                    }

                }
            });



        }

        if (requestActive) {

            if (!driverUsername.equals("")) {

                ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
                userQuery.whereEqualTo("username", driverUsername);
                userQuery.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() > 0) {

                                for (ParseUser driver : objects) {

                                    driverLocation = driver.getParseGeoPoint("location");

                                }

                            }
                        }
                    }
                });

                if (driverLocation.getLatitude() != 0 && driverLocation.getLongitude() != 0) {
                    Log.i("AppInfo", driverLocation.toString());
                }

            }

            final ParseGeoPoint userLocation = new ParseGeoPoint(lat, lng);

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                object.put("riderLocation", userLocation);
                                object.saveInBackground();

                            }

                        }

                    }

                }
            });

        }

        /*
          Causes the Runnable in this thread to be run each 2000 milliseconds, updating location.
         */
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUserLocation(location);
            }
        }, 2000);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        updateUserLocation(location);

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

        mMap.clear();
        updateUserLocation(location);


        getAddress();

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

    /**
     * An as unused method which takes the users latitude and longitude and returns the closest house
     * address
     */
    public void getAddress() {

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


    /**
     * If the user hasn't made an actice ride reequest then a new request is put into the parse database
     * from the current user, it will include their location data too.
     * If there is am active request then it will be deleted from the database and cancelled.
     * @param view Responsible for drawing and event handling
     */
    public void requestUber(View view) {

        if (!requestActive) {

            final ParseObject request = new ParseObject("Requests");

            request.put("riderUsername", ParseUser.getCurrentUser().getUsername());

            ParseACL parseACL = new ParseACL();
            parseACL.setPublicWriteAccess(true);
            parseACL.setPublicReadAccess(true);
            request.setACL(parseACL);

            request.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    if (e == null) {

                        updateUserLocation(location);

                        infoTextView.setText("Finding Uber Driver...");
                        requestUberButton.setText("Cancel Uber");
                        requestActive = true;

                    }

                }
            });

        } else {

            ParseQuery<ParseObject> query = new ParseQuery<>("Requests");

            query.whereEqualTo("riderUsername", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {

                    if (e == null) {

                        if (objects.size() > 0) {

                            for (ParseObject object : objects) {

                                object.deleteInBackground();

                            }

                        }

                    }

                }
            });

            infoTextView.setText("Uber Cancelled");
            requestUberButton.setText("Request Uber");
            requestActive = false;

        }

    }

}
