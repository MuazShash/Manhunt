package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Game extends FragmentActivity implements OnMapReadyCallback {

    //database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference lobbyRef;

    private GoogleMap mMap;
    private ActivityGameBinding binding;
    private FusedLocationProviderClient fusedLocationClient; // fused location provider client
    private String lobbyChosen, username;
    private GlobalPlayerClass globalPlayer;
    private TextView txtTimer, txtScan;
    final Handler handler = new Handler();
    MediaPlayer mpCaught, mpScan, mpDC, mpApproaching;
    AudioManager am;
    LocationManager lm;
    private ValueEventListener dcListener, scanListener, usersListener, usersScanListener, hunterListener;
    LocationListener locationListener;
    Marker player;
    private Button scan, players;
    private Location lastLocation = new Location("");
    boolean ready = false, inBound = true, gameEnd = false, booting = true, doubleBackToExitPressedOnce = false, updateMap, initialLocation = true, caught = false;
    long startTime = System.currentTimeMillis(), warningTimer, runTime, cooldownTimer = System.currentTimeMillis(), timeOfLastCatch = System.currentTimeMillis(), gameEndTime; //Stores information for round start and out of bounds timers
    double startLat, startLng, lastLat, lastLng; //Stores starting latitude and longitude
    int zoom;

    private final int DIST_TRAVELLED = 0;
    private final int MAX_SPEED = 1;
    private final int AVG_SPEED = 2;
    private final int TIME_ALIVE = 3;
    private final int RUNNERS_CAUGHT = 4;
    private final int FIRST_CATCH_TIME = 5;
    private final int QUICKEST_CATCH = 6;

    private final int BOUNDARY = 0;
    private final int SCAN_COOLDOWN = 1;
    private final int CATCH_DIST = 2;
    private final int GAME_TIME_LIMIT = 4;
    private final int START_TIMER = 5;

    /*************************************************************************************************************************************
     //**************************************************ON CREATE*************************************************************************
     This area handles the graphics and initializes values that will be used later on.
     //************************************************************************************************************************************/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lm = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setNavigationBarColor(getResources().getColor(R.color.accent_2));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        scan = (Button) findViewById(R.id.btnScan); //scanner button for hunters
        players = (Button) findViewById(R.id.btnPlayers);
        txtTimer = (TextView) findViewById(R.id.txtTimer);
        txtScan = (TextView) findViewById(R.id.txtScan);

        // application player object
        lobbyChosen = globalPlayer.getLobbyChosen();
        username = globalPlayer.getName();

        //Defining database reference location
        lobbyRef = database.getReference().child("lobbies").child(lobbyChosen);

        am = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        mpCaught = MediaPlayer.create(this, R.raw.caught_sound);
        mpCaught.setVolume(1.0f, 1.0f);
        //mpCaught.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpScan = MediaPlayer.create(this, R.raw.scan_sound);
        mpScan.setVolume(1.0f, 1.0f);
        //mpScan.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpDC = MediaPlayer.create(this, R.raw.dc_sound);
        mpDC.setVolume(1.0f, 1.0f);
        //mpDC.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpApproaching = MediaPlayer.create(this, R.raw.approaching_sound);
        //mpApproaching.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //Setting zoom level for scan button
        if (globalPlayer.getSettings(BOUNDARY) <= 1000) {
            zoom = 15;
        } else {
            zoom = 13;
        }

        if (globalPlayer.isHunter()) {
            caught = true;
        }

        // gets the initial location to use in player's stats
        if (initialLocation) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //Updating starting coordinates
                    lastLocation = location;

                    lastLat = location.getLatitude();
                    lastLng = location.getLongitude();

                    //Initialising location
                    lastLocation.setLatitude(lastLat);
                    lastLocation.setLongitude(lastLng);

                    initialLocation = false;
                }
            });
        }

        //Sets the start position to the leader's position when they press start game and updates the database
        if (globalPlayer.isLeader() && booting) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //Updating starting coordinates
                    startLat = location.getLatitude();
                    startLng = location.getLongitude();

                    //Updating database
                    lobbyRef.child("startLat").setValue(startLat);
                    lobbyRef.child("startLng").setValue(startLng);

                    globalPlayer.setLongitude(location.getLongitude());
                    globalPlayer.setLatitude(location.getLatitude());

                    lobbyRef.child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                    lobbyRef.child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());

                    showBoundary();
                    booting = false;
                }
            });
        } else if (!globalPlayer.isLeader() && booting) { //Reads the start position from the database for non leaders
            lobbyRef.child("startLat").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (task.isSuccessful()) {
                        startLat = Double.parseDouble(String.valueOf(task.getResult().getValue()));
                    }
                }
            });

            lobbyRef.child("startLng").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    startLng = Double.parseDouble(String.valueOf(task.getResult().getValue()));
                    fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            globalPlayer.setLongitude(location.getLongitude());
                            globalPlayer.setLatitude(location.getLatitude());

                            lobbyRef.child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                            lobbyRef.child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());
                        }
                    });
                    showBoundary();
                    booting = false;
                }
            });
        }

        // setting initial visibility of scan button and player status in top right
        if (globalPlayer.isHunter()) {
            ShowButton();
            showStatus("Hunter", Color.RED);
        } else {
            showStatus("Runner", Color.GREEN);
            hideButton();
        }
    }

    /*************************************************************************************************************************************
     //**************************************************ON START***************************************************************************
     This area creates the listeners needed to read from the database such as disconnect, scan, hunter status and other information.
     //************************************************************************************************************************************/
    protected void onStart() {
        super.onStart();

        //Declaring listeners
        dcListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.getValue()) {
                    Intent backToStart = new Intent(getApplicationContext(), Start.class);
                    Toast.makeText(getApplicationContext(), "Leader has left the game!", Toast.LENGTH_SHORT).show();
                    //am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.floor(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.6), 0);
                    dcSound();
                    startActivity(backToStart);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        scanListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot scanSnapshot) {
                updateMap = (boolean) scanSnapshot.getValue();
                lobbyRef.child("users").addValueEventListener(usersScanListener = new ValueEventListener() { //Look at all user locations
                    @Override
                    public void onDataChange(DataSnapshot snapshot) { // on data change of a runner's coordinates
                        if (updateMap && globalPlayer.isHunter()) {
                            mMap.clear(); // clear map
                            MarkLocation(snapshot); // redraw the map with new locations
                            //am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                            scanSound();
                            updateMap = false;
                        } else if (updateMap && !globalPlayer.isHunter()) {
                            //mVolume(AudioManager.STREAM_MUSIC, (int) Math.floor(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.6), 0);
                            scanSound();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        hunterListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot hunterSnapshot) {
                //  Block of code to try
                if ((boolean) hunterSnapshot.getValue()) { //If they are now seen as hunter on the database
                    globalPlayer.setHunter(true); //They are hunter on their device
                    //am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5), 0);
                    mpCaught.setVolume(0.1f, 0.1f);
                    caughtSound();
                    showToast("You have been caught by a hunter!");
                    showStatus("hunter", Color.RED);
                    ShowButton();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) { // on data change of a player's coordinates
                if (ready) { //Checks if the start timer is complete
                    checkCaught(snapshot); //Checks if the runner/hunter are close enough to each other
                    if (runnersCaught(snapshot)) {
                        txtTimer.setText("All runners have been caught! Hunters win!");
                        gameEnd = true;

                        globalPlayer.setUserStat(AVG_SPEED, globalPlayer.getUserStat(DIST_TRAVELLED) / (System.currentTimeMillis() - startTime) / 1000.0); //userStats[AVG_SPEED] = userStats[DIST_TRAVELLED] / ((System.currentTimeMillis() - startTime) / 1000.0);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                globalPlayer.setLongitude(location.getLongitude());
                globalPlayer.setLatitude(location.getLatitude());

                lobbyRef.child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                lobbyRef.child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());

                System.out.println("*******************LKocation listener is working");
            }
        };
    }

    /*************************************************************************************************************************************
     **************************************************ON RESUME***************************************************************************
     * This method handles all the functions required to run the game such as timers, checking if the game is over, if players are in range
     * of being caught and reading and writing to the database to update their information.
     *************************************************************************************************************************************/
    @Override
    protected void onResume() {
        super.onResume();

        //If statement to delete the lobby or just their user data from the database depending on if they are lobby leader or not
        if (globalPlayer.isLeader()) {
            lobbyRef.child("disconnected").onDisconnect().setValue(true);
            lobbyRef.onDisconnect().removeValue();
        } else {
            lobbyRef.child("users").child(globalPlayer.getName()).onDisconnect().removeValue();
        }

        // Move players back to start page if the disconnected attribute is true
        lobbyRef.child("disconnected").addValueEventListener(dcListener);

        //Update hunter view if converted
        if (globalPlayer.isHunter()) {
            showStatus("Hunter", Color.RED); //Updating user interface
        }

        //Updating user stat time alive
        if (globalPlayer.isHunter() && !caught) {
            if (System.currentTimeMillis() - startTime > globalPlayer.getSettings(START_TIMER) * 1000) // only considers catches made after the round start timer
            {
                globalPlayer.setUserStat(TIME_ALIVE, System.currentTimeMillis() - startTime);
            }

            caught = true;
        }

        lobbyRef.child("users").addValueEventListener(usersListener);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

        lm.requestLocationUpdates(lm.getBestProvider(criteria, true), (long) 1000, (float) 0.25, locationListener);

        /* * * * * * * * * * * * * * * */
        // Handler for updating coordinates in real time below

        // constants
        final int delay = 200;

        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            public void run() {
                //Updates the database with the user's current location

                //Checks if the user is out of bounds
                Location myLocation = new Location("");
                myLocation.setLatitude(globalPlayer.getLatitude());
                myLocation.setLongitude(globalPlayer.getLongitude());

                Location startLocation = new Location("");
                startLocation.setLatitude(startLat);
                startLocation.setLongitude(startLng);

                //mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(180)).position(new LatLng(globalPlayer.getLatitude(), globalPlayer.getLongitude())).title("YOU"));


                if(myLocation.distanceTo(startLocation) <= globalPlayer.getSettings(BOUNDARY)){
                    warningTimer = System.currentTimeMillis();
                    inBound = true;
                }
                else{
                    inBound = false;
                    if(System.currentTimeMillis() - warningTimer >= (30*1000) && !globalPlayer.isHunter()){
                        showToast("You have been out of bounds for too long and have been turned into a hunter!");
                        lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").setValue(true); //Convert the runner to a hunter
                    }
                    else if (!globalPlayer.isHunter()){
                        txtTimer.setText("Return to game bounds in: " + (int) Math.floor(((30*1000) - (System.currentTimeMillis()-warningTimer))/1000) + "s");
                    }
                }

                        // updates the player's stats
                        float distanceTravelled = myLocation.distanceTo(lastLocation);

                        globalPlayer.setUserStat(DIST_TRAVELLED, globalPlayer.getUserStat(DIST_TRAVELLED) + distanceTravelled);

                        if ((distanceTravelled / delay) > globalPlayer.getUserStat(MAX_SPEED)) {
                            globalPlayer.setUserStat(MAX_SPEED, distanceTravelled / delay);
                        }
                        lastLocation.set(myLocation);

                //checks if the game is ready to start
                if (System.currentTimeMillis() - startTime >= globalPlayer.getSettings(START_TIMER) * 1000 && !ready) { //Checks if the start timer is complete
                    ready = true;
                    runTime = System.currentTimeMillis();
                } else if (!ready) {
                    txtTimer.setText("Round starts in: " + (int) Math.floor((globalPlayer.getSettings(START_TIMER) * 1000 - (System.currentTimeMillis() - startTime)) / 1000) + "s");
                }

                //checks if the game has reached the end of its time
                if (!gameEnd && ready && inBound && (globalPlayer.getSettings(GAME_TIME_LIMIT) * (60 * 1000) - (System.currentTimeMillis() - runTime)) / 1000 < 300 && (globalPlayer.getSettings(GAME_TIME_LIMIT) * 1000 - (System.currentTimeMillis() - runTime)) / 1000 > 0) {
                    txtTimer.setText("Round ends in: " + (int) Math.floor((globalPlayer.getSettings(GAME_TIME_LIMIT) * (60 * 1000) - (System.currentTimeMillis() - runTime)) / 1000) + "s");
                } else if (!gameEnd && ready && inBound && (globalPlayer.getSettings(GAME_TIME_LIMIT) * (60 * 1000) - (System.currentTimeMillis() - runTime)) / 1000 > 300) {
                    txtTimer.setText("Round ends in: " + (int) Math.floor((globalPlayer.getSettings(GAME_TIME_LIMIT) * (60 * 1000) - (System.currentTimeMillis() - runTime)) / (60 * 1000)) + " mins");
                } else if (!gameEnd && ready && (globalPlayer.getSettings(GAME_TIME_LIMIT) * (60 * 1000) - (System.currentTimeMillis() - runTime)) < 0) {
                    globalPlayer.setHunterWins(false);
                    txtTimer.setText("Hunters failed to catch all runners in time! Runners win!");
                    gameEnd = true;

                    globalPlayer.setUserStat(AVG_SPEED, globalPlayer.getUserStat(DIST_TRAVELLED) / ((System.currentTimeMillis() - startTime) / 1000.0));
                }

                //Button enabled/disabled on cooldown
                if (System.currentTimeMillis() - cooldownTimer > globalPlayer.getSettings(SCAN_COOLDOWN) * 1000 && globalPlayer.isHunter() && ready) {
                    scan.setEnabled(true);
                    txtScan.setText("");
                } else if (globalPlayer.isHunter()) {
                    scan.setEnabled(false);
                    txtScan.setText(globalPlayer.getSettings(SCAN_COOLDOWN) - (System.currentTimeMillis() - cooldownTimer) / 1000 + "s");
                } else if (!ready) {
                    scan.setEnabled(false);
                    txtScan.setText("");
                }

                if (gameEnd) {
                    if (System.currentTimeMillis() - gameEndTime > (7 * 1000)) {
                        startActivity(new Intent(Game.this, EndGame.class)); //sending users to the endgame screen
                        finish(); //kills game activity
                    }
                } else {
                    gameEndTime = System.currentTimeMillis();
                }
                // Do your work here
                handler.postDelayed(this, delay);
            }
        }, delay);

        // Listener for scan button when clicked by hunters
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lobbyRef.child("scan").setValue(true); //sets scan object to true now the locations of the runners become available
                cooldownTimer = System.currentTimeMillis();
            }
        });

        //Listener for player button when clicked
        players.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Game.this, ListOfPlayers.class));
            }
        });

        lobbyRef.child("scan").addValueEventListener(scanListener); //Update the map with user locations for hunters and plays the sound for all when this is true

        if (!globalPlayer.isHunter()) { //Only runners should have to update their hunter status to hunter once they are caught
            lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").addValueEventListener(hunterListener); //Update their status as hunter in game if they are hunter on the database
        }

    }

    /*************************************************************************************************************************************
     //**************************************************ON PAUSE AND ON STOP**************************************************************

     These methods are called when the activity is getting ready to close. Removes listeners and deletes the lobby from the database if they are a leader
     or deletes their user from the lobby if they are not.
     *************************************************************************************************************************************/
    protected void onPause() {
        super.onPause();
        lobbyRef.child("disconnected").removeEventListener(dcListener);
        lobbyRef.child("scan").removeEventListener(scanListener);
        lobbyRef.child("users").removeEventListener(usersListener);
        lobbyRef.child("users").child(username).child("hunter").removeEventListener(hunterListener);
        lobbyRef.child("users").removeEventListener(usersScanListener);
        handler.removeCallbacksAndMessages(null);
    }

    protected void onStop() {
        super.onStop();
        if (globalPlayer.isLeader() && !gameEnd) {
            lobbyRef.setValue(null);
        } else if (!globalPlayer.isLeader() && !gameEnd) {
            lobbyRef.child("users").child(globalPlayer.getName()).setValue(null);
        }

    }

    //Updates the map with markers of all player's locations
    private void MarkLocation(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())), Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            // draw hunters in blue, and runners in red
            if (!globalPlayer.getName().equals(dataSnapshot.getKey())) {
                if ((boolean) dataSnapshot.child("hunter").getValue()) { // hunters
                    mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(180)).position(PlayerLocation).title(dataSnapshot.getKey()));
                } else { // runners
                    mMap.addMarker(new MarkerOptions().position(PlayerLocation).title(dataSnapshot.getKey()));
                }
            }
        }
        showBoundary();
        lobbyRef.child("scan").setValue(false);
    }

    //Initializes the map and any features
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //player = mMap.addMarker(new MarkerOptions().position(new LatLng(globalPlayer.getLatitude(), globalPlayer.getLongitude())).title("YOU"));
        mMap.setMyLocationEnabled(true);
    }

    private void caughtSound(){
        mpCaught.start();
    }

    private void scanSound(){
        mpScan.start();
    }

    private void dcSound(){
        mpDC.start();
    }

    private void approachingSound(){
        mpApproaching.start();
    }



    //Draws a filled circle and moves the players camera and zoom centered at the start location
    private void showBoundary(){
        LatLng startPosition = new LatLng(startLat, startLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, zoom));
        CircleOptions boundary = new CircleOptions().center(startPosition).radius(globalPlayer.getSettings(0)).strokeColor(Color.RED).fillColor(Color.argb(50, 200, 4, 4));
        mMap.addCircle(boundary);
    }

    //Updates the user's status as hunter or runner
    private void showStatus(String type, int color) { // textview in top right showing the player status (hunter)
        TextView txtPlayerStatus = (TextView) findViewById(R.id.txtPlayerStatus);

        txtPlayerStatus.setTextColor(color); // colour change for visibility
        txtPlayerStatus.setText(type);
    }

    //Reveals the button to scan players
    private void ShowButton() {
        // scan button visibility
        View StartVisibility = findViewById(R.id.btnScan);
        StartVisibility.setVisibility(View.VISIBLE);
    }

    //Hides the button to scan players
    private void hideButton() {
        // scan button visibility
        View StartVisibility = findViewById(R.id.btnScan);
        StartVisibility.setVisibility(View.INVISIBLE);
    }

    //Checks if the runner and hunter are close enough to each other to be considered caught. This is called every time a user moves in the database
    private void checkCaught(DataSnapshot snapshot) {

        Location myLocation = new Location(""); //Saves the hunter's current location
        myLocation.setLatitude(globalPlayer.getLatitude());
        myLocation.setLongitude(globalPlayer.getLongitude());

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if (!((boolean) dataSnapshot.child("hunter").getValue()) && globalPlayer.isHunter()) { //Hunters should only compare themselves to runners
                String playerName = dataSnapshot.getKey();

                Location playerLocation = new Location("");//Saves a runner's location
                playerLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                playerLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(playerLocation); //Compare the distance between the device hunter and some runner in the database

                if (distanceInMeters <= globalPlayer.getSettings(CATCH_DIST)) { //If the runner is within 10 meters from a hunter
                    lobbyRef.child("users").child(playerName).child("hunter").setValue(true); //Convert the runner to a hunter
                    lobbyRef.child("users").child(playerName).child("caught").setValue(true); //Convert the runner to a hunter

                    // updating user's stats
                    globalPlayer.setUserStat(RUNNERS_CAUGHT, globalPlayer.getUserStat(RUNNERS_CAUGHT) + 1.0);

                    if (globalPlayer.getUserStat(FIRST_CATCH_TIME) == 0.0) {
                        globalPlayer.setUserStat(QUICKEST_CATCH, System.currentTimeMillis() - globalPlayer.getUserStat(TIME_ALIVE));
                        globalPlayer.setUserStat(FIRST_CATCH_TIME, System.currentTimeMillis() - globalPlayer.getUserStat(TIME_ALIVE)); // not sure if startTime should be subtracted, tentative
                    } else if (System.currentTimeMillis() - timeOfLastCatch < globalPlayer.getUserStat(QUICKEST_CATCH)) {
                        globalPlayer.setUserStat(QUICKEST_CATCH, System.currentTimeMillis() - timeOfLastCatch);
                    }

                    timeOfLastCatch = System.currentTimeMillis();
                }

            } else if (((boolean) dataSnapshot.child("hunter").getValue()) && !globalPlayer.isHunter()) { // sound changes for runners
                Location hunterLocation = new Location("");//Saves a runner's location
                hunterLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                hunterLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(hunterLocation); //Compare the distance between the device hunter and some runner in the database
                System.out.println("***********This person is " + distanceInMeters + " meters away");
                if (!mpApproaching.isPlaying() && distanceInMeters > globalPlayer.getSettings(CATCH_DIST) && !mpScan.isPlaying()){ //If the runner is within 10 meters from a hunter
                    approachingSound();
                    //am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1)), 0);
                    mpApproaching.setVolume((float) am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1), (float) am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1));
                }
                else if (distanceInMeters > globalPlayer.getSettings(CATCH_DIST) && mpApproaching.isPlaying() && !mpScan.isPlaying()){
                    //am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1)), 0);
                    mpApproaching.setVolume((float) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1)), (float) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters/15+1)));
                }
                else if(mpScan.isPlaying() || distanceInMeters > 40 || distanceInMeters < globalPlayer.getSettings(2) || gameEnd){
                    mpApproaching.pause();
                }
            }
        }
    }

    //Checks if there are any runners remaining signaling a game end. Called whenever a user moves in the database.
    private boolean runnersCaught(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if (!((boolean) dataSnapshot.child("hunter").getValue())) { //Hunters should only compare themselves to runners
                return false;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce && globalPlayer.isLeader()) { //If the user is the leader and is leaving, prepare to close the game
            lobbyRef.child("disconnected").setValue(true);
            startActivity(new Intent(Game.this, Start.class));
            finish();
            return;
        }
        else if(doubleBackToExitPressedOnce && !globalPlayer.isLeader()){ //Otherwise just remove the user
            startActivity(new Intent(Game.this, Start.class));
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to leave the game", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, (2*1000));    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
