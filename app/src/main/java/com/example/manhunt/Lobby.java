package com.example.manhunt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.security.cert.PKIXRevocationChecker;

public class Lobby extends AppCompatActivity {

    String name;

    EditText nameInput;
    Button saveName;

    TextView lobbyView = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        nameInput = (EditText) findViewById(R.id.nameInput);
        saveName = (Button) findViewById(R.id.btnName);
        lobbyView = (TextView) findViewById(R.id.lobbyView);

        lobbyView.setText(null);

        saveName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameInput.getText().toString();
                lobbyView.append(name + "\n");

                showToast(name);
            }
        });

        final ImageButton button = findViewById(R.id.settings);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent OpenOptions = new Intent(getApplicationContext(), Options.class);
                startActivity(OpenOptions);
            }
        });

    }

    private void showToast(String text) {
        Toast.makeText(this, "Name updated!", Toast.LENGTH_SHORT).show();
    }
}