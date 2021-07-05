package com.example.manhunt;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class EndGame extends AppCompatActivity {

    private GlobalPlayerClass globalPlayer;
    private TextView txtWinner;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_game);

    }


    @Override
    protected void onResume(){
        super.onResume();
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        //back to start button
        final Button backToStart = (Button) findViewById(R.id.backToStart);
        backToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EndGame.this, Start.class)); //starting the start page activity
                finish(); //closing the end game activity
            }
        });

        //showing whether hunters or runners have won in the textview
        txtWinner = (TextView) findViewById(R.id.winnerType);

        if(globalPlayer.isHunterWins()){
            txtWinner.setText("Hunters");
        }
        else if(!globalPlayer.isHunterWins()){
            txtWinner.setText("Runners");
        }


    }

}
