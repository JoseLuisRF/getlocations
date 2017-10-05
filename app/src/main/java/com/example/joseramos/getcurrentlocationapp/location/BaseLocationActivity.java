package com.example.joseramos.getcurrentlocationapp.location;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.joseramos.getcurrentlocationapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;


/**
 * Created by jose.ramos on 10/4/17.
 */

public abstract class BaseLocationActivity extends AppCompatActivity
        implements PermissionsManager.PermissionsCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = BaseLocationActivity.class.getSimpleName();
    protected final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    protected final static String KEY_LOCATION = "location";
    private static final long POLLING_FREQ = 5000; // 5 seconds
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int REQUESTED_PERMISSION_INDEX = 0;
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    protected boolean requestingLocationUpdates;
    private PermissionsManager mPermissionsManager;
    protected Location mCurrentLocation;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected LocationSettingsRequest mLocationSettingsRequest;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);
        mPermissionsManager = new PermissionsManager(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(POLLING_FREQ);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

    }

    /***********************************************************************************************
     * Class Method
     ************************************************************************************************/


    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST: {
                    if (grantResults[REQUESTED_PERMISSION_INDEX] == PackageManager.PERMISSION_GRANTED) {
                        startLocationUpdates();
                    } else {
                        //TODO:
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                requestLocationUpdates(false);
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                if (!Utils.isGpsEnabled(this)) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    this.startActivity(intent);
                }
                break;
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    private void requestLocationUpdates(boolean isRationalPermission) {
        if (requestingLocationUpdates) {
            return;
        }

        Log.i(TAG, "requestLocationUpdates");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!mPermissionsManager.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                mPermissionsManager.requestPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST,
                        this, isRationalPermission);
                return;
            }
        }

        try {

            requestLastKnownLocation();
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    this).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    Log.i(TAG, "FusedLocationApi.onResult-Status.isSuccess:" + status.isSuccess());
                    requestingLocationUpdates = true;
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
            showRationalPermissionForLocation();
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                requestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES);
            }

            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates);
        outState.putParcelable(KEY_LOCATION, mCurrentLocation);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onShowRequestPermissionRationale(final String permission, final int permissionRequest) {
        if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
            showRationalPermissionForLocation();
        }
    }

    /***********************************************************************************************
     * Protected Methods
     ************************************************************************************************/

    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(
                            mGoogleApiClient,
                            mLocationSettingsRequest);
            result.setResultCallback(this);
        }
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient,
                    this
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    requestingLocationUpdates = false;
                }
            });
        }
    }

    private void showRationalPermissionForLocation() {
        String title = getString(R.string.publish_alert_title_permission_location);
        String rationalMessage = getString(R.string.publish_alert_message_permission_location);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(rationalMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestLocationUpdates(true);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        onLocationApiReady();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    protected Location requestLastKnownLocation() {
        Location location = null;
        try {
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } catch (SecurityException e) {
            e.printStackTrace();
            showRationalPermissionForLocation();
        }
        if (location != null) {
            onLocationChanged(location);
        } else {
            startLocationUpdates();
        }
        return location;
    }

    protected abstract void onLocationApiReady();

    public boolean isAPIConnected() {
        return mGoogleApiClient.isConnected();
    }
}
