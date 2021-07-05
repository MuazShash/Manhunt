package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
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
    private Runnable myRunnable;
    MediaPlayer mpCaught, mpScan, mpDC, mpApproaching;
    AudioManager am;
    private ValueEventListener dcListener, scanListener, usersListener, usersScanListener, hunterListener;
    private Button scan, players;
    boolean ready = false, inBound = true, gameEnd = false, booting = true, doubleBackToExitPressedOnce = false, updateMap;
    long startTime = System.currentTimeMillis(), warningTimer = System.currentTimeMillis(), runTime, cooldownTimer = System.currentTimeMillis(); //Stores information for round start and out of bounds timers
    double startLat, startLng; //Stores starting latitude and longitude
    int zoom;

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
        //Setting zoom level for scan button
        if(globalPlayer.getSettings(0) <= 1000){
            zoom = 15;
        }
        else{
            zoom = 13;
        }

        //Sets the start position to the leader's position when they press start game and updates the database
        if(globalPlayer.isLeader() && booting){
            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //Updating starting coordinates
                    startLat = location.getLatitude();
                    startLng = location.getLongitude();
                    LatLng startPosition = new LatLng(startLat, startLng);

                    //Updating database
                    lobbyRef.child("startLat").setValue(startLat);
                    lobbyRef.child("startLng").setValue(startLng);

                    showBoundary();
                    booting = false;
                }
            });
        }
        else if (!globalPlayer.isLeader() && booting){ //Reads the start position from the database for non leaders
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
                        showBoundary();
                        booting = false;
                }
            });
        }

        // setting initial visibility of scan button and player status in top right
        if (globalPlayer.isHunter()) {
            ShowButton();
            showStatus("Hunter", Color.RED);
        }
        else {
            showStatus("Runner", Color.GREEN);
            hideButton();
        }
    }
/*************************************************************************************************************************************
//**************************************************ON START***************************************************************************
 This area creates the listeners needed to read from the database such as disconnect, scan, hunter status and other information.
//************************************************************************************************************************************/
    protected void onStart(){
        super.onStart();

        //Declaring listeners
        dcListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.getValue()) {
                    Intent backToStart = new Intent(getApplicationContext(), Start.class);
                    Toast.makeText(getApplicationContext(), "Leader has left the game!", Toast.LENGTH_SHORT).show();
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    dcSound();
                    startActivity(backToStart);
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
                        if(updateMap && globalPlayer.isHunter()){
                            mMap.clear(); // clear map
                            MarkLocation(snapshot); // redraw the map with new locations
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                            scanSound();
                            updateMap = false;
                        }
                        else if(updateMap && !globalPlayer.isHunter()){
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                            scanSound();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });

            }
            @Override
            public void onCancelled(DatabaseError error) {}
        };

        hunterListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot hunterSnapshot) {
                //  Block of code to try
                if((boolean) hunterSnapshot.getValue() == true){ //If they are now seen as hunter on the database
                    globalPlayer.setHunter(true); //They are hunter on their device
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5), 0);
                    caughtSound();
                    showToast("You have been caught by a hunter!");
                    showStatus("hunter", Color.RED);
                    ShowButton();
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) { // on data change of a player's coordinates
                if(ready){ //Checks if the start timer is complete
                    checkCaught(snapshot); //Checks if the runner/hunter are close enough to each other
                    if(runnersCaught(snapshot)){
                        txtTimer.setText("All runners have been caught! Hunters win!");
                        gameEnd = true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
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
        }
        else {
            lobbyRef.child("users").child(globalPlayer.getName()).onDisconnect().removeValue();
        }

        // Move players back to start page if the disconnected attribute is true
        lobbyRef.child("disconnected").addValueEventListener(dcListener);

        //Update hunter view if converted
        if (globalPlayer.isHunter()) {
            showStatus("Hunter", Color.RED); //Updating user interface
        }

        lobbyRef.child("users").addValueEventListener(usersListener);

        /* * * * * * * * * * * * * * * */
        // Handler for updating coordinates in real time below

        // constants
        final int delay = 200;

        handler.postDelayed(myRunnable = new Runnable() {
            @SuppressLint("MissingPermission")
            public void run() {
                //Updates the database with the user's current location
                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {

                    @Override
                    public void onSuccess(Location location) {

                        globalPlayer.setLongitude(location.getLongitude());
                        globalPlayer.setLatitude(location.getLatitude());

                        lobbyRef.child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                        lobbyRef.child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());

                        //Checks if the user is out of bounds
                        Location myLocation = new Location("");
                        myLocation.setLatitude(globalPlayer.getLatitude());
                        myLocation.setLongitude(globalPlayer.getLongitude());

                        Location startLocation = new Location("");
                        startLocation.setLatitude(startLat);
                        startLocation.setLongitude(startLng);

                        if(myLocation.distanceTo(startLocation) <= globalPlayer.getSettings(0)){
                            warningTimer = System.currentTimeMillis();
                            inBound = true;
                        }
                        else{
                            inBound = false;
                            if(System.currentTimeMillis() - warningTimer >= 10000 && !globalPlayer.isHunter()){
                                showToast("You have been out of bounds for too long and have been turned into a hunter!");
                                lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").setValue(true); //Convert the runner to a hunter
                            }
                            else if (!globalPlayer.isHunter()){
                                txtTimer.setText("Return to game bounds in: " + (int) Math.floor((10000 - (System.currentTimeMillis()-warningTimer))/1000) + "s");
                            }
                        }
                    }
                });

                //checks if the game is ready to start
                if(System.currentTimeMillis() - startTime >= globalPlayer.getSettings(5)*1000 && !ready){ //Checks if the start timer is complete
                    ready = true;
                    runTime = System.currentTimeMillis();
                }
                else if(!ready){
                    txtTimer.setText("Round starts in: " + (int) Math.floor((globalPlayer.getSettings(5)*1000 - (System.currentTimeMillis() - startTime))/1000) + "s");
                }

                //checks if the game has reached the end of its time
                if(!gameEnd && ready && inBound && (globalPlayer.getSettings(4)*60000- (System.currentTimeMillis() - runTime))/1000 < 300 && (globalPlayer.getSettings(4)*1000- (System.currentTimeMillis() - runTime))/1000 > 0){
                    txtTimer.setText("Round ends in: " + (int) Math.floor((globalPlayer.getSettings(4)*60000- (System.currentTimeMillis() - runTime))/1000) + "s");
                }
                else if (!gameEnd && ready && inBound && (globalPlayer.getSettings(4)*60000- (System.currentTimeMillis() - runTime))/1000 > 300){
                    txtTimer.setText("Round ends in: " + (int) Math.floor((globalPlayer.getSettings(4)*60000- (System.currentTimeMillis() - runTime))/60000) + " mins");
                }
                else if(!gameEnd && ready && (globalPlayer.getSettings(4)*60000- (System.currentTimeMillis() - runTime)) < 0){
                    txtTimer.setText("Hunters failed to catch all runners in time! Runners win!");
                    gameEnd = true;
                }

                //Button enabled/disabled on cooldown
                if(System.currentTimeMillis() - cooldownTimer > globalPlayer.getSettings(1)*1000 && globalPlayer.isHunter()) {
                    scan.setEnabled(true);
                    txtScan.setText("");
                }
                else if (globalPlayer.isHunter()){
                    scan.setEnabled(false);
                    txtScan.setText(globalPlayer.getSettings(1) - (System.currentTimeMillis() - cooldownTimer)/1000 + "s");
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

        if(!globalPlayer.isHunter()) { //Only runners should have to update their hunter status to hunter once they are caught
            lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").addValueEventListener(hunterListener); //Update their status as hunter in game if they are hunter on the database
        }
    }

/*************************************************************************************************************************************
//**************************************************ON PAUSE AND ON STOP**************************************************************

 These methods are called when the activity is getting ready to close. Removes listeners and deletes the lobby from the database if they are a leader
 or deletes their user from the lobby if they are not.
*************************************************************************************************************************************/
    protected void onPause(){
        super.onPause();
        lobbyRef.child("disconnected").removeEventListener(dcListener);
        lobbyRef.child("scan").removeEventListener(scanListener);
        lobbyRef.child("users").removeEventListener(usersListener);
        lobbyRef.child("users").child(username).child("hunter").removeEventListener(hunterListener);
        lobbyRef.child("users").removeEventListener(usersScanListener);
        handler.removeCallbacks(myRunnable);
    }

    protected void onStop(){
        super.onStop();
        if(globalPlayer.isLeader()) {
            lobbyRef.setValue(null);
        }
        else{
            lobbyRef.child("users").setValue(null);
        }

    }

    //Updates the map with markers of all player's locations
    private void MarkLocation(DataSnapshot snapshot) {
        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

            LatLng PlayerLocation = new LatLng(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())) , Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

            // draw hunters in blue, and runners in red
            if(!globalPlayer.getName().equals(dataSnapshot.getKey())){
                if((boolean) dataSnapshot.child("hunter").getValue()){ // hunters
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
        mMap.setMyLocationEnabled(true);
    }

    private void caughtSound(){
        mpCaught = MediaPlayer.create(this, R.raw.caught_sound);
        mpCaught.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpCaught.start();
    }

    private void scanSound(){
        mpScan = MediaPlayer.create(this, R.raw.scan_sound);
        mpScan.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpScan.start();
    }

    private void dcSound(){
        mpDC= MediaPlayer.create(this, R.raw.dc_sound);
        mpDC.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpDC.start();
    }

    private void approachingSound(){
        mpApproaching = MediaPlayer.create(this, R.raw.approaching_sound);
        mpApproaching.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mpApproaching.start();
    }

    //Draws a filled circle and moves the players camera and zoom centered at the start location
    private void showBoundary(){
        LatLng startPosition = new LatLng(startLat, startLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, zoom));
        CircleOptions boundary = new CircleOptions().center(startPosition).radius(globalPlayer.getSettings(0)).strokeColor(Color.RED).fillColor(Color.argb(50,200,4,4));
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

                if (distanceInMeters <= globalPlayer.getSettings(2)) { //If the runner is within 10 meters from a hunter
                    lobbyRef.child("users").child(playerName).child("hunter").setValue(true); //Convert the runner to a hunter
                }

            }
            else if(((boolean) dataSnapshot.child("hunter").getValue()) && !globalPlayer.isHunter()){
                Location hunterLocation = new Location("");//Saves a runner's location
                hunterLocation.setLatitude(Double.parseDouble(String.valueOf(dataSnapshot.child("latitude").getValue())));
                hunterLocation.setLongitude(Double.parseDouble(String.valueOf(dataSnapshot.child("longitude").getValue())));

                float distanceInMeters = myLocation.distanceTo(hunterLocation); //Compare the distance between the device hunter and some runner in the database

                if (distanceInMeters <= 30 && distanceInMeters > globalPlayer.getSettings(2) && !mpApproaching.isPlaying()) { //If the runner is within 10 meters from a hunter
                    approachingSound();
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.5), 0);
                }
                else if(distanceInMeters > 30 || distanceInMeters < globalPlayer.getSettings(2) && mpApproaching.isPlaying()){
                    mpApproaching.stop();
                }
                else if (distanceInMeters < 30 && distanceInMeters > globalPlayer.getSettings(2) && mpApproaching.isPlaying()){
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.ceil(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*1/(distanceInMeters+1)), 0);
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
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
