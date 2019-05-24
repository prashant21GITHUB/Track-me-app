package com.pyb.trackme.fcm;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pyb.trackme.R;
import com.pyb.trackme.activities.HomeActivity;

import static com.pyb.trackme.cache.AppConstants.NOTIFICATION_CHANNEL_ID;
import static com.pyb.trackme.cache.AppConstants.NOTIFICATION_ID_FOR_FCM_PUSH_NOTIFICATION;
import static com.pyb.trackme.fcm.MessageAction.STARTED_SHARING;
import static com.pyb.trackme.fcm.MessageAction.TRACKING_REQUEST;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = "TrackMe_PushNotiService";

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.i(TAG,s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, remoteMessage.getMessageId());
        showMessageNotification(remoteMessage);
    }

    private void showMessageNotification(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Intent notificationIntent = new Intent(getApplicationContext(), HomeActivity.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.setAction(NOTIFICATION_CHANNEL_ID);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_noti)
                .setContentIntent(pendingIntent)
                .setContentTitle(notification.getTitle())
//                .setContentText(notification.getBody())
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notification.getBody()))
                .setSound(uri)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if(remoteMessage.getData() != null) {
            String action = remoteMessage.getData().get("ACTION");
            if(action != null) {
                MessageAction messageAction = MessageAction.parse(action);
                if(messageAction.equals(STARTED_SHARING)) {
                    String publisher = remoteMessage.getData().get("PUBLISHER");
                    addIntentForStartedSharingAction(builder, publisher);
                } else if(messageAction.equals(TRACKING_REQUEST)) {
                    String subscriber = remoteMessage.getData().get("SUBSCRIBER");
                    addIntentForTrackingRequestAction(builder, subscriber);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForeground(NOTIFICATION_ID_FOR_FCM_PUSH_NOTIFICATION, builder.build());
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID_FOR_FCM_PUSH_NOTIFICATION, builder.build());
    }

    private void addIntentForTrackingRequestAction(NotificationCompat.Builder builder, String subscriber) {
        Intent notiIntent = new Intent(getApplicationContext(), HomeActivity.class);
        notiIntent.putExtra("SUBSCRIBER", subscriber);
        notiIntent.setAction(getApplicationInfo().packageName + "_" + TRACKING_REQUEST.name());
        notiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntentForAction =
                PendingIntent.getActivity(getApplicationContext(), 0, notiIntent, 0);
        builder.addAction(R.drawable.noti_check, "Share",
                pendingIntentForAction);
    }

    private void addIntentForStartedSharingAction(NotificationCompat.Builder builder, String publisher) {
        Intent notiIntent = new Intent(getApplicationContext(), HomeActivity.class);
        notiIntent.putExtra("PUBLISHER", publisher);
        notiIntent.setAction(getApplicationInfo().packageName + "_" + STARTED_SHARING.name());
        notiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntentForAction =
                PendingIntent.getActivity(getApplicationContext(), 0, notiIntent, 0);
        builder.addAction(R.drawable.noti_check, "Track",
                pendingIntentForAction);
    }

}
