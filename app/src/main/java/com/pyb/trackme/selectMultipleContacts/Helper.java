

package com.pyb.trackme.selectMultipleContacts;

import android.content.Context;
import android.database.Cursor;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.Closeable;
import java.io.IOException;

public class Helper {

    public static boolean isNullOrEmpty(CharSequence string){
        return string == null || string.length() == 0;
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics( metrics );
        return metrics;
    }

    public static void closeQuietly(Cursor cursor) {
        try {
            cursor.close();
        }
        catch (Exception ignore) {}
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ignore) {}
    }

}
