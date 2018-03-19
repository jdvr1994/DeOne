package com.apps.ing3ns.entregas.Fragmentos;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryAcceptController;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryAcceptListener;
import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.BuildConfig;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Client;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.apps.ing3ns.entregas.modelsRoutes.MostrarRuta;
import com.apps.ing3ns.entregas.modelsRoutes.MostrarRutaListener;
import com.apps.ing3ns.entregas.modelsRoutes.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements MostrarRutaListener,OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener, DeliveryAcceptListener {
    //######################################################################################
    //---------------- NUEVO SERVICIO DE UBICACION BACKGROUND/FOREGROUND -------------------
    //######################################################################################
    /**
     * Variables usadas para peticion de Permisos de Ubicacion.
     */
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Variables usadas para administrar la dinamica de Vinculacion y Conexion del Servicio.
     */
    private MyReceiver myReceiver;
    private ForegroundLocationService mService = null;
    // Seguir el Estado de vinculacion con el Servicio.
    private boolean mBound = false;

    // Monitoreamos el estado de conexion del Servicio
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"On Service Connected Fragment Maps");
            ForegroundLocationService.LocalBinder binder = (ForegroundLocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Comienzo la rutina para peticion de PERMISOS y Acceso a la UBICACION
            if (!checkPermissions()) requestPermissions();
            else initServiceLocationDeliveryMode();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"On Services Disconnected Fragment Maps");
            mService = null;
            mBound = false;
        }
    };

    //####################################################################################
    //------------------------------ VARIABLES DE PROCESO ----------------------------------
    //######################################################################################
    /**
     * Objetos y Controladores de eventos API REST.
     */
    private DeliveryAcceptController deliveryAcceptController;
    private Domiciliario domiciliario;
    private Delivery delivery;
    private Client client;

    /**
     * Objetos y variables para Mapa.
     */
    private View rootView;
    private GoogleMap gMap;
    private MapView mapView;
    private Marker markerDomiciliario;
    private Marker markerAddressStart;
    private PolylineOptions polylineOptionsRuta;
    private Polyline polylineRuta;

    /**
     * Otras varibales de proceso.
     */
    private SharedPreferences preferences;
    private FragmentsListener listener;
    private Gson gson;
    private final int PHONE_CALL_CODE = 100;

    //######################################################################################
    //---------------------------- DEFINICION DE OBJETOS UI --------------------------------
    //######################################################################################
    ImageView imageClient;
    TextView nameClient;
    TextView addressStart;
    TextView addressEnd;
    TextView phoneDelivery;
    FloatingActionButton btnProblems;
    FloatingActionButton btnCall;
    FloatingActionButton btnDeliveryOK;
    FloatingActionButton btnGenRoute;
    RelativeLayout cargando;
    TextView textCargando;


    //######################################################################################
    //------------------------------ ON CREATE Y SIMILARES----------------------------------
    //######################################################################################
    public MapFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        Log.i(TAG,"On CreateView Fragment Maps");
        myReceiver = new MyReceiver();
        // Inicializo los controladores de eventos API REST
        deliveryAcceptController = new DeliveryAcceptController(this);
        gson = new GsonBuilder().create();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG,"On ViewCreated Fragment Maps");
        bindUI(view);

        FirebaseMessaging.getInstance().subscribeToTopic(Utils.TOPIC_STATE_2);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_0);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_1);

        // Rescato informacion del domiciliario, cliente y delivery
        // atravez de las SharedPreferences y la VISUALIZO.
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences),Delivery.class);
        client = gson.fromJson(UtilsPreferences.getClient(preferences),Client.class);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        setCardViewDelivery(delivery,client);

        // Recupero el Delivery desde el Server para verificar que esta en estado 0
        // Asignarle el domiciliario y actualizar su estado
        deliveryAcceptController.getDelivery(delivery.get_id());

        //------------------- COMPROBAR CONEXION MAPAS ---------------------
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int status = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (status == ConnectionResult.SUCCESS) {
            mapView = rootView.findViewById(R.id.map);
            if (mapView != null){
                mapView.onCreate(null);
                mapView.onResume();
                mapView.getMapAsync(this);
            }
        } else {
            apiAvailability.getErrorDialog(getActivity(), status, 10).show();
        }

        btnDeliveryOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textCargando.setText("Finalizando Pedido");
                cargando.setVisibility(View.VISIBLE);
                deliveryAcceptController.finishDelivery(domiciliario.get_id(),delivery.get_id());
            }
        });

        btnGenRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textCargando.setText("Generando ruta");
                cargando.setVisibility(View.VISIBLE);
                getRutaGoogle();
            }
        });

        btnProblems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseMessaging.getInstance().subscribeToTopic(Utils.TOPIC_STATE_1);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_0);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_2);

                delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences),Delivery.class);

                textCargando.setText("Cancelando Pedido");
                cargando.setVisibility(View.VISIBLE);
                if(delivery.getState()==Utils.DELIVERY_RECOGIENDO){
                    deliveryAcceptController.updateDelivery(delivery.get_id(),Utils.getHashMapState(0));
                }

                if(delivery.getState()==Utils.DELIVERY_ENTREGANDO){
                    HashMap<String,String> map = new HashMap<>();
                    map.put("state","0");
                    map.put("addressStart","Domiciliario Pinchado");
                    map.put("positionStart",gson.toJson(domiciliario.getPosition()));

                    deliveryAcceptController.updateDelivery(delivery.get_id(),map);
                }

            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callClient();
            }
        });
        Log.i(TAG,"On ViewCreated2 Fragment Maps");
    }

    private void initServiceLocationDeliveryMode(){
        if(mBound) {
            if (!UtilsPreferences.getStateLocationUpdates(getActivity())) mService.requestLocationUpdates();
            mService.setModeEntregando();
        }
    }

    //######################################################################################
    //------------------------------ CICLO DE VIDA FRAGMENT --------------------------------
    //######################################################################################

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"On Start Fragment Maps");
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

        // Se vincula con el Servicio. Si el servicio se encuentra en modo FOREGROUND,
        // esta señal indica al Servicio que el Fragment esta en PrimerPlano y que
        // el Servicio debe pasar a trabajar BACKGROUND
        getActivity().bindService(new Intent(getActivity(), ForegroundLocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        Log.i(TAG,"On Stop Fragment Maps");
        if (mBound) {
            // Se vincula con el Servicio. Si el servicio se encuentra en modo BACKGROUND,
            // esta señal indica al Servicio que el Fragment deja de estar en Primer Plano y que
            // el Servicio debe pasar a trabajar FOREGROUND
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"On Resume Fragment Maps");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myReceiver, new IntentFilter(ForegroundLocationService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        Log.i(TAG,"On Pause Fragment Maps");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG,"On Atach Fragment Maps");
        try {
            listener = (FragmentsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement OnItemClickedListener");
        }
    }

    @Override
    public void onDetach() {
        Log.i(TAG,"On Detach Fragment Maps");
        listener = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //######################################################################################
    //--------------------------------- CONFIGURAR UI --------------------------------------
    //######################################################################################

    private void setCardViewDelivery(Delivery delivery, Client client) {
        //----------------------CONFIGURAMOS OBJETOS UI --------------------------------
        addressStart.setText(delivery.getAddressStart());
        addressEnd.setText(delivery.getAddressEnd());
        nameClient.setText(client.getName());
        phoneDelivery.setText(client.getPhone());
        Utils.imagePicasso(getContext(),client.getAvatar(),imageClient);
    }

    private void bindUI(View view) {
        imageClient = view.findViewById(R.id.image_client);
        nameClient = view.findViewById(R.id.txt_name_client);
        addressStart = view.findViewById(R.id.txt_addressStartClient);
        addressEnd = view.findViewById(R.id.txt_addressEndClient);
        phoneDelivery = view.findViewById(R.id.txt_phone);
        btnProblems = view.findViewById(R.id.btn_problem);
        btnCall = view.findViewById(R.id.btn_call);
        btnDeliveryOK = view.findViewById(R.id.btn_ok);
        btnGenRoute = view.findViewById(R.id.btn_gen_route);
        cargando = view.findViewById(R.id.layout_pb_map);
        textCargando = view.findViewById(R.id.txt_cargando_map);
    }

    //######################################################################################
    //------------------------------ PERMISOS DE UBICACION ---------------------------------
    //######################################################################################
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    getView().findViewById(R.id.domiciliario_fragment),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    }).show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback recibida cuando una peticion de permisos ha sido completada.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /**
         * Resivimos Permisos de Ubicacion
         */
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                Log.i(TAG, "Permisos Garantizados Fragment Map");
                initServiceLocationDeliveryMode();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                        getView().findViewById(R.id.domiciliario_fragment),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }

        /**
         * Resivimos Permisos para llamada
         */
        if(requestCode == PHONE_CALL_CODE){
            String permission = permissions[0];
            int result = grantResults[0];

            if (permission.equals(Manifest.permission.CALL_PHONE)) {
                // Comprobar si ha sido aceptado o denegado la petición de permiso
                if (result == PackageManager.PERMISSION_GRANTED) {
                    // Concedió su permiso
                    String phoneNumber = delivery.getPhone();
                    Intent intentCall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return;
                    startActivity(intentCall);
                }
                else {
                    // No concendió su permiso
                    Toast.makeText(getActivity(), "Tu negaste permiso para llamadas", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /**
     * Receiver for broadcasts sent by {@link ForegroundLocationService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(ForegroundLocationService.EXTRA_LOCATION);
            if (location != null) {
                markerDomiciliario.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s.equals(UtilsPreferences.KEY_STATE_LOCATION_UPDATES)) {
            Toast.makeText(getActivity(), ""+sharedPreferences.getBoolean(UtilsPreferences.KEY_STATE_LOCATION_UPDATES, false), Toast.LENGTH_SHORT).show();
        }

        if (s.equals(UtilsPreferences.KEY_DOMICILIARIO_FREE)) {
            Toast.makeText(getActivity(), ""+sharedPreferences.getBoolean(UtilsPreferences.KEY_DOMICILIARIO_FREE, false), Toast.LENGTH_SHORT).show();
            Toast.makeText(getActivity(), "Alguien acepto tu pedido", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
            UtilsPreferences.removeDelivery(preferences);
            UtilsPreferences.removeClient(preferences);
            listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
        }
    }

    //---------------------------- MAP READY AND LOCATION CHANGED -----------------------------
    //-----------------------------------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMinZoomPreference(13);
        gMap.setMaxZoomPreference(18);
        MarkerInit();
    }

    /*
    @Override
    public void onLocationChanged(Location location) {
        markerDomiciliario.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
    }
    */

    public void MarkerInit(){
        LatLng pasto = new LatLng(1.2080008824889852, -77.2782935335938);
        Location lastLocation = gson.fromJson(UtilsPreferences.getLastLocation(preferences),Location.class);
        if(lastLocation!=null)pasto = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());

        markerDomiciliario = gMap.addMarker(new MarkerOptions()
                .position(pasto)
                .title(getResources().getString(R.string.user_position_title))
                .snippet(getResources().getString(R.string.user_position_message))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_moto))
                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );

        markerAddressStart = gMap.addMarker(new MarkerOptions()
                .position(new LatLng(delivery.getPositionStart().getLat(),delivery.getPositionStart().getLng()))
                .title(getResources().getString(R.string.client_position_title))
                .snippet(getResources().getString(R.string.client_position_message))
                //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );

        CameraPosition camera = new CameraPosition.Builder()
                .target(pasto)
                .zoom(15)
                .tilt(0)
                .build();

        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera));
    }

    public void setCameraUserMap(){
        CameraPosition camera = new CameraPosition.Builder()
                .target(markerDomiciliario.getPosition())
                .zoom(17)
                .tilt(0)
                .build();

        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera));
    }

    public void setCameraPedidoMap(){
        CameraPosition camera = new CameraPosition.Builder()
                .target(markerAddressStart.getPosition())
                .zoom(17)
                .tilt(0)
                .build();

        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera));
    }

    //----------------------------EVENTOS MOSTRAR RUTA --------------------------------
    //---------------------------------------------------------------------------------
    private void getRutaGoogle(){
        if(markerDomiciliario!=null && markerAddressStart!=null) {
            try {
                new MostrarRuta(this, markerDomiciliario.getPosition(), markerAddressStart.getPosition()).execute();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }else {
            cargando.setVisibility(View.INVISIBLE);
            Toast.makeText(getContext(), "Imposible generar Ruta", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRutaLista(List<Route> routes) {
        if(routes!=null) {
            if (routes.size() > 0) dibujarRuta(routes.get(0).points);
            else Toast.makeText(getContext(), "Posicion del cliente DESCONOCIDA", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getContext(), "Asegurate de tener conexion a internet", Toast.LENGTH_LONG).show();
        }

        cargando.setVisibility(View.INVISIBLE);
    }

    public void dibujarRuta(List<LatLng> ruta){
        borrarRutas();
        polylineOptionsRuta = new PolylineOptions().
                geodesic(true).
                color(Color.argb(125, 255, 0, 0)).
                width(7);

        for (LatLng position : ruta) {
            polylineOptionsRuta.add(position);
        }

        polylineOptionsRuta.color(Color.argb(150, 255, 150, 0));
        polylineRuta = gMap.addPolyline(polylineOptionsRuta);
    }

    public  void borrarRutas(){
        if(polylineRuta != null) polylineRuta.remove();
    }

    //######################################################################################
    //-------------------------------------- Llamadas --------------------------------------
    //######################################################################################

    private void callClient() {
        String phoneNumber = delivery.getPhone();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            // comprobar version actual de android que estamos corriendo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Comprobar si ha aceptado, no ha aceptado, o nunca se le ha preguntado
                if (CheckPermission(Manifest.permission.CALL_PHONE)) {
                    // Ha aceptado
                    Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return;
                    startActivity(i);
                } else {
                    // Ha denegado o es la primera vez que se le pregunta
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                        // No se le ha preguntado aún
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PHONE_CALL_CODE);
                    } else {
                        // Ha denegado
                        Toast.makeText(getActivity(), "Please, enable the request permission", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setData(Uri.parse("package:" + getActivity().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(i);
                    }
                }
            } else {
                OlderVersions(phoneNumber);
            }
        } else {
            Toast.makeText(getActivity(), "Numero de telefono invalido", Toast.LENGTH_SHORT).show();
        }
    }

    private void OlderVersions(String phoneNumber) {
        Intent intentCall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        if (CheckPermission(Manifest.permission.CALL_PHONE)) {
            startActivity(intentCall);
        } else {
            Toast.makeText(getActivity(), "Tu no otorgaste permiso para llamadas", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean CheckPermission(String permission) {
        int result = getActivity().checkCallingOrSelfPermission(permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    //######################################################################################
    //--------------------------------- Funciones Api Rest ---------------------------------
    //######################################################################################

    @Override
    public void getDelivery(Delivery newDelivery) {
        if(newDelivery.getState()== Utils.DELIVERY_DISPONIBLE) {
            deliveryAcceptController.startDelivery(domiciliario.get_id(),newDelivery.get_id());
        }else {
            if (domiciliario.getState() != Utils.DOMICILIARIO_ENTREGANDO) {
                Toast.makeText(getActivity(), "Alguien mas acepto el pedido antes.", Toast.LENGTH_LONG).show();
                returnToDomiciliarioFragment();
            }
        }
    }

    @Override
    public void updateDeliverySuccessful(Delivery deliveryUpdated) {
        if(delivery.getState()==Utils.DELIVERY_RECOGIENDO){
            Toast.makeText(getContext(), "Has cancelado el pedido, debido a una emergencia", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
            UtilsPreferences.removeDelivery(preferences);
            UtilsPreferences.removeClient(preferences);
            listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
        }

        if(delivery.getState()==Utils.DELIVERY_ENTREGANDO){
            textCargando.setText("Esperando a que otro domiciliario acepte tu pedido");
            Toast.makeText(getContext(), "Esperando a que otro domiciliario acepte tu pedido", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void startDeliverySuccessful(String message) {
        Toast.makeText(getActivity(), "Tu cliente te esta esperando...", Toast.LENGTH_LONG).show();
        delivery.setState(1);
        UtilsPreferences.saveDelivery(preferences,gson.toJson(delivery));
    }

    @Override
    public void finishDeliverySuccessful(String message) {
        Toast.makeText(getActivity(), "Entrega completada con exito.", Toast.LENGTH_LONG).show();
        cargando.setVisibility(View.INVISIBLE);
        UtilsPreferences.removeDelivery(preferences);
        UtilsPreferences.removeClient(preferences);
        listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        Log.i(TAG, nameEvent + "  "+ code + "  " + errorMessage);
        if(nameEvent.contentEquals(DeliveryAcceptController.GET)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
            returnToDomiciliarioFragment();
        }

        if(nameEvent.contentEquals(DeliveryAcceptController.START)){
            if(code==500) Toast.makeText(getActivity(), "No se pudo completar la acción." , Toast.LENGTH_LONG).show();
            else Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            returnToDomiciliarioFragment();
        }

        if(nameEvent.contentEquals(DeliveryAcceptController.FINISH)){
            if(code==500) Toast.makeText(getActivity(), "No se pudo completar la acción" , Toast.LENGTH_LONG).show();
            else Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
        }

        if(nameEvent.contentEquals(DeliveryAcceptController.UPDATE)){
            if(code==500) Toast.makeText(getActivity(), "No se pudo completar la acción" , Toast.LENGTH_LONG).show();
            else Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
            if(!nameEvent.contentEquals(DeliveryAcceptController.FINISH)){
                returnToDomiciliarioFragment();
            }else{
                cargando.setVisibility(View.INVISIBLE);
            }
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión" , Toast.LENGTH_LONG).show();
    }

    public void returnToDomiciliarioFragment(){
        textCargando.setText("Finalizando Pedido");
        cargando.setVisibility(View.VISIBLE);
        UtilsPreferences.removeDelivery(preferences);
        UtilsPreferences.removeClient(preferences);
        cargando.setVisibility(View.INVISIBLE);
        listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
    }
}

