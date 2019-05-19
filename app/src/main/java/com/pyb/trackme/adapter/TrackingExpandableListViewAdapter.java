package com.pyb.trackme.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.activities.IPerContactSwitchListener;
import com.pyb.trackme.activities.IRemoveContactButtonClickListener;
import com.pyb.trackme.cache.TrackDetailsDB;

import java.util.List;

public class TrackingExpandableListViewAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<String> valuesWithLiveStatus;
    private final String GROUP_HEADER = "Contacts";
    private final IRemoveContactButtonClickListener removeContactButtonClickListener;
    private final IOnTrackingContactFocusListener onFocusListener;
    private final IPerContactSwitchListener perContactSwitchListener;
    private final TrackDetailsDB db;

    public TrackingExpandableListViewAdapter(Context context, List<String> valuesWithLiveStatus,
                                             IRemoveContactButtonClickListener removeContactButtonClickListener,
                                             IOnTrackingContactFocusListener onFocusListener,
                                             IPerContactSwitchListener perContactSwitchListener) {
        this.context = context;
        this.valuesWithLiveStatus = valuesWithLiveStatus;
        this.removeContactButtonClickListener = removeContactButtonClickListener;
        this.onFocusListener = onFocusListener;
        this.perContactSwitchListener = perContactSwitchListener;
        this.db = TrackDetailsDB.db();
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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        convertView = inflater.inflate(R.layout.drawer_list_item_tracking, parent, false);
        if(childPosition % 2 == 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        String number = valuesWithLiveStatus.get(childPosition);
        TextView numberView = convertView.findViewById(R.id.tracked_number);
        numberView.setText(number);
        ImageView focusBtn = convertView.findViewById(R.id.focus);
        focusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFocusListener.onTrackingContactFocus(childPosition);
            }
        });
        ImageView removeContactBtn = convertView.findViewById(R.id.delete_contact_image);
        removeContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeContactButtonClickListener.onRemoveTrackingContactButtonClick(childPosition);
            }
        });
        Switch startStopTrackingSwitch = convertView.findViewById(R.id.per_contact_switch);
        if(db.getTrackingStatus(number)) {
            startStopTrackingSwitch.setChecked(true);
        } else {
            startStopTrackingSwitch.setChecked(false);
        }
        startStopTrackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                perContactSwitchListener.onTrackingContactSwitchClick(childPosition, isChecked);
            }
        });
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
