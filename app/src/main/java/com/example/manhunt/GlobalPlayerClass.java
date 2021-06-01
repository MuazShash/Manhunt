package com.example.manhunt;

import android.app.Application;

public class GlobalPlayerClass extends Application {
    private String name;
    private boolean Hunter = false;
    private boolean Leader = false;
    private double Longitude;
    private double Latitude;


    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public boolean isLeader() {
        return Leader;
    }

    public void setLeader(boolean leader) {
        Leader = leader;
    }

    public boolean isHunter() {
        return Hunter;
    }

    public void setHunter(boolean hunter) {
        Hunter = hunter;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
