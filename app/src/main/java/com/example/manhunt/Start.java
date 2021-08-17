
package com.example.manhunt;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Timer;
import java.util.TimerTask;


public class Start extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    GlobalPlayerClass globalPlayer;
    BroadcastReceiver myReceiver;
    WifiAwareManager mWifiAwareManager;
    LocationManager locationManager;
    public static final int locationRequest = 20;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        checkBackgroundLocationPermission();
        checkLocationPermission();
        globalPlayer = (GlobalPlayerClass) getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        System.out.println("**" + this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT));
        System.out.println("***" + this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onStart(){
        super.onStart();

        mWifiAwareManager = (WifiAwareManager) this.getSystemService(Context.WIFI_AWARE_SERVICE);

        IntentFilter filter = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);

        BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // discard current sessions
            }
        };

        this.registerReceiver(myReceiver, filter);
        /* This broadcast is not sticky, using the isAvailable()
         * API after registering the broadcast to check the current
         * state of Wi-Fi Aware. */
       // if (mWifiAwareManager.isAvailable()) {
         //   System.out.println("Wi-Fi Aware is available");
        //} else {
         //   System.out.println("Wi-Fi Aware NOT available!");
       // }


        globalPlayer.startTheme(this);

        final Button JoinGame = (Button) findViewById(R.id.joinGame);
        final Button CreateGame = (Button) findViewById(R.id.createGame);
        final TextInputEditText usernameInput = (TextInputEditText) findViewById(R.id.NameTextInput);



        CreateGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = usernameInput.getText().toString(); // storing username

                //if location permissions have not ben granted, the user will not be able to move forward in the app
                if (ContextCompat.checkSelfPermission(Start.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(Start.this, "Please allow location access to play Manhunt", Toast.LENGTH_SHORT).show();
                }

                // if username is blank, they need to make one before advancing
                if (username.equals("")) {
                    // popup asking for username
                    Toast.makeText(Start.this, "Please enter a username", Toast.LENGTH_SHORT).show();

                } else if (!username.equals("") && ContextCompat.checkSelfPermission(Start.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) { // once they have a username
                    // set username
                    globalPlayer.setName(username);
                    globalPlayer.setLeader(true); //setting them leader for creating the game
                    // display available lobbies
                    startActivity(new Intent(Start.this, CreateGamePopup.class));
                }
            }
        });

        JoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString(); // storing username

                // if username is blank, they need to make one before advancing
                if (username.equals("")) {

                    // popup asking for username
                    Toast.makeText(Start.this, "Please enter a username", Toast.LENGTH_SHORT).show();

                } else { // once they have a username

                    // set username
                    globalPlayer.setName(username);
                    globalPlayer.setLeader(false); // setting them non-leader for joining game
                    // display available lobbies
                    startActivity(new Intent(Start.this, ListofLobbies.class));
                }
            }
        });

    }
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage("Manhunt needs your location to continue")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(Start.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        locationRequest);

                            }
                        })
                        .create()
                        .show();


            }

            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        locationRequest);

            }
            return false;
        } else {
            return true;
        }

    }
    public boolean checkBackgroundLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Permission")
                        .setMessage("Manhunt needs your Background location to continue")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(Start.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, locationRequest);
                            }
                        })
                        .create()
                        .show();
                }

                else {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, locationRequest);
                }

            return false;

            }

         else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {
            case locationRequest: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {



                        //Request location updates:
                        //locationManager.requestLocationUpdates(provider, 400, 1, this);
                            globalPlayer.setLocationPermissions(true);

                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        globalPlayer.resumeTheme();
    }

    @Override
    protected void onPause(){
        super.onPause();
        globalPlayer.pauseTheme();
        //unregisterReceiver(myReceiver);
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();

            globalPlayer.stopTheme();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}