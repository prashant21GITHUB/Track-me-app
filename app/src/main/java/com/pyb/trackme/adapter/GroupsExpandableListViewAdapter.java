package com.pyb.trackme.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.pyb.trackme.cache.TrackDetailsDB;

import java.util.List;

public class GroupsExpandableListViewAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<GroupInfo> groupInfos;
    private final TrackDetailsDB db;

    public GroupsExpandableListViewAdapter(Context context, List<GroupInfo> groupInfos) {
        this.context = context;
        this.groupInfos = groupInfos;
        db = TrackDetailsDB.db();
    }

    @Override
    public int getGroupCount() {
        return 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return null;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
