package com.apps.ing3ns.entregas.Services.NotificationServices;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
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

    Intent intentForSer;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        intent = new Intent(INTENT_ACTION);

        //intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        //intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String,String> data = remoteMessage.getData();

        if (data.size() > 0) {
            String type = data.get(KEY_TYPE);
            String delivery;

            switch (type){
                case KEY_TYPE_ADD_DELIVERY:
                    Log.i(TAG,"Llego notificacion add delivery");
                    delivery = data.get(KEY_DELIVERY);
                    intent.putExtra(KEY_TYPE, type);
                    intent.putExtra(KEY_DELIVERY, delivery);
                    broadcaster.sendBroadcast(intent);

                    break;

                case KEY_TYPE_DELETE_DELIVERY:
                    Log.i(TAG,"Llego notificacion remove delivery");
                    delivery = data.get(KEY_DELIVERY);
                    intent.putExtra(KEY_TYPE, type);
                    intent.putExtra(KEY_DELIVERY, delivery);
                    broadcaster.sendBroadcast(intent);

                    break;

                case KEY_TYPE_NOTIFICATION:
                    break;
            }
        }
    }
}
