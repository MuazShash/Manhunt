package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.example.manhunt.databinding.ActivityGameBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
    String LobbyChosen;
    GlobalPlayerClass globalPlayer;

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

        // application player object
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        LobbyChosen = globalPlayer.getLobbychosen();

        //scanner button for hunters
        final Button scan = (Button) findViewById(R.id.btnScan);

        //limiting scan button visibility to only hunters
        View StartVisibility = findViewById(R.id.btnScan);
        if (!globalPlayer.isHunter()) {
            StartVisibility.setVisibility(View.GONE);
        }

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("scan").setValue(true); //sets scan object to true now the locations of the runners become available
                myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        mMap.clear();
                        MarkLocation(snapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });

            }
        });

        final Handler handler = new Handler();
        final int delay = 2000;

        handler.postDelayed(new Runnable() {
            public void run() {
                System.out.println("myHandler: here!");
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

    private void MarkLocation(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if((boolean) dataSnapshot.child("hunter").getValue() != true){
                LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())) , Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));
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
}