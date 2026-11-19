package com.lostlink.app.data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.lostlink.app.ItemDetailsActivity;
import com.lostlink.app.R;

public final class LocalNotifier {
    private static final String CHANNEL_ID = "lostlink_updates";
    private static final int PERMISSION_REQUESTED_API = 33;

    private LocalNotifier() { }

    public static void showForSignedInUser(Context context, String recipientEmail, String title,
                                           String message, long itemId) {
        String signedInEmail = context.getSharedPreferences("lostlink_auth", Context.MODE_PRIVATE)
                .getString("session_email", "");
        if (!recipientEmail.equals(signedInEmail)) return;
        if (Build.VERSION.SDK_INT >= PERMISSION_REQUESTED_API
                && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent(context, ItemDetailsActivity.class);
        openIntent.putExtra("item_id", itemId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (itemId & 0x7fffffff),
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_lostlink)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), notification);
    }
}
