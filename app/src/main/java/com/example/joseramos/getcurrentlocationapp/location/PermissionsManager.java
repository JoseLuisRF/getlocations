package com.example.joseramos.getcurrentlocationapp.location;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by joseluisrf on 5/10/16.
 */
public class PermissionsManager {
    private static final String TAG = PermissionsManager.class.getSimpleName();
    private Activity context;

    public  interface PermissionsCallback {
        void onShowRequestPermissionRationale(String permission, int permissionRequest);
    }


    public PermissionsManager(Activity context) {
        this.context = context;
    }

    public void requestPermissions(String[] permissions, int permissionRequest, PermissionsCallback permissionsCallback, boolean isFromRationalAlert) {
        List<String> deniedPermissions = new ArrayList<>();
        List<String> rationalePermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission) && !isFromRationalAlert) {
                    rationalePermissions.add(permission);
                } else {
                    deniedPermissions.add(permission);
                }

            }
        }

        if( !Utils.isNullOrEmpty(deniedPermissions) ) {
            ActivityCompat.requestPermissions(
                    context,
                    deniedPermissions.toArray(new String[deniedPermissions.size()]),
                    permissionRequest);
        }

        if(!Utils.isNullOrEmpty(rationalePermissions)){
            for(String permission : rationalePermissions ){
                permissionsCallback.onShowRequestPermissionRationale(permission, permissionRequest);
            }
        }
    }


    public void requestPermissions(String permission, int permissionRequest, PermissionsCallback permissionsCallback, boolean isFromRationalAlert) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission) && !isFromRationalAlert) {
                permissionsCallback.onShowRequestPermissionRationale(permission, permissionRequest);
            } else {
                ActivityCompat.requestPermissions(
                        context,
                        new String[]{permission},
                        permissionRequest);
            }

        }
    }



    public boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

}
