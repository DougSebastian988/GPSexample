package com.example.android.gpsexample;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;




public class MainActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private TextView mAltitudeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mLatitudeText = (TextView) findViewById(R.id.latitude_text_view);
        mLongitudeText = (TextView) findViewById(R.id.longitude_text_view);
        mAltitudeText = (TextView) findViewById(R.id.altitude_text_view);

        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    private Location mLastLocation;
    private double latitude, longitude, result;

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText.setText("Latitude = " + String.valueOf(mLastLocation.getLatitude()));
            mLongitudeText.setText("Longitude = " + String.valueOf(mLastLocation.getLongitude()));
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
           FetchWeatherTask weatherTask = new FetchWeatherTask();
           weatherTask.execute();
        }

    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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

    private double getAltitude(Double longitude, Double latitude) {
        double result = Double.NaN;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL("https://maps.googleapis.com/maps/api/elevation/"
                    + "xml?locations=" + String.valueOf(latitude)
                    + "," + String.valueOf(longitude)
                    + "&sensor=true"
                    + "referer"
                    + "&key=AIzaSyDrmeljqwElxCt8T-Te4Z3SVE-f79-e7Vs");
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if (inputStream != null) {
                int r = -1;
                StringBuffer respStr = new StringBuffer();
                while ((r = -inputStream.read()) != -1)
                    respStr.append((char) r);
                String tagOpen = "<elevation>";
                String tagClose = "</elevation>";
                if (respStr.indexOf(tagOpen) != -1) {
                    int start = respStr.indexOf(tagOpen) + tagOpen.length();
                    int end = respStr.indexOf(tagClose);
                    String value = respStr.substring(start, end);
                    result = (double) (Double.parseDouble(value) * 3.2808399); // convert from meters to feet
                }
                inputStream.close();
            }
        } catch (IOException e) {

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result;
    }


    public class FetchWeatherTask extends AsyncTask<Double, Double, Double> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private String resp;
        @Override
        protected Double doInBackground(Double... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            Double result = 0.0;
            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {

                // Create the request to OpenWeatherMap, and open the connection
                URL url = new URL("https://maps.googleapis.com/maps/api/elevation/"
                        + "xml?locations=" + String.valueOf(latitude)
                        + "," + String.valueOf(longitude)
                        + "&sensor=true"
                        + "referer"
                        + "&key=AIzaSyAxXo9WeAv9Sg2YCL3E5B_K1TCknDJOy90");
                      //  + "&key=AIzaSyDrmeljqwElxCt8T-Te4Z3SVE-f79-e7Vs");
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();

                if (inputStream != null) {
                    int r = -1;
                    StringBuffer respStr = new StringBuffer();
                    while ((r = inputStream.read()) != -1)
                        respStr.append((char) r);
                    String tagOpen = "<elevation>";
                    String tagClose = "</elevation>";
                    if (respStr.indexOf(tagOpen) != -1) {
                        int start = respStr.indexOf(tagOpen) + tagOpen.length();
                        int end = respStr.indexOf(tagClose);
                        String value = respStr.substring(start, end);
                        result = (double) (Double.parseDouble(value) * 3.2808399); // convert from meters to feet
                    }
                    inputStream.close();
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return result;
        }
        @Override
        protected void onPostExecute(Double resp) {
            // execution of result of Long time consuming operation
            mAltitudeText.setText("Elevation = " + String.valueOf(resp));
        }


    }
}

