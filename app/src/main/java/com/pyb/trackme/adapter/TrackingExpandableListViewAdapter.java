package com.pyb.trackme.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pyb.trackme.R;

import java.util.List;

public class TrackingExpandableListViewAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<Pair<String, Boolean>> valuesWithLiveStatus;
    private final String GROUP_HEADER = "Contacts";

    public TrackingExpandableListViewAdapter(Context context, List<Pair<String, Boolean>> valuesWithLiveStatus) {
        this.context = context;
        this.valuesWithLiveStatus = valuesWithLiveStatus;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return valuesWithLiveStatus.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return GROUP_HEADER;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return valuesWithLiveStatus.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.drawer_list_view_group, null);
        }
        String headerTitle = (String) getGroup(groupPosition);

        TextView lblListHeader = convertView.findViewById(R.id.list_view_header);
//        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
        }
        if(childPosition % 2 == 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        Pair<String, Boolean> numberWithStatusPair = valuesWithLiveStatus.get(childPosition);
        TextView number = convertView.findViewById(R.id.tracked_number);
        number.setText(numberWithStatusPair.first);
//        ImageView liveImageView = convertView.findViewById(R.id.live_image);
//        ImageView noLiveImageView = convertView.findViewById(R.id.no_live_image);
//        if(numberWithStatusPair.second) {
//            liveImageView.setVisibility(View.VISIBLE);
//            noLiveImageView.setVisibility(View.GONE);
//        } else {
//            liveImageView.setVisibility(View.GONE);
//            noLiveImageView.setVisibility(View.VISIBLE);
//        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }
}
