package com.example.manhunt;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    // Write a string when this client loses connection


    // listener for start of game maybe?

    /* also is game going to work through a lobby or through a game object?
     * would lobby just be a waiting room then for the game to start?
     */
    ListView listOfPlayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // getting global variables to check which lobby was chosen
        GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();
        String lobbyChosen = globalPlayer.getLobbychosen();

        //If statement to delete the lobby or just their user data from the database depending on if they are lobby leader or not
        if (globalPlayer.isLeader()) {
            myRef.child("lobbies").child(lobbyChosen).child("disconnected").onDisconnect().setValue(true);
            myRef.child("lobbies").child(lobbyChosen).onDisconnect().removeValue();
        } else {
            myRef.child("lobbies").child(lobbyChosen).child("users").child(globalPlayer.getName()).onDisconnect().removeValue();
        }

        // Updating listview of players in the lobby
        myRef.child("lobbies").child(lobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ShowPlayers(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Move players back to start page
        myRef.child("lobbies").child(lobbyChosen).child("disconnected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((boolean) dataSnapshot.getValue()) {
                    Intent backToStart = new Intent(getApplicationContext(), Start.class);
                    Toast.makeText(getApplicationContext(), "Leader has left the game!", Toast.LENGTH_SHORT).show();
                    startActivity(backToStart);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // hunter, runner and start button IDs

        final Button Hunter = (Button) findViewById(R.id.selectHunter);
        final Button Runner = (Button) findViewById(R.id.selectRunner);


        Hunter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // on click of hunter

                // set the user to be hunter (hunter = true)
                myRef.child("lobbies").child(lobbyChosen).child("users").child(globalPlayer.getName()).child("hunter").setValue(true);
                globalPlayer.setHunter(true);
            }
        });

        Runner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // on click of runner

                // set the user to be runner (hunter = false)
                myRef.child("lobbies").child(lobbyChosen).child("users").child(globalPlayer.getName()).child("hunter").setValue(false);
                globalPlayer.setHunter(false);
            }
        });

        // settings button
        final ImageButton button = findViewById(R.id.settings);
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

        //start button
        final Button Start = (Button) findViewById(R.id.btnStart);

        //limiting start button visibility to only lobby leader
        View StartVisibility = findViewById(R.id.btnStart);
        if (!globalPlayer.isLeader()) {
            StartVisibility.setVisibility(View.GONE);
        }

        //on Start button press, the game will start
        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child("lobbies").child(lobbyChosen).child("start").setValue(true);
                startActivity(new Intent(Lobby.this, Game.class)); //open maps game activity
            }
        });


        //putting every user into the game now
        myRef.child("lobbies").child(lobbyChosen).child("start").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if ((boolean) snapshot.getValue()) {
                    startActivity(new Intent(Lobby.this, Game.class)); //open maps game activity
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();
        String lobbyChosen = globalPlayer.getLobbychosen();
        boolean leader = globalPlayer.isLeader();

        if (leader) {
            myRef.child("lobbies").child(lobbyChosen).child("disconnect").setValue(true);
            myRef.child("lobbies").child(lobbyChosen).removeValue();
        } else {
            myRef.child("lobbies").child(lobbyChosen).child("users").child(globalPlayer.getName()).removeValue();
        }

        globalPlayer.setLobbychosen("");
        super.onBackPressed();
    }

    private void ShowPlayers(DataSnapshot dataSnapshot) {
        listOfPlayers = (ListView) findViewById(R.id.lstPlayers);//the list view is the lobbies list view

        ArrayList<String> players = new ArrayList<>();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, players); //creating an arrayadapter for the listview
        listOfPlayers.setAdapter(arrayAdapter); //setting the views adapter to array adapter
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            players.add(snapshot.getKey());
        }
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}