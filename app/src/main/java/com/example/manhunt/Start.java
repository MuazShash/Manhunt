
package com.example.manhunt;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Start extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    GlobalPlayerClass globalPlayer;
    BroadcastReceiver myReceiver;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

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


        globalPlayer.startTheme(this);

        final Button JoinGame = (Button) findViewById(R.id.joinGame);
        final Button CreateGame = (Button) findViewById(R.id.createGame);
        final TextInputEditText usernameInput = (TextInputEditText) findViewById(R.id.NameTextInput);


        CreateGame.setOnClickListener(new View.OnClickListener() {
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

    @Override
    protected void onResume(){
        super.onResume();
        globalPlayer.resumeTheme();
    }

    @Override
    protected void onPause(){
        super.onPause();
        globalPlayer.pauseTheme();
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