package com.apps.ing3ns.entregas.Services.GpsServices;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by JuanDa on 27/08/2017.
 *
 * Un Servicio iniciado y vinculado que es covertido en un Foreground Service cuando todos sus clientes se han desvinculado.
 *
 * Para apps corriendo en Background en dispositivos "O", la ubicación solo es computada cada 10
 * minutos y enviada a su cliente cada 30 minutos. Esta restriccion no se aplica a dispositivos
 * "N" o inferiores.
 *
 * Este ejemplo muestra como usar un Servicio de largo funcionamiento para actualizaciones de Ubicación.
 * Cuando un fragment o actividad es vinculada a este Servicio, son permitidas las actualizaciones de
 * Ubicación frecuentes. Cuando los fragments o actividades son removidas o pasan a Segundo Plano,
 * el Servicio se convierte a si mismo en un Foreground Service, y las actualizaciones de Ubicación
 * continuan. Cuando la actividad vuelve a Primer Plano, el Foreground Service se detiene y la
 * notificación asociada con el servicio se remueve. El Servicio continua funcionando en Background
 * como al comienzo.
 *
 */

public class ForegroundLocationService extends Service implements DomiciliarioListener, DeliveryListener {
    private static final String PACKAGE_NAME = "com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService";
    private static final String TAG = ForegroundLocationService.class.getSimpleName();

    /**
     * El nombre del canal de notificaciones (Android "O").
     */
    private static final String CHANNEL_ID = "channel_location";

    /**
     * Tags para manejar diferentes eventos de vinculacion con el Servicio
     */
    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    public static final String EXTRA_NEARBY_DELIVERIES = PACKAGE_NAME + ".nearby_deliveries";
    public static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";
    private static final String NEW_DELIVERY_CHANNEL = "new_delivery_chanel";

    private final IBinder mBinder = new LocalBinder();

    /**
     * Configuracion de tiempo de actualizacion del servicio de Ubicación.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final long MAX_WAIT_TIME_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS * 2;

    /**
     * Configuracion distancias Umbrales.
     */
    private static final double DISTANCE_DEFAULT = 9.5;
    private static final double FULL_DISTANCE = 5.0;
    private static final double TARGET_DISTANCE = 0.07;

    /**
     * Variables para administrar Servicio de Ubicación
     */
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Location mLocation;
    private Handler mServiceHandler;

    /**
     * ID para la notificacion mostrada por el ForegroundService.
     */
    private static final int NOTIFICATION_ID = 12345678;
    private NotificationManager mNotificationManager;

    /**
     * Usado para revisar que el fragment o activivy realmente se desvinculo y no fue parte de un
     * cambio de configuracion onConfigurationChanged en {@link com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService}.
     * Nosotros creamos un Foreground Service con Notificacion solo si ocurre lo primero.
     */
    private boolean mChangingConfiguration = false;

    //######################################################################################
    //------------------------------ VARIABLES DE PROCESO ----------------------------------
    //######################################################################################
    /**
     * Objetos y Controladores de eventos API REST.
     */
    private DomiciliarioController domiciliarioController;
    private DeliveryController deliveryController;
    private Domiciliario domiciliario;
    private Delivery delivery;
    List<Delivery> deliveriesActivos = new ArrayList<>();
    List<Delivery> nearbyDeliveries = new ArrayList<>();
    /**
     * SharedPreferences y otras variables.
     */
    private SharedPreferences preferences;
    private Gson gson;
    private int numLastDeliveries = 0;
    private String lastDeliveryID = "withoutId";

    /**
     * Handler para actualizar posicion cada cierto tiempo (SEARCH MODE)
     */
    Handler handler;
    Runnable runnableCode;
    private static final int TIME_UPDATE_POSITION = 10*60*1000;
    boolean positionUpdated = true;
    boolean entregandoAlert = false;
    /**
     * Recivir notificaciones de pedidos nuevos o eliminados
     */
    private BroadcastReceiver AddOrRemoveDeliveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getExtras().getString(MyFirebaseMessagingService.KEY_TYPE);
            String deliveryJson = intent.getExtras().getString(MyFirebaseMessagingService.KEY_DELIVERY);
            Delivery newDelivery = gson.fromJson(deliveryJson,Delivery.class);

            switch (type) {
                case MyFirebaseMessagingService.KEY_TYPE_ADD_DELIVERY:
                    Log.i(TAG,"Nuevo Delivery Añadido");
                    deliveriesActivos.add(newDelivery);
                    break;

                case MyFirebaseMessagingService.KEY_TYPE_DELETE_DELIVERY:
                    Log.i(TAG,"Delivery Eliminado");
                    if(delivery!=null) {
                        Log.i(TAG, "delivery: " + gson.toJson(delivery));
                        Log.i(TAG, "New delivery: " + deliveryJson);

                        //Si se acepta mi pedido desde otro domiciliario y las direcciones de Start son diferentes quiere decir que acepto mi pedido
                        if(!newDelivery.getAddressStart().equals(delivery.getAddressStart())) {
                            if (!newDelivery.getDomiciliario().equals(delivery.getDomiciliario())) {
                                Log.i(TAG, "Han aceptado mi pedido");
                                UtilsPreferences.setStateDomiciliarioFree(getApplicationContext(), !UtilsPreferences.getStateDomiciliarioFree(getApplicationContext()));
                            }
                        }
                    }
                    Delivery.removeDelivery(deliveriesActivos, newDelivery.get_id());
                    break;
            }
        }
    };

    //######################################################################################
    //------------------------------ ON CREATE Y SIMILARES----------------------------------
    //######################################################################################

    public ForegroundLocationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"On Create Service");
        // Inicializamos las preferencias y gson
        preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();

        // Inicializo los controladores de eventos API REST
        domiciliarioController = new DomiciliarioController(this);
        deliveryController = new DeliveryController(this);
        handler = new Handler();

        numLastDeliveries = UtilsPreferences.getNumberLastDeliveries(this);
        lastDeliveryID = UtilsPreferences.getLastDeliveryId(this);

        // Configuramos el Cliente de servicios de Ubicación y activamos su Callback
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Android O requiere un Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Creamos el canal para la notificacion
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel =  new NotificationChannel(NEW_DELIVERY_CHANNEL, getString(R.string.new_delivery_channel_id), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.GREEN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            // Creamos el Canal de Notificaciones para el Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
            mNotificationManager.createNotificationChannel(channel);
        }

        /** Registramos nuestro BroadCastReceiver para recibir actualizaciones de Deliveries
         * desde el Servicio de notificaciones {@link com.google.firebase.messaging.FirebaseMessagingService}
         */
        LocalBroadcastManager.getInstance(this).registerReceiver((AddOrRemoveDeliveryReceiver), new IntentFilter(MyFirebaseMessagingService.INTENT_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

        // Nosotros estamos aqui porque el usuario decidio remover las actualizaciones de ubicación desde la Notificación.
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }

        // El sistema no intentara recrear el servicio despues de que ha sido matado
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Llamado cuando un cliente (Un fragment o activity) viene de estar en primer plano
        // por primera vez y se enlaza con el servicio. El servicio debe dejar de ser
        // Foreground Service, entonces pasa a funcionar en Background.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Llamado cuando un cliente (Un fragment o activity) regresa a estar en primer plano
        // y se enlaza nuevamente con el Servicio. El servicio debe dejar de ser Foreground Service,
        // entonces pasa a funcionar en Background.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Ultimo Cliente se desvinculo del Servicio");

        // Llamado cuando el ultimo cliente (En este caso un fragment) se desvincula de este Servicio
        // Si este metodo es llamado debido a un cambio en la configuracion en el fragment,
        // nosotros no hacemos nada. De otra forma, nosotros convertimos al Servicio en un
        // Foreground Service
        if (!mChangingConfiguration && UtilsPreferences.getStateLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            /*
            // TODO(developer). If targeting O, use the following code.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                mNotificationManager.startServiceInForeground(new Intent(this,
                        LocationUpdatesService.class), NOTIFICATION_ID, getNotification());
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }
             */
            startForeground(NOTIFICATION_ID, getNotification());
            UtilsPreferences.saveLastLocation(preferences,gson.toJson(mLocation));
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }



    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(AddOrRemoveDeliveryReceiver);
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Hacemos una peticion para comenzar las actualizaciones de Ubicación.
     * En este ejemplo mostramos un Log en caso de no tener permisos de Ubicación, basado en
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Iniciando servicio de actualizacion de Ubicación");
        UtilsPreferences.setStateLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), ForegroundLocationService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            UtilsPreferences.setStateLocationUpdates(this, false);
            Log.e(TAG, "Permisos de Ubicación perdidos. No se puede remover el Servicio. " + unlikely);
        }
    }

    /**
     * Remueve el servicio que actualiza la Ubicación {@link com.google.android.gms.location.FusedLocationProviderClient}
     * En este ejemplo mostramos un Log en caso de no tener permisos de Ubicación, basado en
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removiendo Servicios de Ubicación");
        try {
            domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapState(Utils.DOMICILIARIO_INACTIVO));
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            UtilsPreferences.setStateLocationUpdates(this, false);
            UtilsPreferences.setLastDeliveryId(this,lastDeliveryID);
            UtilsPreferences.setNumberLastDeliveries(this, numLastDeliveries);
            stopSelf();
        } catch (SecurityException unlikely) {
            UtilsPreferences.setStateLocationUpdates(this, true);
            Log.e(TAG, "Permisos de Ubicación perdidos. No se puede remover el Servicio." + unlikely);
        }
    }

    /**
     * Cambiamos el modo a Busqueda de Pedidos.
     */
    public void setModeSearch() {
        Log.i(TAG, "Configurando Modo Busqueda de Pedidos...");
        //Cargamos el Domiciliario y Delivery de las preferences
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences), Domiciliario.class);
        delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences), Delivery.class);
        // Recuperamos los pedidos cercanos que tenemos hasta el momento
        if(nearbyDeliveries.size()>0)getNearbyDeliveries();
        // Recuperamos todos los Deliveries en estado 0
        deliveryController.getDeliveriesCondition(Utils.getHashMapState(Utils.DELIVERY_DISPONIBLE));
    }

    /**
     * Cambiamos el modo a Entregando Delivery.
     */
    public void setModeEntregando() {
        Log.i(TAG, "Configurando Modo Entregando Delivery...");
        entregandoAlert = false;
        //Cargamos el Domiciliario y Delivery de las preferences
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences), Domiciliario.class);
        delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences), Delivery.class);
    }

    /**
     * Solicitamos una respuesta con los Pedidos Cercanos
     */
    public void getNearbyDeliveries(){
        // Notificamos a todos los clientes por un broadcast sobre la nueva Ubicación
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_NEARBY_DELIVERIES, gson.toJson(nearbyDeliveries));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public void getNearbyDeliveriesFirsTime(List<Delivery> nearbyDeliveries){
        // Notificamos a todos los clientes por un broadcast sobre la nueva Ubicación
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_NEARBY_DELIVERIES, gson.toJson(nearbyDeliveries));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Retorna {@link NotificationCompat} usada cuando el Servicio corre como Foreground Service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, ForegroundLocationService.class);

        // Extra nos indica si llegamos al onStarCommand por medio de la notificación o no
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // El PenddingIntent que llama al metodo onStartCommand() en este Servicio
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // El PendingIntent para abrir la App.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SplashActivity.class), 0);

        // Configuramos el texto y el titulo de la notificacion FOREGROUND
        CharSequence text;
        CharSequence tittle;
        if (delivery == null) {
            tittle = "Buscando Pedidos...";
            text = "DeOne esta buscando pedidos cerca de ti";
        }
        else {
            tittle = "Entrega en proceso";
            text = "Tu cliente te esta esperando";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .setContentText(text)
                .setContentTitle(tittle)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(null)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_marker_notification))
                .setSmallIcon(R.drawable.myicon)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        if(delivery==null){
            builder.addAction(R.drawable.ic_cancel, getString(R.string.detener_busqueda), servicePendingIntent);
        }
        // Configuramos el Channel ID para Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    /**
     * Esta funcion nos permite recuperar la ultima Ubicacion {@link Location}
     * y almacenarla en mLocation
     */
    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    /**
     * Esta funcion se debe llamar cuando se requiera correr algun proceso basado en su ubicación
     * @param location The {@link Location}.
     */
    private void onNewLocation(Location location) {
        Log.i(TAG, "Nueva Ubicación: " + location);
        mLocation = location;

        if (delivery != null) locationchangeBehavior(Constants.ACTION.DELIVERY_PROCESS, location);
        else locationchangeBehavior(Constants.ACTION.SEARCH_DELIVERY , location);

        // Notificamos a todos los clientes por un broadcast sobre la nueva Ubicación
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Actualizamos la notificación si estamos corriendo como foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Configuramos los parametros de funcionamiento del servicio de Ubicación.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Clase usada por el cliente Binder. Desde este servicio corre en el mismo proceso que su cliente,
     * we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ForegroundLocationService getService() {
            return ForegroundLocationService.this;
        }
    }

    /**
     * Retorna true si este servicio esta funcionando como Foreground Service
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void locationchangeBehavior(int deliveryProcess, Location location) {
        Log.i(TAG, "Comportamiento al cambiar Ubicacion ");
        //######################################################################################
        //------------------------------- REALIZANDO DELIVERY ----------------------------------
        //######################################################################################
        if(deliveryProcess == Constants.ACTION.DELIVERY_PROCESS){
            Log.i(TAG, "DELIVERY_PROCESS");
            domiciliario.setPosition(new Position(location.getLatitude(),location.getLongitude()));
            HashMap<String,String> map = new HashMap<>();
            map.put("position",gson.toJson(domiciliario.getPosition()));
            domiciliarioController.updateDomiciliario(domiciliario.get_id(),map);

            // Si se acerca mucho a la posicion de recogida el pedido pasa a estado 2
            if(!entregandoAlert & delivery.getState()!=Utils.DELIVERY_ENTREGANDO) {
                if (Utils.distance(delivery.getPositionStart().getLat(), delivery.getPositionStart().getLng(), domiciliario.getPosition().getLat(), domiciliario.getPosition().getLng()) < TARGET_DISTANCE) {
                    deliveryController.updateDelivery(delivery.get_id(), Utils.getHashMapState(Utils.DELIVERY_ENTREGANDO));
                }
            }

            /*
            if (!cercaEnviado) {
                if (Utils.distance(delivery.getPositionStart().getLat(), delivery.getPositionStart().getLng(), domiciliario.getPosition().getLat(), domiciliario.getPosition().getLng()) < 0.2) {
                    cercaEnviado = true;
                    pedidoCerca();
                }
            }
            */
        }

        //######################################################################################
        //--------------------------- BUSCANDO DELIVERIES CERCANOS -----------------------------
        //######################################################################################
        if(deliveryProcess == Constants.ACTION.SEARCH_DELIVERY) {
            Log.i(TAG, "SEARCH_DELIVERIES");
            domiciliario.setPosition(new Position(location.getLatitude(),location.getLongitude()));
            // Compartimos la ubicacion cada cierto tiempo
            if(positionUpdated) {
                runnableCode = new Runnable() {
                    @Override
                    public void run() {
                        positionUpdated = true;
                    }
                };
                handler.postDelayed(runnableCode, TIME_UPDATE_POSITION);

                Log.i(TAG,"Actualizando Posicion domiciliario");
                HashMap<String, String> map = new HashMap<>();
                map.put("position", gson.toJson(domiciliario.getPosition()));
                domiciliarioController.updateDomiciliario(domiciliario.get_id(), map);
                positionUpdated = false;
            }

            // Compruebo cuales de estos delieries estan cerca del domiciliario
            nearbyDeliveries = Delivery.getNearbyDeliveries(deliveriesActivos, mLocation, DISTANCE_DEFAULT);

            if(nearbyDeliveries.size()>0) {
                Log.i(TAG, "HAY PEDIDOS CERCANOS");
                // Compruebo que halla ocurrido un cambio en la lista de Nerby Deliveries
                Log.i("COMPARE_LIST_DELIVERIES", gson.toJson(nearbyDeliveries));
                Log.i("COMPARE_LIST_DELIVERIES", lastDeliveryID);
                Log.i("COMPARE_LIST_DELIVERIES", "num deliveries"+numLastDeliveries);
                if(!Delivery.compareListDeliveries(nearbyDeliveries,lastDeliveryID,numLastDeliveries)) {
                    Log.i(TAG, "LA LISTA DE PEDIDOS CERCANOS A CAMBIADO");
                    if(nearbyDeliveries.size()>numLastDeliveries) {
                        Log.i(TAG, "HAY UN NUEVO PEDIDO CERCANO");
                        showNotificationNewDelivery();
                    }

                    // Asigno valores para numLastDeliveries y para lastDeliveryID
                    numLastDeliveries = nearbyDeliveries.size();
                    lastDeliveryID = nearbyDeliveries.get(numLastDeliveries-1).get_id();

                    // Le enviamos los Pedidos Cercanos a los clientes vinculados (Fragments).
                    if(!serviceIsRunningInForeground(this))getNearbyDeliveries();
                }
            }else{
                if(numLastDeliveries!=0) {
                    if (!serviceIsRunningInForeground(this)) getNearbyDeliveries();
                }
                numLastDeliveries = 0;
                lastDeliveryID = " ";
            }
        }

    }

    /**
     * Mostrar Notificacion cuando se detecta un nuevo pedido cercano
     */
    private void showNotificationNewDelivery() {
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SplashActivity.class), 0);

        // Configuramos el texto y el titulo de la notificacion FOREGROUND
        CharSequence tittle = "Tienes un nuevo pedido disponible";
        CharSequence text = nearbyDeliveries.get(nearbyDeliveries.size()-1).getAddressStart();
        Uri defaultSoundUri = Uri.parse("android.resource://" + getPackageName() + "/raw/thrown");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_ver_pedido), activityPendingIntent)
                .setContentIntent(activityPendingIntent)
                .setContentText(text)
                .setContentTitle(tittle)
                .setOngoing(false)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVibrate(new long[] {100, 250, 100, 500})
                .setSound(defaultSoundUri)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_moto))
                .setSmallIcon(R.drawable.myicon)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Configuramos el Channel ID para Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NEW_DELIVERY_CHANNEL); // Channel ID
        }

        mNotificationManager.notify(Constants.NOTIFICATION_ID.NEW_DELIVERY, builder.build());
    }

    /**
     * Recibimos las respuestas del API Rest {@link com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController}
     */

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        Log.i(TAG,"Domiciliario Actualizado");
        if(domiciliarioUpdated.getState()!=domiciliario.getState()) UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliarioUpdated));
        domiciliario = domiciliarioUpdated;
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    /**
     * Recibimos las respuestas del API Rest {@link com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryController}
     */

    @Override
    public void getDeliveries(List<Delivery> deliveries) {

    }

    @Override
    public void getDelivery(Delivery delivery) {

    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {
        UtilsPreferences.saveDelivery(preferences,gson.toJson(delivery));
        entregandoAlert = true;
    }

    @Override
    public void getDeliveriesConditionSuccessful(List<Delivery> deliveries) {
        //-------- Recibo todos los deliveries en estado 0------
        Log.i(TAG,"Deliveries Actualizados");
        deliveriesActivos = deliveries;
        if(mLocation==null)mLocation = gson.fromJson(UtilsPreferences.getLastLocation(preferences),Location.class);
        if(mLocation==null){
            mLocation = new Location("nueva");
            mLocation.setLatitude(0);
            mLocation.setLongitude(0);
        }
        List<Delivery> nearbyDeliveriesFirstTime = Delivery.getNearbyDeliveries(deliveriesActivos, mLocation, DISTANCE_DEFAULT);
        if(nearbyDeliveries.size()==0)getNearbyDeliveriesFirsTime(nearbyDeliveriesFirstTime);

        Log.i(TAG,"cercanos  anteriores: "+nearbyDeliveries.size() + "  cercanos actuales: "+nearbyDeliveriesFirstTime.size());
    }

    /**
     * Recibimos las respuesta no OK del API Rest
     * @param nameEvent contiene el nombre de la peticion http que resulto en una respuesta != 200
     * @param errorMessage contiene el mensaje de error que dio como respuesta el server
     */
    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {

    }

    /**
     * Recibimos errores de conexión con el servidor
     * @param nameEvent contiene el nombre de la peticion http que resulto en error de conexión
     * @param t contiene Throwable devuelto por errores en la conexión con el server
     */
    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        Toast.makeText(getApplicationContext(), "No tienes conexion a internet", Toast.LENGTH_SHORT).show();
    }

}