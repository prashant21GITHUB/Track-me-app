

package com.pyb.trackme.selectMultipleContacts.picture;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.pyb.trackme.selectMultipleContacts.contact.Contact;
import com.pyb.trackme.selectMultipleContacts.picture.cache.ContactPictureCache;
import com.pyb.trackme.selectMultipleContacts.picture.cache.ContactUriCache;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Use this class to load contact pictures for ContactBadges.
 *
 * It manages the asynchronous loading of contact pictures and caches Uris and Bitmaps to make
 * sure device resources are used efficiently.
 */
public class ContactPictureManager {
    private static Bitmap sDummyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);

    private static final ExecutorService sExecutor = Executors.newFixedThreadPool(2);

    private final ContactPictureCache sPhotoCache;

    private final boolean mRoundContactPictures;

    public ContactPictureManager(Context context, boolean roundContactPictures) {
        sPhotoCache = ContactPictureCache.getInstance( context );
        mRoundContactPictures = roundContactPictures;
        EventBus.getDefault().register(this);
    }

    /**
     * Load a contact picture and display it using the supplied {@link ContactBadge} instance.
     *
     * <p>
     * If a picture is found in the cache, it is displayed in the {@code ContactBadge}
     * immediately. Otherwise a {@link ContactPictureLoader} is started to try to load the
     * contact picture in a background thread.
     * Depending on the result the contact picture or a fallback picture is shown (letter).
     * </p>
     */
    public void loadContactPicture(Contact contact, ContactBadge badge) {
        String key = contact.getLookupKey();

        // retrieve or create uri for the contact picture
        Uri photoUri = contact.getPhotoUri();
        if (photoUri == null) {
            photoUri = ContactUriCache.getUriFromCache(key);
            if (photoUri == Uri.EMPTY) {
                // pseudo uri used as key to retrieve Uris from the cache
                photoUri = Uri.parse("picture://1gravity.com/"  + Uri.encode(key));
                ContactUriCache.getInstance().put(key, photoUri);
            }
        }

        // retrieve contact picture from cache
        Bitmap bitmap = sPhotoCache.get(photoUri, sDummyBitmap);    // can handle Null keys

        if (bitmap != null && bitmap != sDummyBitmap) {
            // 1) picture found --> update the contact badge
            badge.setBitmap( bitmap );
        }

        else if (photoUri == Uri.EMPTY || bitmap == sDummyBitmap) {
            // 2) we already tried to retrieve the contact picture before (unsuccessfully)
            // --> "letter" contact image
            badge.setCharacter(contact.getContactLetter(), contact.getContactColor());
        }

        else {
            synchronized (badge) {
                boolean hasLoaderAssociated = hasLoaderAssociated(key, badge);

                if (! hasLoaderAssociated) {
                    // 3a) temporary "letter" contact image till the contact picture is loaded (if there's any)
                    badge.setCharacter(contact.getContactLetter(), contact.getContactColor());

                    // 3b) load the contact picture
                    ContactPictureLoader loader = new ContactPictureLoader(key, badge, contact.getPhotoUri(), mRoundContactPictures);
                    badge.setKey(key);
                    try {
                        sExecutor.execute(loader);
                    }
                    catch (Exception ignore) {}
                }
            }
        }
    }

    /**
     * @return {@code true}, if a loader with the same key has already been started for this
     *         ContactBadge, {@code false} otherwise.
     */
    private boolean hasLoaderAssociated(String loaderKey, ContactBadge badge) {
        String badgeKey = badge.getKey();

        if (badgeKey == null || loaderKey == null) {
            // no loader associated with the ContactBadge
            return false;
        }

        return badgeKey.equals(loaderKey);
    }

    /**
     * A ContactPictureLoader has loaded a contact picture and wants to update the ContactBadge.
     * We use EventBus because it's a convenient way to run this on the UI thread and because it's
     * very little boilerplate code.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ContactPictureLoaded event) {
        ContactBadge badge = event.getBadge();
        String badgeKey = badge.getKey();
        String loaderKey = event.getKey();

        if (badgeKey != null && loaderKey != null && badgeKey.equals(loaderKey)) {
            badge.setBitmap( event.getBitmap() );
        }
    }

}
