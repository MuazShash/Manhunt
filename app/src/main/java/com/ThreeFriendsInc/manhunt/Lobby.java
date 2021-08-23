package com.ThreeFriendsInc.manhunt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;





import java.util.ArrayList;

public class Lobby extends AppCompatActivity {

    //database reference
    DatabaseReference lobbyRef;
    ValueEventListener dcListener, startListener, usersListener, kickListener, settingsListener;

    // Write a string when this client loses connection
    ListView listOfPlayers;
    GlobalPlayerClass globalPlayer;
    String lobbyChosen;
    long dcTIme;

    int hunters = 0;

    private long lastTouchTime = 0;
    private long currentTouchTime = 0;

    private String kickPlayerName;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
        final ImageButton settings = findViewById(R.id.settings);

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
                hunters = countHunters(dataSnapshot);
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
                    lobbyRef.child("settings").addValueEventListener(settingsListener);
                    startActivity(new Intent(Lobby.this, Game.class)); //open maps game activity
                    globalPlayer.stopTheme();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        };

        settingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int i = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    globalPlayer.setSettings(i++, Integer.parseInt(String.valueOf(dataSnapshot.getValue())));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
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

        settings.setOnClickListener(new View.OnClickListener() {
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
                System.out.println(hunters+ " HUNTERS " + globalPlayer.getSettings(3) + " SETTINGS");
                if(hunters == globalPlayer.getSettings(3)){
                    lobbyRef.child("start").setValue(true);
                }
                else if (hunters > globalPlayer.getSettings(3)){
                    showToast("You have too many hunters!");
                }
                else{
                    showToast("You need " + globalPlayer.getSettings(3) + " hunters to start the game!");
                }
            }
        });


        kickListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if((boolean) snapshot.child("kick").getValue()){
                    showToast("You have been kicked by the leader");
                    startActivity(new Intent(Lobby.this, Start.class));
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

    }

    @Override
    protected void onResume(){
        super.onResume();
        globalPlayer.resumeTheme();
        lobbyRef.child("users").addValueEventListener(usersListener);
        lobbyRef.child("disconnected").addValueEventListener(dcListener);
        lobbyRef.child("start").addValueEventListener(startListener);
        lobbyRef.child("users").child(globalPlayer.getName()).addValueEventListener(kickListener);
    }

    @Override
    protected void onPause(){
        super.onPause();
        globalPlayer.pauseTheme();

        lobbyRef.child("start").removeEventListener(startListener);
        lobbyRef.child("disconnected").removeEventListener(dcListener);
        lobbyRef.child("users").removeEventListener(usersListener);
        lobbyRef.child("users").child(globalPlayer.getName()).removeEventListener(kickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (globalPlayer.isLeader() && !globalPlayer.isRunningInBackground()) {
            lobbyRef.child("settings").removeEventListener(settingsListener);
            lobbyRef.child("disconnected").setValue(true);
            lobbyRef.removeValue();

        } else if(!globalPlayer.isRunningInBackground()){
            lobbyRef.child("settings").removeEventListener(settingsListener);
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
        CustomPlayerList customPlayerList = new CustomPlayerList(this, listPlayers(dataSnapshot), listPlayerTypes(dataSnapshot), putPlayerIcons(dataSnapshot));
        listOfPlayers.setAdapter(customPlayerList);


            listOfPlayers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(globalPlayer.isLeader()){

                        kickPlayerName = listPlayers(dataSnapshot).get(position);
                        lastTouchTime = currentTouchTime;
                        currentTouchTime = System.currentTimeMillis();
                        Toast.makeText(Lobby.this, "Press again to Kick player", Toast.LENGTH_SHORT).show();

                        if (currentTouchTime - lastTouchTime < 3000) {
                            lobbyRef.child("users").child(kickPlayerName).child("kick").setValue(true);
                            lastTouchTime = 0;
                            currentTouchTime = 0;
                        }

                    }

                }
            });



    }

    private int countHunters(DataSnapshot dataSnapshot) {
        int numOfHunters = 0;
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if((boolean) snapshot.child("hunter").getValue()) {
                numOfHunters++;
            }
        }
        System.out.println(numOfHunters + " HUNTERS");
        return numOfHunters;
    }

    private ArrayList<String> listPlayers(DataSnapshot dataSnapshot){
        ArrayList<String> listOfPlayers = new ArrayList<>();
        for(DataSnapshot snapshot: dataSnapshot.getChildren()){
            listOfPlayers.add(snapshot.getKey());
        }

        return listOfPlayers;
    }
    private ArrayList<String> listPlayerTypes(DataSnapshot dataSnapshot){
        ArrayList<String> listOfPlayerTypes = new ArrayList<>();
        for(DataSnapshot snapshot: dataSnapshot.getChildren()){
            if((boolean) snapshot.child("hunter").getValue()){
                listOfPlayerTypes.add("hunter");
            }
            else{
                listOfPlayerTypes.add("runner");
            }

        }

        return listOfPlayerTypes;
    }

    private ArrayList<Integer> putPlayerIcons(DataSnapshot dataSnapshot){
        ArrayList<Integer> playerIcons = new ArrayList<>();
        for(DataSnapshot snapshot: dataSnapshot.getChildren()){
            if((boolean) snapshot.child("hunter").getValue()){
                playerIcons.add(R.drawable.hunter_icon);
            }
            else{
                playerIcons.add(R.drawable.runner_icon);
            }

        }

        return playerIcons;
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}