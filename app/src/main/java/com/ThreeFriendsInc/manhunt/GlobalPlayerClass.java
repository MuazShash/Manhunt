package com.ThreeFriendsInc.manhunt;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;

public class GlobalPlayerClass extends Application {
    private String name;
    private boolean hunter = false;
    private boolean leader = false;
    private boolean locationPermissions = false;
    private double longitude;
    private double latitude;
    int[] settings = new int[6];
    double[] userStats = {0.0, 0.0, 0.0};
    private String lobbyChosen = "";
    private boolean hunterWins = true, runningInBackground = false;
    private MediaPlayer mpTheme;
    private String notificationMessage;



    public boolean isLocationPermissions() {
        return locationPermissions;
    }

    public void setLocationPermissions(boolean locationPermissions) {
        this.locationPermissions = locationPermissions;
    }


    public boolean isHunterWins() {
        return hunterWins;
    }

    public void setHunterWins(boolean hunterWins) {
        this.hunterWins = true;
    }

    public String getLobbyChosen() {
        return lobbyChosen;
    }

    public void setLobbyChosen(String lobbyChosen) {
        this.lobbyChosen = lobbyChosen;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double Latitude) {
        latitude = Latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double Longitude) {
        longitude = Longitude;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean Leader) {
        leader = Leader;
    }

    public boolean isHunter() {
        return hunter;
    }

    public void setHunter(boolean Hunter) {
        hunter = Hunter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSettings(int index) {
        return settings[index];
    }

    public void setSettings(int index, int value) {
        settings[index] = value;
    }

    public double getUserStat(int index) {
        return userStats[index];
    }

    public void setUserStat(int index, double value) {
        userStats[index] = value;
    }

    public void startTheme(Context context) {
        mpTheme = MediaPlayer.create(context, R.raw.main_theme);
        mpTheme.setLooping(true);
        mpTheme.start();
    }

    public void pauseTheme(){
        mpTheme.pause();
    }

    public void resumeTheme(){
        mpTheme.start();
    }

    public void stopTheme() {
        mpTheme.setLooping(false);
        mpTheme.stop();
    }

    public boolean isRunningInBackground(){
        return runningInBackground;
    }

    public void setRunningInBackground(boolean flag){
        runningInBackground = flag;
    }

    public void setMessage(String message){
        notificationMessage = message;
    }

    public String getMessage(){
        return notificationMessage;
    }

}
