package com.example.manhunt;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListofLobbies extends AppCompatActivity {

    String SHARED_PREFS = "sharedPrefs";
    private String username = "";
    String LobbyChosen;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    GlobalPlayerClass globalPlayer;
    boolean ready = false;
    boolean isDuplicateUser = false;
    ListView LobbyListView; //listview variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        setContentView(R.layout.listoflobbies);
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int) (width * .8), (int) (height * .75));


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

    protected void onResume(){
        super.onResume();

        globalPlayer.resumeTheme();
    }

    protected void onPause(){
        super.onPause();

        globalPlayer.pauseTheme();
    }
    private void ShowLobbies(DataSnapshot dataSnapshot) {
        LobbyListView = (ListView) findViewById(R.id.endPlayers);//the list view is the lobbies list view

        ArrayList<String> lobbies = new ArrayList<>();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, lobbies); //creating an array adapter for the listview
        LobbyListView.setAdapter(arrayAdapter); //setting the views adapter to array adapter
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if (snapshot.child("start").getValue() != null) {
                if ((boolean) snapshot.child("start").getValue()) {
                    //do nothing
                } else {
                    lobbies.add(snapshot.getKey()); //add lobby to the list of lobbies
                }
            }


        }

        LobbyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();

                LobbyChosen = arrayAdapter.getItem(position).toString();
                username = globalPlayer.getName();
                globalPlayer.setLobbyChosen(LobbyChosen);
                globalPlayer.setLatitude(0.0);
                globalPlayer.setLongitude(0.0);

                myRef.child("lobbies").child(LobbyChosen).child("users").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            if (username.equals(dataSnapshot.getKey())) {
                                isDuplicateUser = true;
                            }
                        }
                        ready = true;

                        if (isDuplicateUser && ready) {
                            Toast.makeText(ListofLobbies.this, "Username taken, please use a different username", Toast.LENGTH_SHORT).show();
                        }
                        if (!isDuplicateUser && ready) {
                            //write username to database here with some defaults
                            myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("hunter").setValue(false);
                            myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("caught").setValue(false);
                            myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("leader").setValue(false);
                            myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("latitude").setValue(0.0);
                            myRef.child("lobbies").child(LobbyChosen).child("users").child(username).child("longitude").setValue(0.0);

                            //Bringing user to the lobby screen
                            startActivity(new Intent(ListofLobbies.this, Lobby.class));
                            finish();
                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });

            }
        });
    }
}
