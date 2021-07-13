package com.example.manhunt;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomPlayerList extends ArrayAdapter{
    private String[] playerNames;
    private String[] playerType;
    private Integer[] imageId;
    private Activity context;


    public CustomPlayerList(Activity context, String[] playerNames, String[] playerType, Integer[] imageId){
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

        textViewCountry.setText(playerNames[position]);
        textViewCapital.setText(playerType[position]);
        imageFlag.setImageResource(imageId[position]);
        return  row;
    }

}
