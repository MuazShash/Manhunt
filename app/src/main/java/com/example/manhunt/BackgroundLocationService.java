package com.example.manhunt;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BackgroundLocationService extends Service{

    //firebase database reference
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;


    private GlobalPlayerClass globalPlayer;

    final Handler handler = new Handler();
    private String username, lobbyChosen;

    //location listener and criteria
    Criteria criteria = new Criteria();
    LocationListener locationListener;

    String CHANNEL_ID = "ManhuntNotif";
    String channel_name = "Manhunt";
    String channel_description = "APP IN USE";
    Notification notification;
    NotificationCompat.Builder builder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        // application player object
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        globalPlayer.setRunningInBackground(true);

        if(intent.getAction().equals("start_service")){
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            Intent quitIntent = new Intent("close_app");
            PendingIntent pendingQuitIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification = builder.setContentTitle("Manhunt")
                    .setContentText("APP RUNNING???")
                    .setSmallIcon(R.drawable.m_icon_colorised3)
                    .addAction(R.drawable.m_icon_colorised3, "QUIT",
                    pendingQuitIntent)
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);
        }
        else if(intent.getAction().equals("stop_service")){
            globalPlayer.setRunningInBackground(false);
            stopForeground(true);
            stopSelf();
        }



        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channel_name;
            String description = channel_description;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;

    }

    @Override
    public void onCreate() {
        //this.startForeground();
        super.onCreate();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

    }


}
