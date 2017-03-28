package com.parse.starter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;


/**
 * A google maps class which shows the driver a ride request location and allows them to accept or
 * ignore the request.
 */
public class ViewRiderLocation extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Intent intent;
    Double lat;
    Double lng;
    Double userLat;
    Double userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_rider_location);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        intent = getIntent();

    }


    /**
     * A Google Maps api method that is called when the mapis ready to be used.
     *Retrieves location data from intent
     * @param googleMap A non-null instance of a GoogleMap associated with the MapFragment or MapView
     *                  that defines the callback
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        lat = intent.getDoubleExtra("latitude", 0);
        lng = intent.getDoubleExtra("longitude", 0);
        userLat = intent.getDoubleExtra("userLatitude", 0);
        userLng = intent.getDoubleExtra("userLongitude", 0);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.relativeLayout);
        /**
         * Registers listeners that are aware of global changes in the relativeLayout view.
         * Automatically resizes and positions the map so that both the rider and driver
         * markers are displayed.
         * Caluculates bounds of all markers to be displayed.
         * Adds markers and animates camera change.
         */
        mapLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                ArrayList<Marker> markers = new ArrayList<>();

                markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title("Rider location")));
                markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(userLat, userLng)).title("Your location")));

                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }

                LatLngBounds bounds = builder.build();

                int padding = 100;
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                mMap.animateCamera(cu);

            }
        });

    }

    /**
     * Lets the driver accept an open ride request.
     * Queries the Requests table in Parse database and returns any rows where riderUsername is equal
     * to the username of the current request view.
     * When the query is complete, if there are no exceptions and a row is returned then
     * the driverUsername is added to that row and the request is no longer retrievable is ViewRequests
     * The user is redirected to google maps app with the rider request's location data entered and
     * and ready to be given directions.
     * @param view
     */
    public void acceptRequest(View view) {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereEqualTo("riderUsername", intent.getStringExtra("username"));

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null) {

                    if (objects.size() > 0) {

                        for (ParseObject object : objects) {

                            object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
                            object.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {

                                    if (e == null) {

                                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                                Uri.parse("http://maps.google.com/maps?daddr=" + lat + "," + lng));
                                        startActivity(intent);

                                    }

                                }
                            });

                        }

                    }

                }

            }
        });

    }

    /**
     * Takes the user back to the ViewRequests screen
     * @param view
     */
    public void back(View view) {

        Intent i = new Intent(getApplicationContext(), ViewRequests.class);
        startActivity(i);

    }
}
