package com.parse.starter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


/**
 * A class for the driver to view active ride requests.
 */
public class ViewRequests extends AppCompatActivity implements LocationListener {

    ListView listView;
    ArrayList<String> listViewContent;
    ArrayList<String> usernames;
    ArrayList<Double> latitudes;
    ArrayList<Double> longitudes;
    ArrayAdapter<String> arrayAdapter;
    LocationManager locationManager;
    String provider;
    Location location;
    Double lat;
    Double lng;


    /**
     * Initialises the ViewRequests screen with a listView view, populates the listView with inital loading
     * text
     * Creates an adapter which sets each item in listViewContent to a built in XML layout and maps
     * each on to listView.
     * Accesses the devices location service and finds a location provider based on default criteria
     * which in this case is ranked by device power requirements.
     * Checks if location permission has been given and if true finds data from the last location
     * where gps services were enabled then calls and passes that data to onLocationChanged method.
     * Creates a listener which checks if a listView item has been clicked.
     *
     * @param savedInstanceState    Non-persistant data which is saved and passed to onCreate in
     *                              instances such as orientation change
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        listView = (ListView) findViewById(R.id.listView);
        usernames = new ArrayList<>();
        latitudes = new ArrayList<>();
        longitudes = new ArrayList<>();

        listViewContent = new ArrayList<>();
        listViewContent.add("Finding nearby requests...");

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listViewContent);

        listView.setAdapter(arrayAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = locationManager.getLastKnownLocation(provider);

        onLocationChanged(location);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * A method for dealing with the event in which a listView item is clicked.
             * Creates new intent which changes the current context to ViewRiderLocation screen.
             * Adds extended data to the intent from rider username and location, and driver location.
             * @param parent    The AdapterView where the click happened
             * @param view      The view in which to change to
             * @param position  The position of the view in the adapter
             * @param id        The row id of the clicked item
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(), ViewRiderLocation.class);

                intent.putExtra("username", usernames.get(position));
                intent.putExtra("latitude", latitudes.get(position));
                intent.putExtra("longitude", longitudes.get(position));
                intent.putExtra("userLatitude", location.getLatitude());
                intent.putExtra("userLongitude", location.getLongitude());

                startActivity(intent);

            }
        });

    }


    /**
     * Updates the current user's location.
     * userLocation changes the location point into an object with lat and lng keys.
     * The user's location object is updated in the parse database.
     * Database Requests table is queried and returns the location data for the 10 closest ride
     * requests which haven't been accepted by a driver yet.
     */
    public void updateUserLocation() {

        final ParseGeoPoint userLocation = new ParseGeoPoint(lat, lng);

        ParseUser.getCurrentUser().put("location", userLocation);
        ParseUser.getCurrentUser().saveInBackground();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Requests");
        query.whereDoesNotExist("driverUsername");
        query.whereNear("riderLocation", userLocation);
        query.setLimit(10);
        query.findInBackground(new FindCallback<ParseObject>() {
            /**
             * When the database query is finished if there are no errors and at least one row object
             * has been fetched then the distance in kilometers between the user and each ride request
             * is calculated.
             * The underlying view is then updated to reflect the new data.
             * @param objects   Each row returned from the database query
             * @param e         error
             */
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null) {

                    if (objects.size() > 0) {

                        listViewContent.clear();
                        usernames.clear();
                        latitudes.clear();
                        longitudes.clear();

                        for (ParseObject object : objects) {

                            Double distanceInKms = userLocation.distanceInKilometersTo((ParseGeoPoint) object.get("riderLocation"));

                            Double roundedDistance = (double) Math.round(distanceInKms * 10) / 10;

                            listViewContent.add(roundedDistance.toString() + " kms");

                            usernames.add(object.getString("riderUsername"));
                            latitudes.add(object.getParseGeoPoint("riderLocation").getLatitude());
                            longitudes.add(object.getParseGeoPoint("riderLocation").getLongitude());

                        }

                        arrayAdapter.notifyDataSetChanged();

                    }

                }

            }
        });

    }

    /**
     * A lifecycle method that is called when the app is exited but not destroyed.
     * Turns off location data retrieval.
     */
    @Override
    protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(this);
    }

    /**
     * A lifecycle method that is called when the app is brought to the foreground but has already
     * been created.
     * Checks device location permissions and turns on location data retrieval which fetches every
     * 400 milliseconds or each 1 meter moved.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    /**
     * Google api method that is called when a new user location is known.
     * Updates the latitude and longitude then calls updateUserLocation method.
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {

        lat = location.getLatitude();
        lng = location.getLongitude();

        updateUserLocation();


    }


    /**
     *  LocationListener method called when the provider status changes
     * @param provider  The location provider instantiated above
     * @param status    Indicates if the provider is in or out of service range
     * @param extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /**
     * LocationListener method called when the provider is enabled by a user.
     * @param provider
     */
    @Override
    public void onProviderEnabled(String provider) {

    }

    /**
     * LocationListener method called when the provider is disabled by a user.
     * @param provider
     */
    @Override
    public void onProviderDisabled(String provider) {

    }
}
