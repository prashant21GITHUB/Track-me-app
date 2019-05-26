

package com.pyb.trackme.selectMultipleContacts.picture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;

import com.pyb.trackme.selectMultipleContacts.Helper;
import com.pyb.trackme.selectMultipleContacts.picture.cache.ContactPictureCache;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import static android.graphics.Bitmap.createBitmap;

/**
 * Runnable to load a contact picture for a specific ContactBadge.
 */
public class ContactPictureLoader implements Runnable {

    private final String mKey;
    private final SoftReference<ContactBadge> mBadge;
    private final Uri mPhotoUri;
    private final boolean mRoundContactPictures;

    ContactPictureLoader(String key, ContactBadge badge, Uri photoUri, boolean roundContactPictures) {
        mKey = key;
        mBadge = new SoftReference<>(badge);
        mPhotoUri = photoUri;
        mRoundContactPictures = roundContactPictures;
    }

    @Override
    public void run() {
        ContactBadge badge = mBadge.get();
        if (badge == null) return; // fail fast

        Bitmap bitmap = retrievePicture(badge.getContext(), mPhotoUri, mRoundContactPictures);
        if (bitmap != null) {
            ContactPictureLoaded.post(mKey, badge, bitmap);
        }
    }

    public static Bitmap retrievePicture(Context context, Uri photoUri, boolean roundContactPictures) {
        if (context == null || photoUri == null || Helper.isNullOrEmpty(photoUri.toString())) {
            return null;
        }

        InputStream stream = null;
        Bitmap bitmap = null;

        // read contact picture
        try {
            stream = context.getContentResolver().openInputStream(photoUri);
            bitmap = BitmapFactory.decodeStream(stream);

            if (bitmap != null) {
                // some contact pictures aren't square...
                if (bitmap.getWidth() != bitmap.getHeight()) {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    int indent = Math.abs(w - h) / 2;
                    int x = w > h ? indent : 0;
                    int y = w < h ? indent : 0;
                    int size = Math.min(w, h);
                    bitmap = createBitmap(bitmap, x, y, size, size);
                }

                if (roundContactPictures) {
                    bitmap = getRoundedBitmap(bitmap);
                }
            }
        }
        catch (OutOfMemoryError | FileNotFoundException ignore) {}
        finally {
            Helper.closeQuietly(stream);
        }

        // cache contact picture
        if (bitmap != null) {
            ContactPictureCache.getInstance(context).put(photoUri, bitmap);
        }

        return bitmap;
    }

    /**
     * See http://www.curious-creature.com/2012/12/11/android-recipe-1-image-with-rounded-corners/
     */
    static private Bitmap getRoundedBitmap(Bitmap bitmap) {
        Bitmap output = createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        RectF rectF = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
        final Canvas canvas = new Canvas(output);
        canvas.drawOval(rectF, paint);

        return output;
    }

}
