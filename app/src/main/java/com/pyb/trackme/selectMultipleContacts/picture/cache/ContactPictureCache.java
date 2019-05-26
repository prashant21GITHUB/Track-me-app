

package com.pyb.trackme.selectMultipleContacts.picture.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

public class ContactPictureCache extends InMemoryCache<Uri, Bitmap> {

    private static ContactPictureCache sInstance;
    private static int sMemClass;

    // we need to synchronize this to make sure there's no race condition instantiating the cache
    public synchronized static ContactPictureCache getInstance(Context context) {
        if (sInstance == null) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            sMemClass = activityManager.getMemoryClass();
            sInstance = new ContactPictureCache();
        }
        return sInstance;
    }

    private ContactPictureCache() {
        // purge after 5 minutes of being idle, the cacheCapacity parameter is ignored
        super(1000 * 60 * 5, 50);
    }

    @Override
    protected HardLruCache createHardLruCache(int cacheCapacity) {
        // Use 1/16th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * sMemClass / 16;
        return new PhotoHardLruCache(cacheSize);
    }

    private class PhotoHardLruCache extends HardLruCache {
        public PhotoHardLruCache(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        protected int sizeOf(Uri key, Bitmap bitmap) {
            // The cache size will be measured in bytes rather than number of items.
            return bitmap.getByteCount();
        }
    }
}