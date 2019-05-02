package com.pyb.trackme;

import android.widget.ImageView;

public class ListViewItemHolder {

    private final ImageView liveImage;

    public ListViewItemHolder(ImageView liveImage) {
        this.liveImage = liveImage;
    }

    public ImageView getLiveImage() {
        return liveImage;
    }
}
