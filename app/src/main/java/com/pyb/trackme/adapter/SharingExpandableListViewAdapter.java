package com.pyb.trackme.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.activities.IPerContactSwitchListener;
import com.pyb.trackme.activities.IRemoveContactButtonClickListener;
import com.pyb.trackme.cache.TrackDetailsDB;

import java.util.List;

public class SharingExpandableListViewAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final IRemoveContactButtonClickListener removeContactButtonClickListener;
    private final List<String> values;
    private final String GROUP_HEADER = "Contacts";
    private final IPerContactSwitchListener perContactSwitchListener;
    private final TrackDetailsDB db;

    public SharingExpandableListViewAdapter(Context context, List<String> values,
                                            IRemoveContactButtonClickListener removeContactButtonClickListener,
                                            IPerContactSwitchListener perContactSwitchListener) {
        this.context = context;
        this.values = values;
        this.removeContactButtonClickListener = removeContactButtonClickListener;
        this.perContactSwitchListener = perContactSwitchListener;
        db = TrackDetailsDB.db();
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return values.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return GROUP_HEADER;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return values.get(childPosition);
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
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.drawer_list_view_group, null);
        }

        TextView lblListHeader = convertView.findViewById(R.id.list_view_header);
//        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawer_list_item_sharing, parent, false);
        if(childPosition % 2 == 0) {
            rowView.setBackgroundColor(Color.LTGRAY);
        }
        ImageView removeContactBtn = rowView.findViewById(R.id.delete_sharing_contact_image);
        removeContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeContactButtonClickListener.onRemoveSharingContactButtonClick(childPosition);
            }
        });
        String contact = values.get(childPosition);
        TextView numberView = rowView.findViewById(R.id.sharing_number);
        numberView.setText(contact);
        CheckBox sharingStatusCheckbox = rowView.findViewById(R.id.sharing_checkbox);
        if(db.getSharingStatus(contact)) {
            sharingStatusCheckbox.setChecked(true);
        } else {
            sharingStatusCheckbox.setChecked(false);
        }
        sharingStatusCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                perContactSwitchListener.onSharingContactSwitchClick(childPosition, isChecked);
            }
        });
        return rowView;
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
