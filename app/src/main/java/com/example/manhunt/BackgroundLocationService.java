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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();

        // application player object
        globalPlayer = (GlobalPlayerClass) getApplicationContext();
        username = globalPlayer.getName();
        lobbyChosen = globalPlayer.getLobbyChosen();

        //Defining database reference location
        myRef = database.getReference().child("lobbies").child(lobbyChosen);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                globalPlayer.setLongitude(location.getLongitude());
                globalPlayer.setLatitude(location.getLatitude());

                myRef.child("users").child(username).child("latitude").setValue((Double) globalPlayer.getLatitude());
                myRef.child("users").child(username).child("longitude").setValue((Double) globalPlayer.getLongitude());

                System.out.println(location.getLatitude() + "location listener is working" + location.getLongitude());

            }
        };



        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), (long) 1000, (float) 1, locationListener);



        Intent intent1 = new Intent(this, Game.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0);

        Notification notification = new NotificationCompat.Builder(this, "ChannelId1")
                .setContentTitle("Manhunt")
                .setContentText("APP RUNNING")
                .setSmallIcon(R.drawable.m_icon_colorised3)
                .setContentIntent(pendingIntent).build();

        startForeground(1, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(
                    "ChannelId1", "Foreground notification", NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);



        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {


        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();
        super.onDestroy();

    }

}
