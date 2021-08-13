package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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
    private TextView txtWinner, gameStats;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference lobbyRef;
    ListView playerListView; //listview variable
    ValueEventListener usersListener, statsListener;
    Button backToStart;

    private final int TIME_ALIVE = 0;
    private final int RUNNERS_CAUGHT = 1;
    private final int CLOSE_CALLS = 2;

    private String[] awards = {"page 1", "page 2", "page 3", "page 4"};

    ViewPager mViewPager;
    ViewPagerAdapter mViewPagerAdapter;

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

        mViewPager = (ViewPager)findViewById(R.id.viewPagerMain);
        mViewPagerAdapter = new ViewPagerAdapter(EndGame.this, awards);
        mViewPager.setAdapter(mViewPagerAdapter);

    }

    protected void onStart() {
        super.onStart();

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //showPlayers(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        statsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(Double.parseDouble(String.valueOf(snapshot.child("best_runner").child("time_alive").getValue())) < globalPlayer.getUserStat(TIME_ALIVE)){
                    lobbyRef.child("stats").child("best_runner").child("time_alive").setValue(globalPlayer.getUserStat(TIME_ALIVE));
                    lobbyRef.child("stats").child("best_runner").child("name").setValue(globalPlayer.getName());
                }

                if((long) snapshot.child("best_hunter").child("catches").getValue() < globalPlayer.getUserStat(RUNNERS_CAUGHT)){
                    lobbyRef.child("stats").child("best_hunter").child("catches").setValue(globalPlayer.getUserStat(RUNNERS_CAUGHT));
                    lobbyRef.child("stats").child("best_hunter").child("name").setValue(globalPlayer.getName());
                }

                if((long) snapshot.child("most_evasive").child("close_calls").getValue() < globalPlayer.getUserStat(CLOSE_CALLS)){
                    lobbyRef.child("stats").child("most_evasive").child("close_calls").setValue(globalPlayer.getUserStat(CLOSE_CALLS));
                    lobbyRef.child("stats").child("most_evasive").child("name").setValue(globalPlayer.getName());
                }

                if((long) snapshot.child("first_caught").child("time_alive").getValue() > globalPlayer.getUserStat(TIME_ALIVE)){
                    lobbyRef.child("stats").child("first_caught").child("time_alive").setValue(globalPlayer.getUserStat(TIME_ALIVE));
                    lobbyRef.child("stats").child("first_caught").child("name").setValue(globalPlayer.getName());
                }

                awards[0] = "Best Runner\n" + String.valueOf(snapshot.child("best_runner").child("name").getValue()) + "\n" + String.valueOf(snapshot.child("best_runner").child("time_alive").getValue()) + " minutes spent running";
                awards[1] = "Best Hunter\n" + String.valueOf(snapshot.child("best_hunter").child("name").getValue()) + "\n" + String.valueOf(snapshot.child("best_hunter").child("catches").getValue()) + " runners caught";
                awards[2] = "Most Evasive\n" + String.valueOf(snapshot.child("most_evasive").child("name").getValue()) + "\n" + String.valueOf(snapshot.child("most_evasive").child("close_calls").getValue()) + " close calls";
                awards[3] = "First Caught\n" + String.valueOf(snapshot.child("first_caught").child("name").getValue()) + "\ncaught in " +  String.valueOf(snapshot.child("first_caught").child("time_alive").getValue()) + " minutes";


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };


    }

    @SuppressLint("DefaultLocale")
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
        lobbyRef.child("stats").addValueEventListener(statsListener);
        String winner;

        if (globalPlayer.isHunterWins()) {
            winner = "Hunters";
        } else {
            winner = "Runners";
        }

        txtWinner.setText(new StringBuilder().append(winner).append(" win!"));

        /* DO NOT TOUCH FORMATTING */
        /* * * * * * * * * * * * * */

    }

    protected void onPause() {
        super.onPause();

        lobbyRef.child("users").removeEventListener(usersListener);
        lobbyRef.child("stats").removeEventListener(statsListener);
    }

    protected void onStop() {
        super.onStop();

        if (globalPlayer.isLeader()) {
            lobbyRef.setValue(null);
        } else if (!globalPlayer.isLeader()) {
            lobbyRef.child("users").child(globalPlayer.getName()).setValue(null);
        }
    }
}
