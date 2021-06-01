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
import java.util.UUID;

public class ListofLobbies extends AppCompatActivity {

    String SHARED_PREFS = "sharedPrefs";
    private String username = "";
    String Lobbychosen;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();


    ListView LobbyListView; //listview variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println(username);
        setContentView(R.layout.listoflobbies);
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int)(width*.8),(int)(height*.75));


        myRef.child("lobbies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ShowLobbies(dataSnapshot);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void ShowLobbies(DataSnapshot dataSnapshot) {
        LobbyListView = (ListView) findViewById(R.id.lobbies);//the list view is the lobbies list view

        ArrayList<String> lobbies = new ArrayList<>();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,lobbies); //creating an arrayadapter for the listview
        LobbyListView.setAdapter(arrayAdapter); //setting the views adapter to array adapter
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            lobbies.add(snapshot.getKey());

        }
        LobbyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();
                    Lobbychosen = arrayAdapter.getItem(position).toString();
                    username = globalPlayer.getName();
                    globalPlayer.setLobbychosen(Lobbychosen);

                    //write username to database here with some defaults
                    myRef.child("lobbies").child(Lobbychosen).child("users").child(username).child("hunter").setValue(false);
                    myRef.child("lobbies").child(Lobbychosen).child("users").child(username).child("leader").setValue(false);
                    myRef.child("lobbies").child(Lobbychosen).child("users").child(username).child("latitude").setValue(0.0);
                    myRef.child("lobbies").child(Lobbychosen).child("users").child(username).child("longitude").setValue(0.0);

                    //Bringing user to the lobby screen
                    startActivity(new Intent(ListofLobbies.this,Lobby.class));


            }
        });
    }
}
