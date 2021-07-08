package com.example.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class EndGame extends AppCompatActivity {

    private GlobalPlayerClass globalPlayer;
    private TextView txtWinner;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference lobbyRef;
    ListView playerListView; //listview variable
    ValueEventListener usersListener;
    Button backToStart;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        lobbyRef = database.getReference().child("lobbies").child(globalPlayer.getLobbyChosen());
        //showing whether hunters or runners have won in the textview
        txtWinner = (TextView) findViewById(R.id.winnerType);
        //back to start button
        backToStart = (Button) findViewById(R.id.backToStart);
    }

    protected void onStart() {
        super.onStart();

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showPlayers(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        backToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EndGame.this, Start.class)); //starting the start page activity
                finish(); //closing the end game activity
            }
        });

        lobbyRef.child("users").addValueEventListener(usersListener);


        if (globalPlayer.isHunterWins()) {
            txtWinner.setText("Hunters");
        } else if (!globalPlayer.isHunterWins()) {
            txtWinner.setText("Runners");
        }
    }

    protected void onPause() {
        super.onPause();

        lobbyRef.child("users").removeEventListener(usersListener);
    }

    protected void onStop() {
        super.onStop();

        if (globalPlayer.isLeader()) {
            lobbyRef.setValue(null);
        } else if (!globalPlayer.isLeader()) {
            lobbyRef.child("users").child(globalPlayer.getName()).setValue(null);
        }
    }


    private void showPlayers(DataSnapshot dataSnapshot) {
        playerListView = (ListView) findViewById(R.id.endPlayers);//the list view is the lobbies list view

        ArrayList<String> players = new ArrayList<>();

        ArrayAdapter arrayAdapter2 = new ArrayAdapter(this, android.R.layout.simple_list_item_1, players); //creating an arrayadapter for the listview
        playerListView.setAdapter(arrayAdapter2); //setting the views adapter to array adapter

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if ((boolean) snapshot.child("hunter").getValue()) {
                players.add(snapshot.getKey() + ": Hunter");
            } else if ((boolean) snapshot.child("caught").getValue()) {
                players.add(snapshot.getKey() + ": Caught");
            } else {
                players.add(snapshot.getKey() + ": Escaped");
            }
        }
    }
}