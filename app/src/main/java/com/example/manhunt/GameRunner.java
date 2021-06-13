package com.example.manhunt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.manhunt.databinding.ActivityGameRunnerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GameRunner extends FragmentActivity implements OnMapReadyCallback {

    //database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private GoogleMap mMap;
    private ActivityGameRunnerBinding binding;
    private Options GameOptions;
    private FusedLocationProviderClient fusedLocationClient; // fused location provider client
    private String LobbyChosen;
    private GlobalPlayerClass globalPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        super.onCreate(savedInstanceState);
        binding = ActivityGameRunnerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // application player object
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        LobbyChosen = globalPlayer.getLobbychosen();


        // setting initial player status in top left
        ShowStatus();

        myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) { // on data change of a runner's coordinates
                mMap.clear(); // clear map
                runnerLocations(snapshot); // redraw the map with new locations

                if ((boolean) snapshot.child(globalPlayer.getName()).child("hunter").getValue()) {
                    globalPlayer.setHunter(true);

                    /* Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    } */

                    startActivity(new Intent(GameRunner.this, Game.class));
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });



        /* * * * * * * * * * * * * * * */
        // Handler for updating coordinates in real time below

        // constants
        final Handler handler = new Handler();
        final int delay = 200;

        // handler
        handler.postDelayed(new Runnable() {
            public void run() {
                System.out.println("myHandler: here!");
                if (ActivityCompat.checkSelfPermission(GameRunner.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(GameRunner.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        globalPlayer.setLongitude(location.getLongitude());
                        globalPlayer.setLatitude(location.getLatitude());
                        myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("latitude").setValue(globalPlayer.getLatitude());
                        myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("longitude").setValue(globalPlayer.getLongitude());

                    }
                });
                // Do your work here
                handler.postDelayed(this, delay);
            }
        }, delay);

        /*
        myRef.child("lobbies").child(LobbyChosen).child("scan").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if ((boolean) snapshot.getValue()) {
                    //asking to use location
                    if (ActivityCompat.checkSelfPermission(Game.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Game.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            globalPlayer.setLongitude(location.getLongitude());
                            globalPlayer.setLatitude(location.getLatitude());

                            myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("latitude").setValue(globalPlayer.getLatitude());
                            myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("longitude").setValue(globalPlayer.getLongitude());

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
          */
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    private void runnerLocations(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())) , Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            // draw hunters in blue, and runners in red
            if((boolean) dataSnapshot.child("hunter").getValue()){ // hunters
                // does nothing since runners can only see other runners

            } else { // runners
                mMap.addMarker(new MarkerOptions().position(PlayerLocation).title(dataSnapshot.getKey()));
            }
        }
    }

    private void ShowStatus() { // textview in top right showing the player status (runner)
        TextView txtPlayerStatus = (TextView) findViewById(R.id.txtPlayerStatus);

            txtPlayerStatus.setTextColor(Color.GREEN);
            txtPlayerStatus.setText("Runner");

    }


    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            startActivity(new Intent(GameRunner.this, Start.class));
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to leave the game", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);    }
}