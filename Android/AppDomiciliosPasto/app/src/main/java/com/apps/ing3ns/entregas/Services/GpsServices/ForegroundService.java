package com.apps.ing3ns.entregas.Services.GpsServices;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryController;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryListener;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Actividades.SplashActivity;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.Modelos.Position;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.NotificationServices.MyFirebaseMessagingService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by JuanDa on 27/08/2017.
 */

public class ForegroundService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DomiciliarioListener, DeliveryListener {
    private static final String LOG_TAG = "ForegroundService";
    public static final String INTENT_ACTION = "DataReady";
    private LocalBroadcastManager broadcaster;

    DomiciliarioController domiciliarioController;

    //-------------------------------------- GOOGLE API CLIENT ----------------------------
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    FusedLocationProviderApi fusedLocationProviderApi;
    PendingResult<LocationSettingsResult> result;
    boolean cercaEnviado = false;

    Notification notification;
    Intent intentMainActivity;
    PendingIntent pendingIntent;
    public SharedPreferences preferences;
    Gson gson;
    Domiciliario domiciliario;
    Delivery delivery;
    DeliveryController deliveryController;
    List<Delivery> deliveriesActivos = new ArrayList<>();
    int numPedidosCercanos = 0;
    Location lastLocation;
    boolean flagLogin = false;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getExtras().getString(MyFirebaseMessagingService.KEY_TYPE);
            String deliveryJson = intent.getExtras().getString(MyFirebaseMessagingService.KEY_DELIVERY);
            Delivery delivery = gson.fromJson(deliveryJson,Delivery.class);

            switch (type) {
                case MyFirebaseMessagingService.KEY_TYPE_ADD_DELIVERY:
                    deliveriesActivos.add(delivery);
                    Log.d("DELIVERY","ADD"+type+" "+deliveryJson);
                    break;

                case MyFirebaseMessagingService.KEY_TYPE_DELETE_DELIVERY:
                    Delivery.removeDelivery(deliveriesActivos,delivery.get_id());
                    Log.d("DELIVERY","RECIBIDA"+type+" "+deliveryJson);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG,"On create Services");
        intentMainActivity = new Intent(this, SplashActivity.class);
        intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
        broadcaster = LocalBroadcastManager.getInstance(this);
        domiciliarioController = new DomiciliarioController(this);
        deliveryController = new DeliveryController(this);

        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter(MyFirebaseMessagingService.INTENT_ACTION)
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG,"On Start Command");
        if(intent!=null) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.START_FOREGROUND_SHARE)) {
                    //------------------------ Cargamos el domiciliario y delivery de las preferences -----------------
                    domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences), Domiciliario.class);
                    delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences), Delivery.class);
                    //------------------------ Recuperamos todos los Deliveries en estado 0 -------------------------
                    if(delivery==null) {
                        deliveryController.getDeliveriesCondition(Utils.getHashMapState(0));
                        Log.d(LOG_TAG,"Peticion pedidos");
                    }
                    //--------------------------- INICIAMOS Y ACTIVAMOS EL GOOGLE API CLIENT ------------------------
                    googleApiLocationActive();
                } else if (intent.getAction().equals(Constants.ACTION.STOP_FOREGROUND)) {
                    stopForeground(true);
                    googleApiClientDisconnect();
                    cercaEnviado = false;
                    stopSelf();
                } else if (intent.getAction().equals(Constants.ACTION.START_FOREGROUND)) {
                    flagLogin = true;
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG,"On Destroy Service");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        googleApiClientDisconnect();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //------------------------------------GOOGLE API CLIENT LOCATION -----------------------------
    //-----------------------------------------------------------------------------------------
    public void googleApiClientDisconnect(){
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
    }

    public void googleApiLocationActive() {
        Log.d(LOG_TAG,"Services----Google api location active");
        if(fusedLocationProviderApi!=null) {
            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    fusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient,this);
                }
            }
        }

        mLocationRequest = null;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);
        fusedLocationProviderApi = LocationServices.FusedLocationApi;

        googleApiClientInit();
        mGoogleApiClient.connect();
    }

    public void googleApiClientInit() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {

            }
        });

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(mGoogleApiClient.isConnected()) {
            fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            pendingIntent = PendingIntent.getActivity(this, 0, intentMainActivity, PendingIntent.FLAG_ONE_SHOT);
            setTextNotification("Buscando ");
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE_GPS, notification);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        UtilsPreferences.saveLastLocation(preferences,gson.toJson(location));
        lastLocation = location;
        if (delivery != null) locationchangeBehavior("Compartiendo Posicion con el Cliente:::" + new Random().nextInt(), Constants.ACTION.DELIVERY_PROCESS, location);
        else locationchangeBehavior("Buscando pedidos cercanos", Constants.ACTION.SEARCH_DELIVERY , location);
    }

    private void locationchangeBehavior(String textNotification, int deliveryProcess, Location location) {
        //-------------------------- Modifico la notificacion solo si antes ha sido Creada ------------------------
        if (notification != null) {
            setTextNotification(textNotification);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE_GPS, notification);
        }
        //------------------  Comportamiento del servicio cuando se esta REALIZANDO DELIVERY -----------------------
        if(deliveryProcess == Constants.ACTION.DELIVERY_PROCESS){
            domiciliario.setPosition(new Position(location.getLatitude(),location.getLongitude()));
            HashMap<String,String> map = new HashMap<>();
            map.put("position",gson.toJson(domiciliario.getPosition()));
            domiciliarioController.updateDomiciliario(domiciliario.get_id(),map);

            if (!cercaEnviado) {
                if (Utils.distance(delivery.getPositionStart().getLat(), delivery.getPositionStart().getLng(), domiciliario.getPosition().getLat(), domiciliario.getPosition().getLng()) < 0.2) {
                    cercaEnviado = true;
                    pedidoCerca();
                }

                if (Utils.distance(delivery.getPositionStart().getLat(), delivery.getPositionStart().getLng(), domiciliario.getPosition().getLat(), domiciliario.getPosition().getLng()) < 0.07) {
                    //Cambio el estado del domiciliario y/o pedido
                }
            }
        }
        //---------------  Comportamiento del servicio cuando se esta BUSCANDO DELIVERY CERCANO ---------------------
        if(deliveryProcess == Constants.ACTION.SEARCH_DELIVERY) {
            //-------- Compruebo cuales de estos delieries estan cerca del domiciliario ---------------
             List<Delivery> nearbyDeliveries = Delivery.getNearbyDeliveries(deliveriesActivos, lastLocation, 0.5);
             List<Delivery> lastNearbyDeliveries = gson.fromJson(UtilsPreferences.getNearbyDeliveries(preferences),new TypeToken<List<Delivery>>(){}.getType());

             if(lastNearbyDeliveries!=null) Log.d("Deliveries Compare"," "+nearbyDeliveries.size()+ " " + lastNearbyDeliveries.size());
             if(nearbyDeliveries.size()>0) {
                 //-------- Compruebo que halla ocurrido un cambio en la lista de Nerby Deliveries ----------------
                 if(!Delivery.compareListDeliveries(nearbyDeliveries,lastNearbyDeliveries)) {
                     Log.d("Deliveries Compare"," "+nearbyDeliveries.size()+ " " + lastNearbyDeliveries.size());
                     if(lastNearbyDeliveries.size()<nearbyDeliveries.size()) {
                         NotificationBackground notification = new NotificationBackground("DeOne","Hay un nuevo pedido disponible");
                         sendNotificationFull(notification);
                     }
                     numPedidosCercanos = nearbyDeliveries.size();
                     UtilsPreferences.saveNearbyDeliveries(preferences, gson.toJson(nearbyDeliveries));
                     //-------- Uso intent para avisarle al fragment Domiciliario que puede cargar los deliveries cercanos en las preferences ---------------
                     Intent intent = new Intent(INTENT_ACTION);
                     intent.putExtra("data", true);
                     if(broadcaster!=null) {
                         broadcaster.sendBroadcast(intent);
                     }
                 }
             }
        }
    }

    private void sendNearbyDeliveriesFirstTime(){
        if(flagLogin) {
            lastLocation = gson.fromJson(UtilsPreferences.getLastLocation(preferences),Location.class);
            if(lastLocation != null) {
                List<Delivery> nearbyDeliveries = Delivery.getNearbyDeliveries(deliveriesActivos, lastLocation, 0.5);
                if (nearbyDeliveries.size() > 0) {
                    numPedidosCercanos = nearbyDeliveries.size();
                    UtilsPreferences.saveNearbyDeliveries(preferences, gson.toJson(nearbyDeliveries));
                    //-------- Uso intent para avisarle al fragment Domiciliario que puede cargar los deliveries cercanos en las preferences ---------------
                    Intent intent = new Intent(INTENT_ACTION);
                    intent.putExtra("data", true);
                    broadcaster.sendBroadcast(intent);
                }
                flagLogin = false;
            }
        }
    }

    private void setTextNotification(String textNotification) {
        notification = new NotificationCompat.Builder(this,getString(R.string.default_notification_channel_id))
                .setContentTitle("DeOne")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_marker_notification))
                .setSmallIcon(R.drawable.myicon)
                .setTicker("DeOne Servicio de Posicionamiento")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(textNotification))
                .setContentText(textNotification)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
    }

    private void sendNotificationFull(NotificationBackground notification) {

        String title = notification.getTitle();
        String body = notification.getBody();

        Intent intent = new Intent(this, SplashActivity.class);
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

        public NotificationBackground(String title, String body) {
            this.title = title;
            this.body = body;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }
    }

    //----------------------- REVISAR POSITION DE DOMICILIARIO ---------------------
    //------------------------------------------------------------------------------
    //------------------------------------------------------------------------------

    public void pedidoCerca(){
        /*
        Gson gson = new GsonBuilder().create();
        String pedidoEnProcesoJson = Utils.getEntregandoSharedPreferences(preferences);
        Pedido pedidoCerca = gson.fromJson(pedidoEnProcesoJson, Pedido.class);
        pedidoCerca.setEstado(6);
        pedidoEnProcesoJson = gson.toJson(pedidoCerca);

        PedidoService service = API.getApiPost(API.VERSION_MODE).create(PedidoService.class);
        Call<ResponseBody> versionCall = service.sendPedido(pedidoEnProcesoJson);

        versionCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String respuesta = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
        */
    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliario) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliario));
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getDeliveries(List<Delivery> deliveries) {

    }

    @Override
    public void getDelivery(Delivery delivery) {

    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {

    }

    @Override
    public void getDeliveriesConditionSuccessful(List<Delivery> deliveries) {
        //-------- Recibo todos los deliveries en estado 0------
        Log.d(LOG_TAG,"Posicion DOMICILIARIO en SERVER");
        deliveriesActivos = deliveries;
        sendNearbyDeliveriesFirstTime();
    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {

    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {

    }
}