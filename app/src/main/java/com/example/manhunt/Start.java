package com.example.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Start extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final Button JoinGame = (Button) findViewById(R.id.joinGame);
        final Button CreateGame = (Button) findViewById(R.id.createGame);



        CreateGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent OpenLobby = new Intent(getApplicationContext(), Lobby.class);
                startActivity(OpenLobby);
            }
        });



        JoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    startActivity(new Intent(Start.this,ListofLobbies.class));
            }
        });
    }
}