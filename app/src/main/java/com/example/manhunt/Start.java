
package com.example.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.w3c.dom.Text;

public class Start extends AppCompatActivity {

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String UserName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GlobalPlayerClass globalPlayer = (GlobalPlayerClass) getApplicationContext();




        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final EditText usernameInput = (EditText)findViewById(R.id.NameInput);
        final Button JoinGame = (Button) findViewById(R.id.joinGame);
        final Button CreateGame = (Button) findViewById(R.id.createGame);



        CreateGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent OpenLobby = new Intent(getApplicationContext(), Options.class);
                startActivity(OpenLobby);
            }
        });

        JoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    globalPlayer.setName(usernameInput.getText().toString());

                    startActivity(new Intent(Start.this,ListofLobbies.class));
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
}