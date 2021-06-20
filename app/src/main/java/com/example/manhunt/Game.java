package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.manhunt.databinding.ActivityGameBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
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

import java.util.HashMap;

public class Game extends FragmentActivity implements OnMapReadyCallback {

    //database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private GoogleMap mMap;
    private ActivityGameBinding binding;
    private Options GameOptions;
    private FusedLocationProviderClient fusedLocationClient; // fused location provider client
    private String LobbyChosen, username;
    private GlobalPlayerClass globalPlayer;
    Button scan;
    boolean ready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        scan = (Button) findViewById(R.id.btnScan); //scanner button for hunters;
        // application player object
        LobbyChosen = globalPlayer.getLobbychosen();
        username = globalPlayer.getName();


        // setting initial visibility of scan button and player status in top right
        if (globalPlayer.isHunter()) {
            ShowButton();
            ShowStatus("Hunter", Color.RED);
        } else {
            ShowStatus("Runner", Color.GREEN);
            hideButton();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* * * * * * * * * * * * * * * */
        // Handler for updating coordinates in real time below

        // constants
        final Handler handler = new Handler();
        final int delay = 200;

        // handler
        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            public void run() {
                if (globalPlayer.isHunter()) {
                    ShowStatus("hunter", Color.RED); //Updating user interface
                    ShowButton(); //Updating user interface
                    myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) { // on data change of a runner's coordinates
                            if(ready){
                                checkCaught(snapshot);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                }

                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        globalPlayer.setLongitude(location.getLongitude());
                        globalPlayer.setLatitude(location.getLatitude());

                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());

                        ready = true;
                    }
                });
                // Do your work here
                handler.postDelayed(this, delay);
            }
        }, delay);

        // Listener for scan button when clicked by hunters
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("lobbies").child(LobbyChosen).child("scan").setValue(true); //sets scan object to true now the locations of the runners become available
            }
        });


        if(globalPlayer.isHunter()) {
            myRef.child("lobbies").child(LobbyChosen).child("scan").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot scanSnapshot) {
                    if ((boolean) scanSnapshot.getValue()) {
                        myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) { // on data change of a runner's coordinates
                                mMap.clear(); // clear map
                                MarkLocation(snapshot); // redraw the map with new locations
                            }
                            @Override
                            public void onCancelled(DatabaseError error) {
                            }
                        });

                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }

        if(!globalPlayer.isHunter()) {
            myRef.child("lobbies").child(LobbyChosen).child("users").child(globalPlayer.getName()).child("hunter").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot scanSnapshot) {
                    globalPlayer.setHunter(true);
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }
    }


    private void MarkLocation(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())) , Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            // draw hunters in blue, and runners in red
            if((boolean) dataSnapshot.child("hunter").getValue()){ // hunters
                mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(180)).position(PlayerLocation).title(dataSnapshot.getKey()));
            } else { // runners
                mMap.addMarker(new MarkerOptions().position(PlayerLocation).title(dataSnapshot.getKey()));
            }
        }
        myRef.child("lobbies").child(LobbyChosen).child("scan").setValue(false);
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

    private void ShowStatus(String type, int color) { // textview in top right showing the player status (hunter)
        TextView txtPlayerStatus = (TextView) findViewById(R.id.txtPlayerStatus);

            txtPlayerStatus.setTextColor(color); // colour change for visibility
            txtPlayerStatus.setText(type);
    }

    private void ShowButton() {
        // scan button visibility
        View StartVisibility = findViewById(R.id.btnScan);
        StartVisibility.setVisibility(View.VISIBLE);
    }

    private void hideButton() {
        // scan button visibility
        View StartVisibility = findViewById(R.id.btnScan);
        StartVisibility.setVisibility(View.INVISIBLE);
    }
    private void checkCaught(DataSnapshot snapshot) {

        Location myLocation = new Location(""); // my location, using
        myLocation.setLatitude(globalPlayer.getLatitude());
        myLocation.setLongitude(globalPlayer.getLongitude());

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if (!((boolean) dataSnapshot.child("hunter").getValue())) {
                String playerName = dataSnapshot.getKey();

                Location playerLocation = new Location("");// looping through the other player locations
                playerLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                playerLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(playerLocation); // distance to the other players
                System.out.println(globalPlayer.getName() + " is " + distanceInMeters + " meters away from " + playerName);

                if (distanceInMeters <= 10) { // people within 10 meters
                    myRef.child("lobbies").child(LobbyChosen).child("users").child(playerName).child("hunter").setValue(true);
                }
            }
        }
    }


    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            startActivity(new Intent(Game.this, Start.class));
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