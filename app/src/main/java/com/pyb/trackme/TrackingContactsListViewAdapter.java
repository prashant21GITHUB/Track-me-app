package com.pyb.trackme;

import android.content.Context;
import android.graphics.Color;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingContactsListViewAdapter extends ArrayAdapter<Pair<String, Boolean>> {

    private final Context context;
    private final List<Pair<String, Boolean>> valuesWithLiveStatus;

    public TrackingContactsListViewAdapter(Context context, List<Pair<String, Boolean>> valuesWithLiveStatus) {
        super(context, R.layout.drawer_list_item, valuesWithLiveStatus);
        this.context = context;
        this.valuesWithLiveStatus = valuesWithLiveStatus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
        if(position % 2 == 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        Pair<String, Boolean> numberWithStatusPair = valuesWithLiveStatus.get(position);
        TextView number = convertView.findViewById(R.id.tracked_number);
        number.setText(numberWithStatusPair.first);
        ImageView liveImageView = convertView.findViewById(R.id.live_image);
        ImageView noLiveImageView = convertView.findViewById(R.id.no_live_image);
        if(numberWithStatusPair.second) {
            liveImageView.setVisibility(View.VISIBLE);
            noLiveImageView.setVisibility(View.GONE);
        } else {
            liveImageView.setVisibility(View.GONE);
            noLiveImageView.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
