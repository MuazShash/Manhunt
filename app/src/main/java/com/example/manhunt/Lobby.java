package com.example.manhunt;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.security.cert.PKIXRevocationChecker;
import java.util.ArrayList;

public class Lobby extends AppCompatActivity {

    //database reference
    DatabaseReference lobbyRef;
    ValueEventListener dcListener, startListener, usersListener;

    // Write a string when this client loses connection
    ListView listOfPlayers;
    GlobalPlayerClass globalPlayer;
    String lobbyChosen;
    boolean zeroHunters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        getWindow().setNavigationBarColor(getResources().getColor(R.color.accent_2));

        // getting global variables to check which lobby was chosen
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        lobbyChosen = globalPlayer.getLobbyChosen();
        lobbyRef = FirebaseDatabase.getInstance().getReference().child("lobbies").child(lobbyChosen);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // hunter, runner and start button IDs
        final Button Hunter = (Button) findViewById(R.id.selectHunter);
        final Button Runner = (Button) findViewById(R.id.selectRunner);
        final TextView Status = (TextView) findViewById(R.id.lobbyView2);
        final Button Start = (Button) findViewById(R.id.btnStart);
        final ImageButton button = findViewById(R.id.settings);

        //If statement to delete the lobby or just their user data from the database depending on if they are lobby leader or not
        if (globalPlayer.isLeader()) {
            lobbyRef.child("disconnected").onDisconnect().setValue(true);
            lobbyRef.onDisconnect().removeValue();
        } else {
            lobbyRef.child("users").child(globalPlayer.getName()).onDisconnect().removeValue();
        }

        // Updating listview of players in the lobby
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ShowPlayers(dataSnapshot);
                zeroHunters = countHunters(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        // Move players back to start page
        dcListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.getValue()) {
                    Intent backToStart = new Intent(getApplicationContext(), Start.class);
                    showToast("Leader has left the game!");
                    startActivity(backToStart);
                    globalPlayer.stopTheme();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        //putting every user into the game now
        startListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if ((boolean) snapshot.getValue()) {
                    if(zeroHunters) {
                        showToast("Minimum 1 hunter required to start game");
                    } else {
                        lobbyRef.child("settings").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                int i = 0;
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    globalPlayer.setSettings(i++, Integer.parseInt(String.valueOf(dataSnapshot.getValue())));
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                            }
                        });
                        startActivity(new Intent(Lobby.this, Game.class)); //open maps game activity
                        globalPlayer.stopTheme();
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        };

        Hunter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // on click of hunter

                // set the user to be hunter (hunter = true)
                lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").setValue(true);
                globalPlayer.setHunter(true);
                Status.setText("You are a: Hunter");
            }
        });

        Runner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // on click of runner

                // set the user to be runner (hunter = false)
                lobbyRef.child("users").child(globalPlayer.getName()).child("hunter").setValue(false);
                globalPlayer.setHunter(false);
                Status.setText("You are a: Runner");
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (globalPlayer.isLeader()) { // only the leader can change game settings
                    Intent OpenOptions = new Intent(getApplicationContext(), Options.class);
                    startActivity(OpenOptions); // opens settings page
                } else {
                    showToast("Only the host can change the game settings");
                }
            }
        });



        //limiting start button visibility to only lobby leader
        View StartVisibility = findViewById(R.id.btnStart);
        if (!globalPlayer.isLeader()) {
            StartVisibility.setVisibility(View.GONE);
        }

        //on Start button press, the game will start
        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lobbyRef.child("start").setValue(true);
            }
        });


    }

    @Override
    protected void onResume(){
        super.onResume();
        globalPlayer.resumeTheme();
        lobbyRef.child("users").addValueEventListener(usersListener);
        lobbyRef.child("disconnected").addValueEventListener(dcListener);
        lobbyRef.child("start").addValueEventListener(startListener);

    }

    @Override
    protected void onPause(){
        super.onPause();
        globalPlayer.pauseTheme();

        lobbyRef.child("start").removeEventListener(startListener);
        lobbyRef.child("disconnected").removeEventListener(dcListener);
        lobbyRef.child("users").removeEventListener(usersListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (globalPlayer.isLeader() && !globalPlayer.isRunningInBackground()) {
            lobbyRef.child("disconnect").setValue(true);
            lobbyRef.removeValue();

        } else if(!globalPlayer.isRunningInBackground()){
            lobbyRef.child("users").child(globalPlayer.getName()).removeValue();
        }
    }
    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            globalPlayer.setLobbyChosen("");
            startActivity(new Intent(Lobby.this, Start.class));
            finish();
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


    private void ShowPlayers(DataSnapshot dataSnapshot) {
        listOfPlayers = (ListView) findViewById(R.id.lstPlayers);//the list view is the lobbies list view

        ArrayList<String> players = new ArrayList<>();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, players); //creating an arrayadapter for the listview
        listOfPlayers.setAdapter(arrayAdapter); //setting the views adapter to array adapter
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if((boolean) snapshot.child("hunter").getValue()){
                players.add(snapshot.getKey() + ": hunter");
            }
            else{
                players.add(snapshot.getKey() + ": runner" );
            }

        }
    }

    private boolean countHunters(DataSnapshot dataSnapshot) {
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if((boolean) snapshot.child("hunter").getValue()) {
                return false;
            }
        }
        return true;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}