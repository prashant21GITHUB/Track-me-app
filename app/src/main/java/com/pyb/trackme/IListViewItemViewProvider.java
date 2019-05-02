package com.pyb.trackme;

import android.widget.ImageView;

public interface IListViewItemViewProvider {

    ListViewItemHolder getViewHolder(int position);

    ListViewItemHolder getViewHolder(String contact);
}
