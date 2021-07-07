package com.example.manhunt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListOfPlayers extends AppCompatActivity {
    String lobbyChosen;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private GlobalPlayerClass globalPlayer;

    ListView playerListView; //listview variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        lobbyChosen = globalPlayer.getLobbyChosen();

        setContentView(R.layout.listofplayers);
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int) (width * .6), (int) (height * .7));

        myRef.child("lobbies").child(lobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showPlayers(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void showPlayers(DataSnapshot dataSnapshot) {
        playerListView = (ListView) findViewById(R.id.players);//the list view is the lobbies list view

        ArrayList<String> players = new ArrayList<>();

        ArrayAdapter arrayAdapter2 = new ArrayAdapter(this, android.R.layout.simple_list_item_1, players); //creating an arrayadapter for the listview
        playerListView.setAdapter(arrayAdapter2); //setting the views adapter to array adapter

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if ((boolean) snapshot.child("hunter").getValue()) {
                players.add(snapshot.getKey() + ": Hunter");
            } else {
                players.add(snapshot.getKey() + ": Runner");
            }
        }
    }
}
