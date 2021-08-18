package com.ThreeFriendsInc.manhunt;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HowToPlay extends AppCompatActivity {

    GlobalPlayerClass globalPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        final TextView instructions = (TextView) findViewById(R.id.Instructions);
        setContentView(R.layout.activity_how_to_play); //setting content view

        instructions.setText("Step 1: Make sure you have an internet or data connection enabled. Pick a unique name that can differentiate you from your fellow players.\n" +
                "Step 2: Either create a new lobby, or if someone has already made one, join theirs.\n" +
                "Step 3: Once everyone from your party has joined the lobby, selected their starting role (hunter/runner), and the host has edited the settings, you are ready to start.\n" +
                "Step 4: When the host clicks \"START GAME\" all the players will be put into the game. Runners have a head start before the hunters can catch them.\n" +
                "Step 5: The goal for the runners is to evade capture for as long as possible. The goal for the hunters is to capture all the runners within the time limit. The hunters will be able to scan at regular intervals to see the runners' locations.\n" +
                "Step 6: The game ends when either all the runners have been caught or the time limit runs out. Players will see the stats for the game before being brought back to the menu screen.\n" +
                "\nFor more details, visit out ");


    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
