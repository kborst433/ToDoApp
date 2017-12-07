package edu.unc.borst.finalproject;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class MapsActivity
        extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 666;
    private GoogleMap googleMap;
    private GoogleApiClient client = null;
    BottomNavigationView bottomNavigationView;
    ArrayList<ToDoItem> toDoList = new ArrayList<>();
    Map<Integer, Geofence> geofences = new HashMap<Integer, Geofence>();
    Map<Integer, Circle> geofenceCircles = new HashMap<Integer, Circle>();
    Map<Integer, Marker> markers = new HashMap<Integer, Marker>();

    SQLiteDatabase db;
    boolean locationPermissionGranted;
    Location lastKnownLocation;
    LatLng lastKnownLatLng;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location defaultChapelHill = new Location(LocationManager.GPS_PROVIDER);
    Location defaultNyack = new Location(LocationManager.GPS_PROVIDER);
    AlertDialog.Builder builder;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        defaultChapelHill.setLatitude(35.913);
        defaultChapelHill.setLongitude(-79.056);

        defaultNyack.setLatitude(41.107);
        defaultNyack.setLongitude(-73.916);

        locationPermissionGranted = false;
        builder = new AlertDialog.Builder(this);

        if (client == null) {
            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();
        }
        client.connect();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(1 * 1000 / 2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.view_map);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.view_map:
                        break;

                    case R.id.add_task:
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        break;

                    case R.id.view_task_list:
                        startActivity(new Intent(getApplicationContext(), ToDoList.class));
                        break;
                }
                return true;
            }
        });

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int[] ids = intent.getIntArrayExtra("triggeredGeofences");
            for (int i = 0; i < ids.length; i++) {
                Cursor c = db.rawQuery("SELECT * FROM TasksTable WHERE ID = " + ids[i], null);
                c.moveToFirst();
                Date started = null;
                Date due = null;
                LatLng latLng;
                final ToDoItem tdi;
                try {
                    started = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(6));
                    due = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(7));
                } catch (ParseException p) {
                }
                latLng = new LatLng(c.getDouble(4), c.getDouble(5));
                tdi = new ToDoItem(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), latLng, started, due, c.getInt(8), db);
                sendAlertDialog(tdi);
            }
        }
    };

    public void sendAlertDialog(final ToDoItem tdi) {
        Log.v("TAG", "ALERT DIALOG SENT");
        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vib.vibrate(500);
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(tdi.getDescription() + "\n" + tdi.getAddress());
        alert.setCancelable(true);

        alert.setPositiveButton(
                "Complete Task",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int tdiId = tdi.getId();
                        removeGeofence(tdiId);
                        db.execSQL("UPDATE TasksTable SET Completed = 1 WHERE ID = " + tdiId);
                        dialog.dismiss();
                    }
                });

        alert.setNegativeButton(
                "Ignore",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = alert.create();
        alertDialog.show();
    }

    public void removeGeofence(int id) {
        Marker marker = markers.get(id);
        markers.remove(id);
        Circle circle = geofenceCircles.get(id);
        geofenceCircles.remove(id);
        Geofence geofence = geofences.get(id);
        List<String> geofenceToRemove = new ArrayList<>();
        geofenceToRemove.add(geofence.getRequestId());
        LocationServices.GeofencingApi.removeGeofences(client,
                geofenceToRemove).setResultCallback(this);
        geofences.remove(id);
        marker.remove();
        circle.remove();
        Snackbar.make(findViewById(R.id.map), "Task Completed!",
                Snackbar.LENGTH_SHORT)
                .show();
    }

    public void onMapReady(GoogleMap googleMap) {

        LocalBroadcastManager.getInstance(MapsActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("triggered"));

        this.googleMap = googleMap;
        updateLocationUI();

        getDeviceLocation();
        // Add a marker in Chapel Hill and move the camera

        googleMap.setOnMarkerClickListener(this);

        db = openOrCreateDatabase("Tasksdb", MODE_PRIVATE, null);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            this.googleMap.setMyLocationEnabled(true);
        } else {
        }


        Cursor c = db.rawQuery("SELECT * FROM TasksTable WHERE Completed = 0", null);
        c.moveToFirst();
        Date started = null;
        Date due = null;
        LatLng latLng;
        if ((c != null) && (c.getCount() > 0)) {
            try {
                started = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(6));
                due = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(7));
            } catch (ParseException p) {
            }
            latLng = new LatLng(c.getDouble(4), c.getDouble(5));

            ToDoItem tdi = new ToDoItem(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), latLng, started, due, c.getInt(8), db);
            toDoList.add(tdi);

            while (c.moveToNext()) {
                try {
                    started = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(6));
                    due = new SimpleDateFormat("yyyy-MM-dd").parse(c.getString(7));
                } catch (ParseException p) {
                }

                latLng = new LatLng(c.getDouble(4), c.getDouble(5));

                tdi = new ToDoItem(c.getInt(0), c.getInt(1), c.getString(2), c.getString(3), latLng, started, due, c.getInt(8), db);
                toDoList.add(tdi);
            }
        }


        for (int i = 0; i < toDoList.size(); i++) {
            ToDoItem toDoItem = toDoList.get(i);
            Marker locationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(toDoItem.getLatLng())
                    .title(toDoItem.getDescription()));
            locationMarker.setTag(toDoItem);
            markers.put(toDoItem.getId(), locationMarker);

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(String.valueOf(toDoItem.getId()))
                    .setCircularRegion(toDoItem.getLatLng().latitude,
                            toDoItem.getLatLng().longitude,
                            toDoItem.getRadius())
                    .setExpirationDuration(NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build();
            geofences.put(toDoItem.getId(), geofence);

            int color = toDoItem.getColor();
            int strokeColor = Color.parseColor(toDoItem.getBorderColor());
            int radius = toDoItem.getRadius();
            CircleOptions circleOptions = new CircleOptions()
                    .center(toDoItem.getLatLng())
                    .radius(radius)
                    .fillColor(color)
                    .strokeColor(strokeColor)
                    .strokeWidth(3)
                    .visible(true);
            geofenceCircles.put(toDoItem.getId(), this.googleMap.addCircle(circleOptions));

        }

    }

    @Override
    protected void onStart() {
        client.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        client.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLatLng = new LatLng(location.getLatitude(),
                location.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 12.0f));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v("TAG", "onConnected Called");
        if (geofences.size() > 0) {
            try {
                LocationServices.GeofencingApi.addGeofences(client, createGeofenceRequest(),
                        createGeofencePendingIntent()).setResultCallback(this);
            } catch (SecurityException e) {
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        client.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (googleMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (locationPermissionGranted) {
                Task locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = (Location) task.getResult();
                            if (lastKnownLocation != null) {
                                lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 12.0f));
                            } else {
                                Log.v("TAG", "DEFAULT LOC");
                                lastKnownLocation = defaultNyack;
                            }
                        } else {
                            Log.v("TAG", "Current location is null. Using defaults.");
                            Log.v("TAG", "Exception: %s", task.getException());
                            googleMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(new LatLng(defaultNyack.getLatitude(),
                                            defaultNyack.getLongitude()), 12.0f));
                            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest() {
        ArrayList<Geofence> geofenceArrayList = new ArrayList<Geofence>(geofences.values());
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceArrayList)
                .build();
    }

    private PendingIntent createGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onResult(@NonNull Status status) {

    }
}
