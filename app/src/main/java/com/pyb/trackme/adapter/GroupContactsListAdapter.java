package com.pyb.trackme.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.selectMultipleContacts.contact.Contact;

import java.util.List;

public class GroupContactsListAdapter extends ArrayAdapter<Contact> {

    private final Context context;
    private final List<Contact> values;

    public GroupContactsListAdapter(@NonNull Context context, int resource, @NonNull List<Contact> values) {
        super(context, resource, values);
        this.values = values;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.group_review_list_item, parent, false);
        if(position % 2 == 0) {
            rowView.setBackgroundColor(Color.LTGRAY);
        }
        Contact contact = values.get(position);
        TextView name = rowView.findViewById(R.id.name);
        name.setText(contact.getFirstName() +" " + contact.getLastName());
        CheckBox checkBox = rowView.findViewById(R.id.select);
        checkBox.setChecked(contact.isChecked());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                contact.setChecked(isChecked, true);
            }
        });
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBox.setChecked(!contact.isChecked());
            }
        });
        return rowView;
    }
}
