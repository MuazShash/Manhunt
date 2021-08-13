package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Process;
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
    MediaPlayer mpCaught, mpScan, mpDC, mpApproaching, mpBounds;
    LocationManager lm;
    private ValueEventListener dcListener, scanListener, usersListener, usersScanListener, hunterListener, startLocationListener;
    LocationListener locationListener;
    private SensorEventListener bearingListener;
    private SensorManager sensorManager;
    float[] mgravity = new float[3], mGeomagnetic = new float[3];
    float azimuth = 0f;

    Marker player;
    private Button scan, players, forfeit;
    private Location lastLocation = new Location("");
    boolean ready = false, inBound = true, gameEnd = false, booting = true, doubleBackToExitPressedOnce = false, updateMap;
    long startTime = System.currentTimeMillis(), warningTimer, runTime, cooldownTimer = System.currentTimeMillis(), caughtTimer = System.currentTimeMillis(), gameEndTime; //Stores information for round start and out of bounds timers
    double startLat, startLng;
    int zoom;

    private boolean quit = false;

    private final int TIME_ALIVE = 0;
    private final int RUNNERS_CAUGHT = 1;
    private final int CLOSE_CALLS = 2;

    private final int BOUNDARY = 0;
    private final int SCAN_COOLDOWN = 1;
    private final int CATCH_DIST = 2;
    private final int GAME_TIME_LIMIT = 4;
    private final int START_TIMER = 5;

    //broadcast receivers
    private BroadcastReceiver mReceiver;

    private long lastTouchTime = 0;
    private long currentTouchTime = 0;

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
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setNavigationBarColor(getResources().getColor(R.color.accent_2));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        scan = (Button) findViewById(R.id.btnScan); //scanner button for hunters
        players = (Button) findViewById(R.id.btnPlayers);
        forfeit = (Button) findViewById(R.id.btnForfeit);
        txtTimer = (TextView) findViewById(R.id.txtTimer);
        txtScan = (TextView) findViewById(R.id.txtScan);

        // application player object
        lobbyChosen = globalPlayer.getLobbyChosen();
        username = globalPlayer.getName();

        //Defining database reference location
        lobbyRef = database.getReference().child("lobbies").child(lobbyChosen);

        mpCaught = MediaPlayer.create(this, R.raw.caught_sound);
        mpCaught.setVolume(1.0f, 1.0f);
        mpScan = MediaPlayer.create(this, R.raw.scan_sound);
        mpScan.setVolume(1.0f, 1.0f);
        mpDC = MediaPlayer.create(this, R.raw.dc_sound);
        mpDC.setVolume(1.0f, 1.0f);
        mpBounds = MediaPlayer.create(this, R.raw.bounds);
        mpBounds.setVolume(1.0f, 1.0f);
        mpApproaching = MediaPlayer.create(this, R.raw.approaching_sound);


        Intent startIntent = new Intent(this, BackgroundLocationService.class);
        startIntent.setAction("start_service");
        startService(startIntent);


        //Setting zoom level for scan button
        if (globalPlayer.getSettings(BOUNDARY) <= 1000) {
            zoom = 15;
        } else {
            zoom = 13;
        }

        //Sets the start position to the leader's position when they press start game and updates the database
        if (globalPlayer.isLeader() && booting) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //Updating starting coordinates
                    if(booting){
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

        startLocationListener = new ValueEventListener(){
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(Double.parseDouble(String.valueOf(snapshot.child("startLat").getValue())) != 0.0 && Double.parseDouble(String.valueOf(snapshot.child("startLng").getValue())) != 0.0){
                        startLat = Double.parseDouble(String.valueOf(snapshot.child("startLat").getValue()));
                        startLng = Double.parseDouble(String.valueOf(snapshot.child("startLng").getValue()));
                        if(booting){
                            showBoundary();
                            booting = false;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
        };


        dcListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.getValue()) {
                    Intent backToStart = new Intent(getApplicationContext(), Start.class);
                    Toast.makeText(getApplicationContext(), "Leader has left the game!", Toast.LENGTH_SHORT).show();
                    mpDC.start();
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
                            showBoundary();
                            MarkLocation(snapshot); // redraw the map with new locations
                            mpScan.start();
                            updateMap = false;
                        } else if (updateMap && !globalPlayer.isHunter()) {
                            mpScan.start();
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
                    mpCaught.setVolume(0.1f, 0.1f);
                    mpCaught.start();
                    globalPlayer.setUserStat(TIME_ALIVE, (startTime - System.currentTimeMillis())/60000);
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
                player.setPosition(new LatLng(globalPlayer.getLatitude(),globalPlayer.getLongitude()));
            }
        };

        bearingListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float alpha = 0.97f;

                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mgravity[0] = alpha * mgravity[0] + (1 - alpha) * event.values[0];
                    mgravity[1] = alpha * mgravity[0] + (1 - alpha) * event.values[1];
                    mgravity[2] = alpha * mgravity[0] + (1 - alpha) * event.values[2];
                }
                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                    mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                    mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
                }

                float R[] = new float[9];
                float I[] = new float[9];

                boolean success = SensorManager.getRotationMatrix(R,I,mgravity,mGeomagnetic);
                if(success){
                    float[] orientation = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    azimuth = (float)Math.toDegrees(orientation[0]);
                    player.setRotation(azimuth);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

        //adding function to pending intent for the background location service
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //stopping the background service and finishing this activity
                quit = true;
                Intent backgroundServiceIntent = new Intent(Game.this, BackgroundLocationService.class);
                stopService(backgroundServiceIntent);
                finish();

            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("close_app");
        this.registerReceiver(mReceiver,filter);

        sensorManager.registerListener(bearingListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(bearingListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        //If statement to delete the lobby or just their user data from the database depending on if they are lobby leader or not
        if (globalPlayer.isLeader()) {
            lobbyRef.child("disconnected").onDisconnect().setValue(true);
            lobbyRef.onDisconnect().removeValue();
        } else {
            lobbyRef.child("users").child(globalPlayer.getName()).onDisconnect().removeValue();
            lobbyRef.addValueEventListener(startLocationListener);
        }

        // Move players back to start page if the disconnected attribute is true
        lobbyRef.child("disconnected").addValueEventListener(dcListener);

        //Update hunter view if converted
        if (globalPlayer.isHunter()) {
            showStatus("Hunter", Color.RED); //Updating user interface
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

                if(myLocation.distanceTo(startLocation) <= globalPlayer.getSettings(BOUNDARY) && ready){
                    warningTimer = System.currentTimeMillis();
                    inBound = true;
                    if(mpBounds.isPlaying()){
                        mpBounds.pause();
                    }
                }
                else if (ready){
                    inBound = false;
                    if(!mpBounds.isPlaying()){
                        mpBounds.start();
                    }
                    if(System.currentTimeMillis() - warningTimer >= (30*1000) && !globalPlayer.isHunter()){
                        showToast("You have been out of bounds for too long and have been turned into a hunter!");
                        lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").setValue(true); //Convert the runner to a hunter
                    }
                    else if (!globalPlayer.isHunter()){
                        txtTimer.setText("Return to game bounds in: " + (int) Math.floor(((30*1000) - (System.currentTimeMillis()-warningTimer))/1000) + "s");
                    }
                }

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
                        Intent backgroundServiceIntent = new Intent(Game.this, BackgroundLocationService.class);
                        stopService(backgroundServiceIntent);
                        startActivity(new Intent(Game.this, EndGame.class)); //sending users to the endgame screen
                        globalPlayer.setRunningInBackground(false);
                        finish(); //kills game activity
                    }
                } else {
                    gameEndTime = System.currentTimeMillis();
                }
                // Do your work here
                handler.postDelayed(this, delay);
            }
        }, delay);

        // Forfeit match button listener
        forfeit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastTouchTime = currentTouchTime;
                currentTouchTime = System.currentTimeMillis();
                Toast.makeText(Game.this, "Press again to forfeit", Toast.LENGTH_SHORT).show();
                if (currentTouchTime - lastTouchTime < 3000) {
                    startActivity(new Intent(Game.this, Start.class));
                    finish();
                    lastTouchTime = 0;
                    currentTouchTime = 0;
                }
            }
        });

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

        if(!globalPlayer.isRunningInBackground()) {
            //sensorManager.unregisterListener((SensorEventListener) this);
            lobbyRef.child("disconnected").removeEventListener(dcListener);
            lobbyRef.child("scan").removeEventListener(scanListener);
            lobbyRef.child("users").removeEventListener(usersListener);
            lobbyRef.child("users").child(username).child("hunter").removeEventListener(hunterListener);
            lobbyRef.child("users").removeEventListener(usersScanListener);
            if(!globalPlayer.isLeader()){
                lobbyRef.removeEventListener(startLocationListener);
            }
            handler.removeCallbacksAndMessages(null);
        }
    }

    protected void onStop() {
        super.onStop();

        if (globalPlayer.isLeader() && !gameEnd && !globalPlayer.isRunningInBackground() ) {
            lobbyRef.setValue(null);
        } else if (!globalPlayer.isLeader() && !gameEnd && !globalPlayer.isRunningInBackground()) {
            lobbyRef.child("users").child(globalPlayer.getName()).setValue(null);
        }

    }

    protected void onDestroy(){
        if(quit){
            Process.killProcess(Process.myPid());
        }
        super.onDestroy();

        if (globalPlayer.isLeader() && !gameEnd) {
            lobbyRef.setValue(null);
        } else if (!globalPlayer.isLeader()) {
            lobbyRef.child("users").child(globalPlayer.getName()).setValue(null);
        }

        Intent stopIntent = new Intent(this, BackgroundLocationService.class);
        stopIntent.setAction("stop_service");
        startService(stopIntent);

    }


    //Updates the map with markers of all player's locations
    private void MarkLocation(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())), Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            // draw hunters in blue, and runners in red
            if (!globalPlayer.getName().equals(dataSnapshot.getKey())) {
                if ((boolean) dataSnapshot.child("hunter").getValue()) { // hunters
                    mMap.addMarker(new MarkerOptions().position(PlayerLocation).title(dataSnapshot.getKey()).icon(BitmapDescriptorFactory.fromResource(R.drawable.hunter_icon_small)));
                } else { // runners
                    mMap.addMarker(new MarkerOptions().position(PlayerLocation).title(dataSnapshot.getKey()).icon(BitmapDescriptorFactory.fromResource(R.drawable.runner_icon_small)));
                }
            }
        }
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
    }



    //Draws a filled circle and moves the players camera and zoom centered at the start location
    private void showBoundary(){
        mMap.clear();
        LatLng startPosition = new LatLng(startLat, startLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, zoom));
        CircleOptions boundary = new CircleOptions().center(startPosition).radius(globalPlayer.getSettings(0)).strokeColor(Color.RED).fillColor(Color.argb(50, 200, 4, 4));
        mMap.addCircle(boundary);
        player = mMap.addMarker(new MarkerOptions().position(new LatLng(globalPlayer.getLatitude(), globalPlayer.getLongitude())).title("YOU").anchor(0.5f,0.5f).icon(BitmapDescriptorFactory.fromResource(R.drawable.position)));
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
        float shortestDistance = 9999f;

        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
            if (!((boolean) dataSnapshot.child("hunter").getValue()) && globalPlayer.isHunter()) { //Hunters should only compare themselves to runners
                String playerName = dataSnapshot.getKey();

                Location playerLocation = new Location("");//Saves a runner's location
                playerLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                playerLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(playerLocation); //Compare the distance between the device hunter and some runner in the database
                System.out.println("He is " + distanceInMeters + " meters away!");
                if(distanceInMeters < shortestDistance){
                    shortestDistance = distanceInMeters;
                }
                if (distanceInMeters <= 6) { //If the runner is within 10 meters from a hunter

                    CountDownTimer countDown = new CountDownTimer(5000, 2000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            showToast("They will turn into a hunter within " + Long.toString(millisUntilFinished/10) + " seconds");
                        }

                        @Override
                        public void onFinish() {
                            lobbyRef.child("users").child(playerName).child("hunter").setValue(true); //Convert the runner to a hunter
                            lobbyRef.child("users").child(playerName).child("caught").setValue(true); //Convert the runner to a hunter
                            // updating user's stats
                            globalPlayer.setUserStat(RUNNERS_CAUGHT, globalPlayer.getUserStat(RUNNERS_CAUGHT) + 1.0);
                        }
                    }.start();
                }

            } else if (((boolean) dataSnapshot.child("hunter").getValue()) && !globalPlayer.isHunter()) { // sound changes for runners
                Location hunterLocation = new Location("");//Saves a runner's location
                hunterLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                hunterLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(hunterLocation); //Compare the distance between the device hunter and some runner in the database
                System.out.println("He is " + distanceInMeters + " meters away!");
                if(distanceInMeters < shortestDistance){
                    shortestDistance = distanceInMeters;
                }
                if (!mpApproaching.isPlaying() && distanceInMeters > globalPlayer.getSettings(CATCH_DIST) && distanceInMeters < 40 && !mpScan.isPlaying()){ //If the runner is within 10 meters from a hunter
                    mpApproaching.start();
                    globalPlayer.setUserStat(CLOSE_CALLS, globalPlayer.getUserStat(CLOSE_CALLS) + 1.0);
                    mpApproaching.setVolume((float) 25/(distanceInMeters), (float) 25/(distanceInMeters));
                }
                else if (distanceInMeters > globalPlayer.getSettings(CATCH_DIST) && distanceInMeters < 40 && mpApproaching.isPlaying() && !mpScan.isPlaying()){
                    mpApproaching.setVolume((float) 25/(distanceInMeters), (float) 25/(distanceInMeters));
                }
                else if(mpScan.isPlaying() || distanceInMeters > 40 || distanceInMeters < globalPlayer.getSettings(2) || gameEnd || mpBounds.isPlaying()){
                    mpApproaching.pause();
                }
            }
        }
        if(shortestDistance < 50){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(Game.this, "ManhuntNotif");
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, builder.setContentTitle("Manhunt")
                    .setContentText("someone is " + (int) shortestDistance + " meters away...")
                    .setSmallIcon(R.drawable.m_icon_colorised3)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build());
        }
        else{
            NotificationCompat.Builder builder = new NotificationCompat.Builder(Game.this, "ManhuntNotif");
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, builder.setContentTitle("Manhunt")
                    .setContentText("There is no one nearby...")
                    .setSmallIcon(R.drawable.m_icon_colorised3)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .build());
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
            Intent backgroundServiceIntent = new Intent(this, BackgroundLocationService.class);
            stopService(backgroundServiceIntent);
            finish();
            return;
        }
        else if(doubleBackToExitPressedOnce && !globalPlayer.isLeader()){ //Otherwise just remove the user
            startActivity(new Intent(Game.this, Start.class));
            Intent backgroundServiceIntent = new Intent(this, BackgroundLocationService.class);
            stopService(backgroundServiceIntent);
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
    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }
}
