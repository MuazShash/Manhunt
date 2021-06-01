package com.example.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateGamePopup extends AppCompatActivity {

    //Firebase database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game_popup);

        //getActionBar().hide();//hiding the action bar

        //setting the metrics for the popus window
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int)(width*.8),(int)(height*.4));

        final Button CreateLobby = (Button) findViewById(R.id.btnCreateLobby);
        final EditText LobbyName = (EditText) findViewById(R.id.txtLobbyName);

        //lobby creation
        CreateLobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LobbyName.getText().toString().equals("")){
                    Toast.makeText(CreateGamePopup.this, "Please enter a LobbyName", Toast.LENGTH_SHORT).show();
                }
                else{
                    GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext(); //Global player object
                    globalPlayer.setLobbychosen(LobbyName.getText().toString()); //set global lobby name to the EditText input


                    //setting default user attributes on firebase
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("hunter").setValue(false);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("leader").setValue(true);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("latitude").setValue(0.0);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("users").child(globalPlayer.getName()).child("longitude").setValue(0.0);
                    //updating global player attributes
                    globalPlayer.setHunter(false);
                    globalPlayer.setLeader(true);

                    //setting default lobby settings on firebase
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("boundary").setValue(500);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("cooldown").setValue(5);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("distance").setValue(5);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("time_limit").setValue(60);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("timer").setValue(30);
                    myRef.child("lobbies").child(globalPlayer.getLobbychosen()).child("settings").child("hunters").setValue(1);


                    startActivity(new Intent(CreateGamePopup.this,Lobby.class)); //open lobby activity
                }


            }
        });

    }
}