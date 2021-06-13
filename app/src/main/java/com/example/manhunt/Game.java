package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
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
    private double lat, lon;

    private final boolean HUNTER = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

         globalPlayer = (GlobalPlayerClass) getApplicationContext();

        // application player object
        LobbyChosen = globalPlayer.getLobbychosen();
        username = globalPlayer.getName();


        //scanner button for hunters
        final Button scan = (Button) findViewById(R.id.btnScan);

        // setting initial visibility of scan button and player status in top right
        ShowButton();
        ShowStatus();

        // Listener for scan button when clicked by hunters
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //myRef.child("lobbies").child(LobbyChosen).child("scan").setValue(true); //sets scan object to true now the locations of the runners become available
                myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // on data change of a runner's coordinates
                        mMap.clear(); // clear map
                        MarkLocation(snapshot); // redraw the map with new locations
                        checkCaught(snapshot, globalPlayer);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });

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

                        lat = (Double) globalPlayer.getLatitude();
                        lon = (Double) globalPlayer.getLongitude();

                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("latitude").setValue(lat);
                        myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("longitude").setValue(lon);

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
        //myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("scan").setValue(false);
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

    private void ShowStatus() { // textview in top right showing the player status (hunter)
        TextView txtPlayerStatus = (TextView) findViewById(R.id.txtPlayerStatus);

            txtPlayerStatus.setTextColor(Color.RED); // colour change for visibility
            txtPlayerStatus.setText("Hunter");
    }

    private void ShowButton() {
        // scan button visibility
        View StartVisibility = findViewById(R.id.btnScan);

        StartVisibility.setVisibility(View.VISIBLE);
    }

    private void checkCaught(DataSnapshot snapshot, GlobalPlayerClass gp) {

        Location myLocation = new Location(""); // my location, using
        myLocation.setLatitude(gp.getLatitude());
        myLocation.setLongitude(gp.getLongitude());

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            String playerName = dataSnapshot.getKey();

            Location playerLocation = new Location("");// looping through the other player locations
            playerLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
            playerLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            float distanceInMeters =  myLocation.distanceTo(playerLocation); // distance to the other players

            if (distanceInMeters <= 10) { // people within 10 meters
                if(!((boolean) dataSnapshot.child("hunter").getValue())) { // if they're a runner
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