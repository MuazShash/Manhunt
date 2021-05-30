package com.example.manhunt;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListofLobbies extends AppCompatActivity {


    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    ListView view; //listview variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.listoflobbies);
        DisplayMetrics dimentions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimentions);
        int width = dimentions.widthPixels;
        int height = dimentions.heightPixels;
        getWindow().setLayout((int)(width*.8),(int)(height*.75));


        myRef.child("lobbies").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ShowLobbies(dataSnapshot);

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void ShowLobbies(DataSnapshot dataSnapshot) {
        view = (ListView) findViewById(R.id.lobbies);//the list view is the lobbies list view

        ArrayList<String> lobbies = new ArrayList<>();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,lobbies); //creating an arrayadapter for the listview
        view.setAdapter(arrayAdapter); //setting the views adapter to arrayadapter
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            System.out.println(snapshot.getKey());
            lobbies.add(snapshot.getKey());

        }
    }
}
