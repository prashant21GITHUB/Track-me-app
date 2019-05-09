package com.pyb.trackme.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.activities.IRemoveContactButtonClickListener;

import java.util.List;

public class SharingContactListViewAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final IRemoveContactButtonClickListener removeContactButtonClickListener;
    private final List<String> values;

    public SharingContactListViewAdapter(Context context, List<String> values,
                                         IRemoveContactButtonClickListener removeContactButtonClickListener) {
        super(context, R.layout.drawer_list_item, values);
        this.context = context;
        this.values = values;
        this.removeContactButtonClickListener = removeContactButtonClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);
        if(position % 2 == 0) {
            rowView.setBackgroundColor(Color.LTGRAY);
        }
        ImageView removeContactBtn = rowView.findViewById(R.id.delete_contact_image);
        removeContactBtn.setVisibility(View.VISIBLE);
        removeContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeContactButtonClickListener.onClick(position);
            }
        });
        TextView number = rowView.findViewById(R.id.tracked_number);
        number.setText(values.get(position));
        return rowView;
    }
}
