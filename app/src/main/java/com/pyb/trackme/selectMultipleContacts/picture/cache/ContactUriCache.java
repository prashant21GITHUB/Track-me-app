

package com.pyb.trackme.selectMultipleContacts.picture.cache;

import android.net.Uri;

/**
 * Used to cache the uri for contact photos
 */
public class ContactUriCache extends InMemoryCache<String, Uri> {

    private static ContactUriCache sInstance;

    // we need to synchronize this to make sure there's no race condition instantiating the cache
    public synchronized static ContactUriCache getInstance() {
        if (sInstance == null) {
            sInstance = new ContactUriCache();
        }
        return sInstance;
    }

    /**
     * Get a photo Uri from the cache.
     *
     * @return Null if the Uri is not in the cache.
     * Uri.Empty if it's not in the cache with a previous miss meaning we already tried to
     * retrieve the query before and we failed, so there's really no point trying again
     * A valid Uri that can be used to retrieve the image.
     */
    public static Uri getUriFromCache(String key) {
        return getInstance().get(key, Uri.EMPTY);
    }

    private ContactUriCache() {
        // purge after 10 minutes of being idle, holds a maximum of 100 URIs
        super(1000 * 60 * 10, 100);
    }

}
