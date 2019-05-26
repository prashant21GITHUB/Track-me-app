

package com.pyb.trackme.selectMultipleContacts.contact;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.pyb.trackme.R;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureManager;
import com.pyb.trackme.selectMultipleContacts.picture.ContactPictureType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactAdapter extends RecyclerView.Adapter<ContactViewHolder> implements SectionIndexer  {

    private List<? extends Contact> mContacts;

    final private ContactSortOrder mSortOrder;
    final private ContactPictureType mContactPictureType;
    final private ContactDescription mContactDescription;
    final private int mContactDescriptionType;
    final private ContactPictureManager mContactPictureLoader;

    private LayoutInflater mInflater;

    public ContactAdapter(Context context, List<Contact> contacts,
                          ContactSortOrder sortOrder,
                          ContactPictureType contactPictureType,
                          ContactDescription contactDescription,
                          int contactDescriptionType) {
        mContacts = contacts;
        mSortOrder = sortOrder;
        mContactPictureType = contactPictureType;
        mContactDescription = contactDescription;
        mContactDescriptionType = contactDescriptionType;
        mContactPictureLoader = new ContactPictureManager(context, mContactPictureType == ContactPictureType.ROUND);
    }

    public void setData(List<? extends Contact> contacts) {
        mContacts = contacts;
        notifyDataSetChanged();
        if (! mContacts.isEmpty()) {
            calculateSections();
        }
    }

    @Override
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mInflater == null) {
            mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        View view = mInflater.inflate(R.layout.cp_contact_list_item, parent, false);
        return new ContactViewHolder(view, mContactPictureLoader, mContactPictureType,
                                     mContactDescription, mContactDescriptionType);
    }

    @Override
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        if (mContacts != null) {
            holder.bind( mContacts.get(position) );
        }
    }

    @Override
    public void onViewRecycled(ContactViewHolder holder) {
        holder.onRecycled();
    }

    @Override
    public int getItemCount() {
        return mContacts == null ? 0 : mContacts.size();
    }

    @Override
    public long getItemId(int position) {
        return mContacts == null ? super.getItemId(position) : mContacts.get(position).getId();
    }

    private Map<Character, ContactSection> mSections = new HashMap<>();
    private ContactSection[] mSectionArray;

    synchronized private void calculateSections() {
        mSections.clear();
        List<ContactSection> sectionArray = new ArrayList<>();

        if (mContacts != null) {
            int contactPos = 0;
            int sectionPos = 0;
            char prevLetter = 0;
            for (Contact contact : mContacts) {
                char letter = contact.getContactLetter(mSortOrder);
                if (letter != prevLetter) {
                    ContactSection newSection = new ContactSection(letter, sectionPos++, contactPos);
                    mSections.put(letter, newSection);
                    sectionArray.add(newSection);
                    prevLetter = letter;
                }
                contactPos++;
            }
        }

        mSectionArray = sectionArray.toArray(new ContactSection[sectionArray.size()]);
    }

    @Override
    public synchronized Object[] getSections() {
        return mSectionArray;
    }

    @Override
    public synchronized int getPositionForSection(int sectionPos) {
        if (mSections == null || mSections.isEmpty() ||
            mContacts == null || mContacts.isEmpty()) return 0;

        int maxIndexSections = assertBoundaries(mSectionArray.length - 1, 0, mSectionArray.length);
        sectionPos = assertBoundaries(sectionPos, 0, maxIndexSections);

        int contactPos = mSectionArray[sectionPos].getContactPos();

        int maxIndexContacts = assertBoundaries(mContacts.size() - 1, 0, mContacts.size());
        return assertBoundaries(contactPos, 0, maxIndexContacts);
    }

    @Override
    public synchronized int getSectionForPosition(int contactPosition) {
        if (mSections == null || mSections.isEmpty() ||
            mContacts == null || mContacts.isEmpty()) return 0;

        int maxIndexContacts = assertBoundaries(mContacts.size() - 1, 0, mContacts.size());
        contactPosition = assertBoundaries(contactPosition, 0, maxIndexContacts);

        char contactLetter = mContacts.get(contactPosition).getContactLetter(mSortOrder);
        ContactSection section = mSections.get(contactLetter);
        int sectionPos = section != null ? section.getSectionPos() : 0;

        int maxIndexSections = assertBoundaries(mSectionArray.length - 1, 0, mSectionArray.length);
        return assertBoundaries(sectionPos, 0, maxIndexSections);
    }

    private int assertBoundaries(int index, int lower, int upper) {
        return Math.max(lower, Math.min(index, upper));
    }

}
