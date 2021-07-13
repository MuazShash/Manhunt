package com.example.manhunt;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomPlayerList extends ArrayAdapter{
    private ArrayList<String> playerNames;
    private ArrayList<String> playerType;
    private ArrayList<Integer> imageId;
    private Activity context;


    public CustomPlayerList(Activity context, ArrayList<String> playerNames, ArrayList<String> playerType, ArrayList<Integer> imageId){
        super(context, R.layout.row_design, playerNames);
        this.context = context;
        this.playerNames = playerNames;
        this.playerType = playerType;
        this.imageId = imageId;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row=convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView==null)
            row = inflater.inflate(R.layout.row_design, null, true);
        TextView textViewCountry = (TextView) row.findViewById(R.id.textViewCountry);
        TextView textViewCapital = (TextView) row.findViewById(R.id.textViewCapital);
        ImageView imageFlag = (ImageView) row.findViewById(R.id.imageViewFlag);

        textViewCountry.setText(playerNames.get(position));
        textViewCapital.setText(playerType.get(position));
        imageFlag.setImageResource(imageId.get(position));
        return  row;
    }

}
