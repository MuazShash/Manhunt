package com.ThreeFriendsInc.manhunt;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
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
        TextView link;
        link = (TextView) findViewById(R.id.GitHyperlink);
        setContentView(R.layout.activity_how_to_play); //setting content view

        //for rounded edges
        this.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        //setting the metrics for the popup window
        DisplayMetrics dimensions = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dimensions);
        int width = dimensions.widthPixels;
        int height = dimensions.heightPixels;
        getWindow().setLayout((int) (width * .95), (int) (height * .8));

        if (link != null) {link.setMovementMethod(LinkMovementMethod.getInstance());}

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
