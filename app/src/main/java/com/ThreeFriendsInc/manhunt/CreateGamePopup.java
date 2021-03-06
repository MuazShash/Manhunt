package com.ThreeFriendsInc.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CreateGamePopup extends AppCompatActivity {

    //Firebase database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference().child("lobbies");
    ValueEventListener lobbies;
    GlobalPlayerClass globalPlayer;

    boolean isDuplicateLobby = false;
    boolean ready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game_popup); //setting content view

        //for rounded edges
        this.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        //getActionBar().hide();//hiding the action bar

        //setting the metrics for the popup window
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int) (width * .8), (int) (height * .4));

    }

    @Override
    protected void onStart() {
        super.onStart();

        final Button CreateLobby = (Button) findViewById(R.id.btnCreateLobby);
        final EditText LobbyName = (EditText) findViewById(R.id.txtLobbyName);

        myRef.addValueEventListener(lobbies = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ready = false;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    System.out.println(dataSnapshot.getKey());
                    if (LobbyName.getText().toString().equals((String) dataSnapshot.getKey())) {
                        isDuplicateLobby = true;
                    }
                }
                ready = true;
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

        CreateLobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (LobbyName.getText().toString().equals("")) {
                    Toast.makeText(CreateGamePopup.this, "Please enter a LobbyName", Toast.LENGTH_SHORT).show();
                } else if (isDuplicateLobby && ready) {
                    Toast.makeText(CreateGamePopup.this, "Lobby name taken, please use a new name for your lobby", Toast.LENGTH_SHORT).show();
                } else if (!isDuplicateLobby && ready) {


                    GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext(); //Global player object
                    globalPlayer.setLobbyChosen(LobbyName.getText().toString()); //set global lobby name to the EditText input

                    //setting the start game object to false (players will listen to this object in the lobby screen)
                    myRef.child(globalPlayer.getLobbyChosen()).child("start").setValue(false);

                    //creates the disconnected attribute within the lobby
                    myRef.child(globalPlayer.getLobbyChosen()).child("disconnected").setValue(false);

                    //creates the scan attribute within th lobby
                    myRef.child(globalPlayer.getLobbyChosen()).child("scan").setValue(false);

                    //creates the start position lat/long in the lobby
                    myRef.child(globalPlayer.getLobbyChosen()).child("startLat").setValue(0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("startLng").setValue(0);

                    //setting default user attributes on firebase
                    myRef.child(globalPlayer.getLobbyChosen()).child("users").child(globalPlayer.getName()).child("hunter").setValue(false);
                    myRef.child(globalPlayer.getLobbyChosen()).child("users").child(globalPlayer.getName()).child("leader").setValue(true);
                    myRef.child(globalPlayer.getLobbyChosen()).child("users").child(globalPlayer.getName()).child("latitude").setValue(0.0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("users").child(globalPlayer.getName()).child("longitude").setValue(0.0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("users").child(globalPlayer.getName()).child("kick").setValue(false);

                    //updating global player attributes
                    globalPlayer.setHunter(false);
                    globalPlayer.setLeader(true);

                    //setting default lobby settings on firebase
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("boundary").setValue(1000);
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("cooldown").setValue(60);
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("distance").setValue(5);
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("time_limit").setValue(60);
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("timer").setValue(25);
                    myRef.child(globalPlayer.getLobbyChosen()).child("settings").child("hunters").setValue(1);
                    globalPlayer.setSettings(3, 1);

                    //Stats
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("best_hunter").child("name").setValue("No one");
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("best_hunter").child("catches").setValue(0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("best_runner").child("name").setValue("No one");
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("best_runner").child("time_alive").setValue(0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("first_caught").child("name").setValue("No one");
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("first_caught").child("time_alive").setValue(0);
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("most_evasive").child("name").setValue("No one");
                    myRef.child(globalPlayer.getLobbyChosen()).child("stats").child("most_evasive").child("close_calls").setValue(0);

                    startActivity(new Intent(CreateGamePopup.this, Lobby.class)); //open lobby activity
                    finish();
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

    @Override
    protected void onStop() {
        super.onStop();
        myRef.removeEventListener(lobbies);
    }

}