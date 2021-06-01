package com.example.manhunt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Options extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    int intCD, intDistance, intBoundary, intTimer, intHunters, intTimeLimit;
    SeekBar seekCD, seekDistance, seekBoundary, seekTimer, seekHunters, seekTimeLimit;
    TextView txtCD, txtDistance, txtBoundary, txtTimer, txtHunters, txtTimeLimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        System.out.println("Creating started");
        seekCD = findViewById(R.id.seekCD);
        txtCD = findViewById(R.id.txtCD);
        getCD();

        seekDistance = findViewById(R.id.seekDistance);
        txtDistance = findViewById(R.id.txtDistance);
        getDistance();

        seekBoundary = findViewById(R.id.seekBoundary);
        txtBoundary = findViewById(R.id.txtBoundary);
        getBoundary();


        seekTimer = findViewById(R.id.seekTimer);
        txtTimer = findViewById(R.id.txtTimer);
        getTimer();


        seekHunters = findViewById(R.id.seekHunters);
        txtHunters = findViewById(R.id.txtHunters);
        getHunters();


        seekTimeLimit = findViewById(R.id.seekTimeLimit);
        txtTimeLimit = findViewById(R.id.txtTimeLimit);
        getTimeLimit();


        System.out.println("Creating is done");
    }

    private void getCD(){
        myRef.child("lobbies").child("lobby1").child("settings").child("cooldown").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting cooldown failed");
                    intCD = 60;
                    txtCD.setText("Cooldown duration: " + intCD + " s");
                    seekCD.setProgress(intCD);
                }
                else {
                    intCD = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtCD.setText("Cooldown duration: " + intCD + " s");
                    seekCD.setProgress(intCD);
                }
            }
        });
    }

    private void getDistance(){
        myRef.child("lobbies").child("lobby1").child("settings").child("distance").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intDistance = 2;
                    txtDistance.setText("Capture distnace: " + intDistance + " s");
                    seekDistance.setProgress(intDistance);
                }
                else {
                  intDistance = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                  txtDistance.setText("Capture distnace: " + intDistance + " s");
                  seekDistance.setProgress(intDistance);
                }
            }
        });
    }

    private void getBoundary(){
        myRef.child("lobbies").child("lobby1").child("settings").child("boundary").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intBoundary = 500;
                    txtBoundary.setText("Cooldown duration: " + intBoundary + " s");
                    seekBoundary.setProgress(intBoundary);
                }
                else {
                    intBoundary = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtBoundary.setText("Cooldown duration: " + intBoundary + " s");
                    seekBoundary.setProgress(intBoundary);
                }
            }
        });
    }

    private void getTimer(){
        myRef.child("lobbies").child("lobby1").child("settings").child("timer").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intTimer = 30;
                    txtTimer.setText("Cooldown duration: " + intTimer + " s");
                    seekTimer.setProgress(intTimer);
                }
                else {
                    intTimer = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtTimer.setText("Cooldown duration: " + intTimer + " s");
                    seekTimer.setProgress(intTimer);
                }
            }
        });
    }

    private void getHunters(){
        myRef.child("lobbies").child("lobby1").child("settings").child("hunters").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intHunters = 1;
                    txtHunters.setText("Cooldown duration: " + intHunters + " s");
                    seekHunters.setProgress(intHunters);
                }
                else {
                    intHunters = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtHunters.setText("Cooldown duration: " + intHunters + " s");
                    seekHunters.setProgress(intHunters);
                }
            }
        });
    }

    private void getTimeLimit(){
        myRef.child("lobbies").child("lobby1").child("settings").child("time_limit").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intTimeLimit = 60;
                    txtTimeLimit.setText("Cooldown duration: " + intTimeLimit + " s");
                    seekTimeLimit.setProgress(intTimeLimit);
                }
                else {
                    intTimeLimit = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtTimeLimit.setText("Cooldown duration: " + intTimeLimit + " s");
                    seekTimeLimit.setProgress(intTimeLimit);
                }
            }
        });
    }
}