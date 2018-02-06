package com.yalantis.ucrop.sample;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * 类名: NotificationUtils
 * 此类用途: ---
 *
 * @Date: 2018-02-06 17:04
 * @FileName: com.yalantis.ucrop.sample.NotificationUtils.java
 */
public class NotificationUtils extends ContextWrapper {

    private NotificationManager manager;
    public static final String id = "channel_1";
    public static final String name = "channel_name_1";

    public NotificationUtils(Context context) {
        super(context);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getChannelNotification(String title, String content, Intent intent) {
        return new Notification.Builder(getApplicationContext(), id)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(getString(R.string.notification_image_saved))
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setOngoing(false)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .setAutoCancel(true);
    }

    public NotificationCompat.Builder getNotification_25(String title, String content, Intent intent) {
        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(getString(R.string.notification_image_saved))
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setOngoing(false)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .setAutoCancel(true);
    }

    public void sendNotification(String title, String content, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            Notification notification = getChannelNotification
                    (title, content, intent).build();
            getManager().notify(911, notification);
        } else {
            Notification notification = getNotification_25(title, content, intent).build();
            getManager().notify(911, notification);
        }
    }
}