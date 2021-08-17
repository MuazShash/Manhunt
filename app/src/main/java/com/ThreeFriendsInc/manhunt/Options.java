package com.ThreeFriendsInc.manhunt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    GlobalPlayerClass globalPlayer;
    SeekBar seekCD, seekDistance, seekBoundary, seekTimer, seekHunters, seekTimeLimit;
    TextView txtCD, txtDistance, txtBoundary, txtTimer, txtHunters, txtTimeLimit;
    Button back, save;
    String lobby;

    public Options() {
        intBoundary = 0;    // radius from starting position
        intCD = 0;          // cooldown on hunter scan ability
        intDistance = 0;    // how close a hunter needs to be to catch a runner
        intHunters = 0;     // total hunters in the game
        intTimeLimit = 0;   // maximum time the game is allowed to run for
        intTimer = 0;       // game start timer (where runners can get a head start)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        getWindow().setNavigationBarColor(getResources().getColor(R.color.accent_2));

        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        globalPlayer.setRunningInBackground(true);
        //System.out.println("************************lobby name: " + globalPlayer.getLobbychosen());
        lobby = globalPlayer.getLobbyChosen();

        System.out.println("Creating started");
        seekCD = findViewById(R.id.seekCD);
        seekCD.setOnSeekBarChangeListener(seekCDChange);
        txtCD = findViewById(R.id.txtCD);
        getCD();

        seekDistance = findViewById(R.id.seekDistance);
        txtDistance = findViewById(R.id.txtDistance);
        seekDistance.setOnSeekBarChangeListener(seekDistanceChange);
        getDistance();

        seekBoundary = findViewById(R.id.seekBoundary);
        txtBoundary = findViewById(R.id.txtBoundary);
        seekBoundary.setOnSeekBarChangeListener(seekBoundaryChange);
        getBoundary();


        seekTimer = findViewById(R.id.seekTimer);
        txtTimer = findViewById(R.id.txtTimer);
        seekTimer.setOnSeekBarChangeListener(seekTimerChange);
        getTimer();


        seekHunters = findViewById(R.id.seekHunters);
        txtHunters = findViewById(R.id.txtHunters);
        seekHunters.setOnSeekBarChangeListener(seekHuntersChange);
        getHunters();


        seekTimeLimit = findViewById(R.id.seekTimeLimit);
        txtTimeLimit = findViewById(R.id.txtTimeLimit);
        seekTimeLimit.setOnSeekBarChangeListener(seekTimeLimitChange);
        getTimeLimit();

        save = findViewById(R.id.btnSave);
        back = findViewById(R.id.btnBack);
        System.out.println("Creating is done");
    }

    @Override
    protected void onResume() {
        super.onResume();

        globalPlayer.resumeTheme();

        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                myRef.child("lobbies").child(lobby).child("settings").child("cooldown").setValue(intCD);
                myRef.child("lobbies").child(lobby).child("settings").child("boundary").setValue(intBoundary);
                myRef.child("lobbies").child(lobby).child("settings").child("timer").setValue(intTimer);
                myRef.child("lobbies").child(lobby).child("settings").child("time_limit").setValue(intTimeLimit);
                myRef.child("lobbies").child(lobby).child("settings").child("hunters").setValue(intHunters);
                globalPlayer.setSettings(3, intHunters);
                myRef.child("lobbies").child(lobby).child("settings").child("distance").setValue(intDistance);
                System.out.println("Hunters = " + intHunters);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    SeekBar.OnSeekBarChangeListener seekCDChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intCD = progress;
            txtCD.setText("Cooldown duration: " + intCD + " s");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener seekDistanceChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intDistance = progress;
            txtDistance.setText("Capture distance: " + intDistance + " m");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener seekBoundaryChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intBoundary = progress;
            txtBoundary.setText("Map boundaries (radius): " + intBoundary + " m");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener seekTimerChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intTimer = progress;
            txtTimer.setText("Hunt start timer: " + intTimer + " s");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener seekHuntersChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intHunters = progress;
            txtHunters.setText("Starting hunters: " + intHunters);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    SeekBar.OnSeekBarChangeListener seekTimeLimitChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            intTimeLimit = progress;
            txtTimeLimit.setText("Game Time Limit: " + intTimeLimit + " mins");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void getCD() {
        myRef.child("lobbies").child(lobby).child("settings").child("cooldown").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting cooldown failed");
                    intCD = 60;
                } else {
                    intCD = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                }
                txtCD.setText("Cooldown duration: " + intCD + " s");
                seekCD.setProgress(intCD);
            }
        });
    }

    private void getDistance() {
        myRef.child("lobbies").child(lobby).child("settings").child("distance").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intDistance = 2;
                } else {
                    intDistance = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                }
                txtDistance.setText("Capture distance: " + intDistance + " m");
                seekDistance.setProgress(intDistance);
            }
        });
    }

    private void getBoundary() {
        myRef.child("lobbies").child(lobby).child("settings").child("boundary").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intBoundary = 500;
                } else {
                    intBoundary = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                }
                txtBoundary.setText("Map boundaries (radius): " + intBoundary + " m");
                seekBoundary.setProgress(intBoundary);
            }
        });
    }

    private void getTimer() {
        myRef.child("lobbies").child(lobby).child("settings").child("timer").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intTimer = 30;
                } else {
                    intTimer = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                }
                txtTimer.setText("Hunt start timer: " + intTimer + " s");
                seekTimer.setProgress(intTimer);
            }
        });
    }

    private void getHunters() {
        myRef.child("lobbies").child(lobby).child("settings").child("hunters").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                } else {
                    intHunters = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                    txtHunters.setText("Starting hunters: " + intHunters);
                    seekHunters.setProgress(intHunters);
                }
            }
        });
    }

    private void getTimeLimit() {
        myRef.child("lobbies").child(lobby).child("settings").child("time_limit").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    System.out.println("Getting data failed");
                    intTimeLimit = 60;
                } else {
                    intTimeLimit = Integer.parseInt(String.valueOf(task.getResult().getValue()));
                }
                txtTimeLimit.setText("Game Time Limit: " + intTimeLimit + " mins");
                seekTimeLimit.setProgress(intTimeLimit);
            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
        globalPlayer.pauseTheme();
    }

    @Override
    protected void onStop(){
        super.onStop();
        globalPlayer.setRunningInBackground(false);
    }

}