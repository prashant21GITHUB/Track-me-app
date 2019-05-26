

package com.pyb.trackme.selectMultipleContacts.group;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.pyb.trackme.R;
import com.pyb.trackme.selectMultipleContacts.contact.Contact;

import java.util.Collection;

public class GroupViewHolder extends RecyclerView.ViewHolder {

    private View mRoot;
    private CheckBox mSelect;
    private TextView mName;
    private TextView mDescription;

    GroupViewHolder(View root) {
        super(root);

        mRoot = root;
        mSelect = (CheckBox) root.findViewById(R.id.select);
        mName = (TextView) root.findViewById(R.id.name);
        mDescription = (TextView) root.findViewById(R.id.description);
    }

    void bind(final Group group) {
        mRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelect.toggle();
            }
        });

        // main text / title
        mName.setText(group.getDisplayName());

        // description
        Collection<Contact> contacts = group.getContacts();
        Resources res = mRoot.getContext().getResources();
        String desc = res.getQuantityString(R.plurals.cp_group_description, contacts.size(), contacts.size());
        mDescription.setText(desc);

        // check box
        mSelect.setOnCheckedChangeListener(null);
        mSelect.setChecked( group.isChecked() );
        mSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                group.setChecked(isChecked, false);
            }
        });

    }

}
