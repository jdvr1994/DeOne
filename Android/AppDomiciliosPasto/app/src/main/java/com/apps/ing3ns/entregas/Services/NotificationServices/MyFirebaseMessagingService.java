package com.apps.ing3ns.entregas.Services.NotificationServices;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.Actividades.SplashActivity;
import com.apps.ing3ns.entregas.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;
import java.util.Map;

/**
 * Created by JuanDa on 04/03/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String TAG = "tag_messaging";
    public static final String INTENT_ACTION = "MyData";

    public static final String KEY_TITLE= "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_TYPE = "type";
    public static final String KEY_DELIVERY = "delivery";

    public static final String KEY_TYPE_NOTIFICATION = "notification";
    public static final String KEY_TYPE_DELETE_DELIVERY = "delete";
    public static final String KEY_TYPE_ADD_DELIVERY = "add";


    private LocalBroadcastManager broadcaster;
    Intent intent;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        intent = new Intent(INTENT_ACTION);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String,String> data = remoteMessage.getData();

        if (data.size() > 0) {
            String type = data.get(KEY_TYPE);
            String delivery;

            switch (type){
                case KEY_TYPE_ADD_DELIVERY:

                    delivery = data.get(KEY_DELIVERY);
                    intent.putExtra(KEY_TYPE, type);
                    intent.putExtra(KEY_DELIVERY, delivery);
                    broadcaster.sendBroadcast(intent);

                    break;

                case KEY_TYPE_DELETE_DELIVERY:

                    delivery = data.get(KEY_DELIVERY);
                    intent.putExtra(KEY_TYPE, type);
                    intent.putExtra(KEY_DELIVERY, delivery);
                    broadcaster.sendBroadcast(intent);

                    break;

                case KEY_TYPE_NOTIFICATION:
                    NotificationBackground notification = new NotificationBackground(data.get(KEY_TITLE),data.get(KEY_BODY),data);
                    sendNotificationFull(notification);
                    break;
            }
        }

    }


    private void sendNotificationFull(NotificationBackground notification) {

        String title = notification.getTitle();
        String body = notification.getBody();
        String tipo = notification.getData().get(KEY_TYPE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent, PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this, "notification_ch_1");
        } else {
            notificationBuilder = new Notification.Builder(this);
        }

        notificationBuilder
                 .setContentIntent(pendingIntent)
                 .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_moto))
                 .setSmallIcon(R.drawable.myicon)
                 .setContentTitle(title)
                 .setStyle(new Notification.BigTextStyle().bigText(notification.getBody()))
                 .setContentText(body)
                 .setVibrate(new long[] {100, 250, 100, 500})
                 .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private class NotificationBackground {
        //Notification
        String title;
        String body;
        //Data
        Map<String, String> data;

        public NotificationBackground(String title, String body, Map<String, String> data) {
            this.title = title;
            this.body = body;
            this.data = data;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public Map<String, String> getData() {
            return data;
        }
    }

}
