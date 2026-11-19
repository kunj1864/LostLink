package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lostlink.app.data.AppNotification;
import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public class NotificationsActivity extends Activity {
    private LostLinkDatabase database;
    private AuthStore authStore;
    private LinearLayout notificationsList;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        database = new LostLinkDatabase(this);
        authStore = new AuthStore(this);
        notificationsList = findViewById(R.id.notificationsList);
        emptyMessage = findViewById(R.id.emptyNotificationsMessage);
        findViewById(R.id.notificationsBackButton).setOnClickListener(view -> finish());
        database.markNotificationsRead(authStore.getEmail());
        renderNotifications();
    }

    private void renderNotifications() {
        ArrayList<AppNotification> notifications = database.findNotifications(authStore.getEmail());
        notificationsList.removeAllViews();
        for (AppNotification notification : notifications) {
            View row = getLayoutInflater().inflate(R.layout.row_notification,
                    notificationsList, false);
            ((TextView) row.findViewById(R.id.notificationTitle)).setText(notification.title);
            ((TextView) row.findViewById(R.id.notificationMessage)).setText(notification.message);
            row.setOnClickListener(view -> {
                if (notification.itemId >= 0) {
                    Intent intent = new Intent(this, ItemDetailsActivity.class);
                    intent.putExtra("item_id", notification.itemId);
                    startActivity(intent);
                }
            });
            notificationsList.addView(row);
        }
        emptyMessage.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
