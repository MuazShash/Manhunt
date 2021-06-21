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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    boolean ready = false; //Flags if the round start timer is finished
    long startTime = System.currentTimeMillis(), warningTimer = System.currentTimeMillis(); //Stores information for round start and out of bounds timers
    double startLat, startLng; //Stores starting latitude and longitude

    @SuppressLint("MissingPermission")
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

        if(globalPlayer.isLeader()){ //Sets the start position to the leader's position when they press start game and updates the database
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //Updating starting coordinates
                    startLat = location.getLatitude();
                    startLng = location.getLongitude();
                    LatLng startPosition = new LatLng(startLat, startLng);

                    //Updating database
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("startLat").setValue(startLat);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("startLng").setValue(startLng);

                    //Draws a circle on the map
                    CircleOptions boundary = new CircleOptions().center(startPosition).radius(1000);
                    mMap.addCircle(boundary);
                }
            });
        }
        else{ //Reads the start position from the database for non leaders
            myRef.child("lobbies").child(LobbyChosen).child("startLat").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (task.isSuccessful()) {
                        startLat = Double.parseDouble(String.valueOf(task.getResult().getValue()));
                    }
                }
            });

            myRef.child("lobbies").child(LobbyChosen).child("startLng").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                        startLng = Double.parseDouble(String.valueOf(task.getResult().getValue()));

                        LatLng startPosition = new LatLng(startLat, startLng);

                        //Draws a circle on the map
                        CircleOptions boundary = new CircleOptions().center(startPosition).radius(1000);
                        mMap.addCircle(boundary);

                }
            });
        }

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
                        public void onDataChange(DataSnapshot snapshot) { // on data change of a player's coordinates
                            if(ready){ //Checks if the start timer is complete
                                checkCaught(snapshot); //Checks if the runner/hunter are close enough to eachother
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                }

                //Updates the database with the user's current location
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {

                    @Override
                    public void onSuccess(Location location) {

                        globalPlayer.setLongitude(location.getLongitude());
                        globalPlayer.setLatitude(location.getLatitude());

                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());


                        //Checks if the user is out of bounds
                        Location myLocation = new Location("");
                        myLocation.setLatitude(globalPlayer.getLatitude());
                        myLocation.setLongitude(globalPlayer.getLongitude());

                        Location startLocation = new Location("");
                        startLocation.setLatitude(startLat);
                        startLocation.setLongitude(startLng);

                        if(myLocation.distanceTo(startLocation) <= 1000){
                            warningTimer = System.currentTimeMillis();
                        }
                        else{
                            showToast("You have " + Math.floor((30000 - System.currentTimeMillis()-warningTimer)/1000) + " seconds to return to game");
                            if(System.currentTimeMillis() - warningTimer >= 30000){
                                showToast("You have been out of bounds for too long and have been turned into a hunter!");
                                myRef.child("lobbies").child(LobbyChosen).child("users").child(globalPlayer.getName()).child("hunter").setValue(true); //Convert the runner to a hunter
                            }
                        }

                    }
                });

                if(System.currentTimeMillis() - startTime >= 10000){ //Checks if the start timer is complete
                    ready = true;
                }
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


        if(globalPlayer.isHunter()) { //Only hunters should have to listen to the scan attribute
            myRef.child("lobbies").child(LobbyChosen).child("scan").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot scanSnapshot) {
                    if ((boolean) scanSnapshot.getValue()) { //If a hunter has pressed the scan button
                        myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() { //Look at all user locations
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

        if(!globalPlayer.isHunter()) { //Only runners should have to update their hunter status to hunter once they are caught
            myRef.child("lobbies").child(LobbyChosen).child("users").child(globalPlayer.getName()).child("hunter").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot hunterSnapshot) {
                    if((boolean) hunterSnapshot.getValue() == true){ //If they are now seen as hunter on the database
                        globalPlayer.setHunter(true); //They are hunter on their device
                    }

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
    private void checkCaught(DataSnapshot snapshot) { //Checks if the runner and hunter are close enough to eachother to be considered caught

        Location myLocation = new Location(""); //Saves the hunter's current location
        myLocation.setLatitude(globalPlayer.getLatitude());
        myLocation.setLongitude(globalPlayer.getLongitude());

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if (!((boolean) dataSnapshot.child("hunter").getValue())) { //Hunters should only compare themselves to runners
                String playerName = dataSnapshot.getKey();

                Location playerLocation = new Location("");//Saves a runner's location
                playerLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                playerLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(playerLocation); //Compare the distance between the device hunter and some runner in the database

                System.out.println("- - - - - - - -- - - - - -- -" + globalPlayer.getName() + " is " + distanceInMeters + " meters away from " + playerName + "- - - - - - - -- - - - - -- -");


                if (distanceInMeters <= 10) { //If the runner is within 10 meters from a hunter
                    myRef.child("lobbies").child(LobbyChosen).child("users").child(playerName).child("hunter").setValue(true); //Convert the runner to a hunter
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

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
