/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * This application uses the parseplatform android api for database storage
 * http://parseplatform.org/docs/android/guide/
 */

/**
 * Welcome screen for this app where the user can choose if they are a driver or wanting a ride.
 */
public class MainActivity extends AppCompatActivity {

    Switch riderOrDriverSwitch;


    /**
     * Selects whether the user is a rider or a driver based on riderOrDriverSwitch position.
     * It then calls the getCurrentUser method and saves "rider" or "driver" to riderOrDriver column in the database.
     * @param view Responsible for drawing and event handling.
     */
    public void getStarted(View view) {

        String riderOrDriver = "rider";

        if (riderOrDriverSwitch.isChecked()) {

            riderOrDriver = "driver";

        }

        ParseUser.getCurrentUser().put("riderOrDriver", riderOrDriver);

        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.i("CurrentUser", "Current user logged and redirected");
                    redirectUser();

                }
            }
        });

    }

    /**
     * When ParseUser.logOut() is uncommented it logs out the currently logged in user on app start.
     * If there is no currently logged in user checkCurrentUser creates a new anonymous user in the database and
     * slogs them in.
     * If there is a currently logged in user checkCurrentUser calls redirectUser method.
     */
    public void checkCurrentUser() {
        ParseUser.logOut();

        ParseUser currentUser = ParseUser.getCurrentUser();

        if (currentUser.getUsername() == null) {

            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e != null) {
                        Log.d("MyApp", "Anonymous login failed.");
                    } else {
                        Log.d("MyApp", "Anonymous user logged in.");
                    }
                }
            });

        } else {

            if(currentUser.get("riderOrDriver") != null) {
                redirectUser();
            }

        }
    }


    public void hideActionBar() {
        getSupportActionBar().hide();
    }

    /**
     * Checks the database for the currentUser's riderOrDriver state.
     * If currentUser is a rider then a new intent (an abstract description of an action) is created
     * which changes the current context to YourLocation.
     * If currentUser is a driver then a new intent is created which changes the current context to
     * ViewRequests.
     */
    public void redirectUser() {

        if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {

            Intent intent = new Intent(getApplicationContext(), YourLocation.class);
            startActivity(intent);

        }  else {

            Intent intent = new Intent(getApplicationContext(), ViewRequests.class);
            startActivity(intent);

        }

    }

    /**
     * A lifecycle method that is called on initial loading of the application.
     * Is currently set to assign the currentUser as a rider on app initialisation, this is for convenient
     * testing purposes
     * @param savedInstanceState Non-persistant data which is saved and passed to onCreate in
     *                           instances such as orientation change
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParseUser.getCurrentUser().put("riderOrDriver", "rider");

        riderOrDriverSwitch = (Switch)findViewById(R.id.riderOrDriverSwitch);

        /**
         * ParseAnalytics allow tracking of events to and from the database
         */
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        hideActionBar();

        checkCurrentUser();

    }

    /**
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
