package com.example.joseramos.getcurrentlocationapp;

import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import com.example.joseramos.getcurrentlocationapp.databinding.ActivityMainBinding;
import com.example.joseramos.getcurrentlocationapp.location.BaseLocationActivity;

public class MainActivity extends BaseLocationActivity {

    private ActivityMainBinding binding;
    private boolean buttonPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.btnGetLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonPressed = !buttonPressed;

                binding.btnGetLocations.setText(buttonPressed ?
                        getString(R.string.stop_location_updates) :
                        getString(R.string.start_gps));

                if (buttonPressed) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });
    }

    @Override
    protected void onLocationApiReady() {
        binding.btnGetLocations.setEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        String msg = "\nlatitude:" + location.getLatitude() + ", longitude: " + location.getLongitude();
        binding.tvResult.append(msg);
    }
}
