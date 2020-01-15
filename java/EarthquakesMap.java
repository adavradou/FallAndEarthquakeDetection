package com.example.unipismartalert;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class EarthquakesMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    float DEFAULT_ZOOM = 3f;
    String TAG = "MapActivity";

   /* ArrayList<Double> latitudeList = new ArrayList();
    ArrayList<Double> longitudeList = new ArrayList();  //static ???*/

    static ArrayList<Warning> warningsList = new ArrayList<>();

    // Get a reference to our posts
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("Warnings");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquakes_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(EarthquakesMap.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        // Attach a listener to read the data at our posts reference
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                try {

                    warningsList.clear();
                    //Add data from Firebase to the List.
                    for (DataSnapshot warningSnapshot : dataSnapshot.getChildren()) {
                        Warning warning =  warningSnapshot.getValue(Warning.class);
                        warningsList.add(warning);

                    }

                }
                catch(Exception e){
                    Toast.makeText(EarthquakesMap.this,e.getMessage() , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //Mark Map with the Markers of the Firebase.
        try {
            if (!warningsList.isEmpty()) {
                for (int i = 0; i < warningsList.size(); i++) {
                    if (warningsList.get(i).getLatitude() != 0.0 &&  warningsList.get(i).getLongitude() != 0.0) {

                        LatLng markerLatLng = new LatLng(warningsList.get(i).getLatitude(), warningsList.get(i).getLongitude());
                        mMap.addMarker(new MarkerOptions().position(markerLatLng).title(warningsList.get(i).getEmergency()));
                    }
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(warningsList.get(0).getLatitude(),warningsList.get(0).getLongitude())));
                warningsList.clear();

            }
        }catch(Exception e){
            Toast.makeText(EarthquakesMap.this,e.getMessage() , Toast.LENGTH_LONG).show();
        }


        getDeviceLocation();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        //mMap.setOnMarkerClickListener(this);


    }


    public void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.show();
    }

    private void getDeviceLocation() { //Get the location of the device.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task location = mFusedLocationProviderClient.getLastLocation();
            ((Task) location).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: found location!");
                        Location currentLocation = (Location) task.getResult();
                        moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                DEFAULT_ZOOM);
                    } else {
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(EarthquakesMap.this, "unable to get current location",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom) { //Camera is always moved at the device's position.
        Log.d(TAG, "moveCamera: moving camera to: lat: " + latLng.latitude + "long: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

}
