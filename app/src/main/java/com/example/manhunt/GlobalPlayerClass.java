package com.example.manhunt;

import android.app.Application;

public class GlobalPlayerClass extends Application {
    private String name;
    private boolean hunter = false;
    private boolean leader = false;
    private double longitude;
    private double latitude;
    int[] settings = new int[6];
    private String lobbyChosen = "";
    private boolean hunterWins = true;

    public boolean isHunterWins() {
        return hunterWins;
    }
    public void setHunterWins(boolean hunterWins){
        this.hunterWins = true;
    }

    public String getLobbyChosen() { return lobbyChosen; }

    public void setLobbyChosen(String lobbychosen) {
        this.lobbyChosen = lobbychosen;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double Latitude) {
        latitude = Latitude;
    }

    public double getLongitude() { return longitude; }

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

    public int getSettings(int index){ return settings[index]; }

    public void setSettings(int index, int value){ settings[index] = value;}
}
