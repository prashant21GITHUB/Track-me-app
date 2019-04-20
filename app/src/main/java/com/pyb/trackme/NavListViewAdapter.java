package com.pyb.trackme;

import android.content.Context;
import android.graphics.Color;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class NavListViewAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> values;

    public NavListViewAdapter(Context context, List<String> values) {
        super(context, R.layout.drawer_list_item, values);
        this.context = context;
        this.values = values;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);
        if(position % 2 == 0) {
            rowView.setBackgroundColor(Color.LTGRAY);
        }
        TextView number = rowView.findViewById(R.id.tracked_number);
       number.setText(values.get(position));
        return rowView;
    }
}
