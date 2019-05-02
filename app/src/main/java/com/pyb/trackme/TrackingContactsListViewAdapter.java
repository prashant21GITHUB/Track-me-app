package com.pyb.trackme;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingContactsListViewAdapter extends ArrayAdapter<String> implements IListViewItemViewProvider {

    private final Context context;
    private final List<String> values;
    private final Map<Integer, ListViewItemHolder> viewHolderMapByPosition;
    private final Map<String, ListViewItemHolder> viewHolderMapByContact;

    public TrackingContactsListViewAdapter(Context context, List<String> values) {
        super(context, R.layout.drawer_list_item, values);
        this.context = context;
        this.values = values;
        viewHolderMapByPosition = new HashMap<>();
        viewHolderMapByContact = new HashMap<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListViewItemHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
            viewHolder = new ListViewItemHolder(convertView.findViewById(R.id.live_image));
            viewHolderMapByPosition.put(position, viewHolder);
            viewHolderMapByContact.put(values.get(position), viewHolder);
            convertView.setTag(viewHolder);
        }
        if(position % 2 == 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        TextView number = convertView.findViewById(R.id.tracked_number);
        number.setText(values.get(position));
        return convertView;
    }

    @Override
    public ListViewItemHolder getViewHolder(int position) {
        return viewHolderMapByPosition.get(position);
    }

    @Override
    public ListViewItemHolder getViewHolder(String contact) {
        return viewHolderMapByContact.get(contact);
    }

}
