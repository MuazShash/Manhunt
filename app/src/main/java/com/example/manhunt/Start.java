
package com.example.manhunt;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Start extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        getWindow().setNavigationBarColor(getResources().getColor(R.color.black));

        final Button JoinGame = (Button) findViewById(R.id.joinGame);
        final Button CreateGame = (Button) findViewById(R.id.createGame);
        final TextInputEditText usernameInput = (TextInputEditText)findViewById(R.id.NameTextInput);

        CreateGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = usernameInput.getText().toString(); // storing username

                // if username is blank, they need to make one before advancing
                if(username.equals("")) {
                    // popup asking for username
                    Toast.makeText(Start.this, "Please enter a username", Toast.LENGTH_SHORT).show();

                } else { // once they have a username

                    // set username
                    globalPlayer.setName(username);
                    globalPlayer.setLeader(true); // setting them leader for creating the game
                    // display available lobbies
                    startActivity(new Intent(Start.this, CreateGamePopup.class));
                }
            }
        });

        JoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = usernameInput.getText().toString(); // storing username

                // if username is blank, they need to make one before advancing
                if(username.equals("")) {

                    // popup asking for username
                    Toast.makeText(Start.this, "Please enter a username", Toast.LENGTH_SHORT).show();

                } else { // once they have a username

                    // set username
                    globalPlayer.setName(username);
                    globalPlayer.setLeader(false); // setting them non-leader for joining game
                    // display available lobbies
                    startActivity(new Intent(Start.this, ListofLobbies.class));
                }
            }
        });

        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}