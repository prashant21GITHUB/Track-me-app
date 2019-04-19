package com.pyb.trackme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class CustomListViewAdapter extends ArrayAdapter<Pair<String, String>> {

    private final Context context;
    private final List<Pair<String, String>> values;

    public CustomListViewAdapter(Context context, List<Pair<String, String>> values) {
        super(context, R.layout.list_view_item, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_view_item, parent, false);
        if(position % 2 == 0) {
            rowView.setBackgroundColor(Color.LTGRAY);
        }
        TextView name = rowView.findViewById(R.id.list_item_name);
        TextView number = rowView.findViewById(R.id.list_item_number);
        name.setText(values.get(position).first);
        number.setText(values.get(position).second);
        return rowView;
    }
}
