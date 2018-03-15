package com.apps.ing3ns.entregas.Fragmentos;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
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

import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryController;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryListener;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.BuildConfig;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Client;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.Constants;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.apps.ing3ns.entregas.modelsRoutes.MostrarRuta;
import com.apps.ing3ns.entregas.modelsRoutes.MostrarRutaListener;
import com.apps.ing3ns.entregas.modelsRoutes.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements MostrarRutaListener,OnMapReadyCallback, DeliveryListener, DomiciliarioListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    //######################################################################################
    //---------------- NUEVO SERVICIO DE UBICACION BACKGROUND/FOREGROUND -------------------
    //######################################################################################
    /**
     * Variables usadas para peticion de Permisos de Ubicacion.
     */
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Variables usadas para activar el GPS del dispositivo por medio de @GoogleApiClient.
     */
    final static int REQUEST_LOCATION = 198;
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> result;
    private Status statusGlobal;

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
            ForegroundLocationService.LocalBinder binder = (ForegroundLocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
    private DeliveryController deliveryController;
    private DomiciliarioController domiciliarioController;
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

        myReceiver = new MyReceiver();
        // Inicializo los controladores de eventos API REST
        deliveryController = new DeliveryController(this);
        domiciliarioController = new DomiciliarioController(this);
        gson = new GsonBuilder().create();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindUI(view);

        // Rescato informacion del domiciliario, cliente y delivery
        // atravez de las SharedPreferences y la VISUALIZO.
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences),Delivery.class);
        client = gson.fromJson(UtilsPreferences.getClient(preferences),Client.class);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        setCardViewDelivery(delivery,client);

        // Recupero el Delivery desde el Server para verificar que esta en estado 0
        // Asignarle el domiciliario y actualizar su estado
        deliveryController.getDelivery(delivery.get_id());

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

                // Llamo al metodo DeliveryFinished del API Rest , le envio una id_domiciliario y una id_delivery
                // Este metodo hara lo siguiente:::
                // Actualizo el estado del Delivery a 3
                // Agrego el id_delivery al domiciliario
                // SI SU RESPUESTA ES CORRECTA :
                /*
                    cargando.setVisibility(View.INVISIBLE);
                    UtilsPreferences.removeDelivery(preferences);
                    UtilsPreferences.removeClient(preferences);
                    gpsServiceAction(Constants.ACTION.STOP_FOREGROUND);
                    listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
                 */
                // SI SU RESPUESTA ES ERROR:

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

            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Comienzo la rutina para peticion de PERMISOS y Acceso a la UBICACION
        if (!checkPermissions()) {
            requestPermissions();
        }
        buildGoogleApiClient();
    }

    //######################################################################################
    //------------------------------ CICLO DE VIDA FRAGMENT --------------------------------
    //######################################################################################

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

        // Se vincula con el Servicio. Si el servicio se encuentra en modo FOREGROUND,
        // esta señal indica al Servicio que el Fragment esta en PrimerPlano y que
        // el Servicio debe pasar a trabajar BACKGROUND
        getActivity().bindService(new Intent(getActivity(), ForegroundLocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
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
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myReceiver, new IntentFilter(ForegroundLocationService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (FragmentsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement OnItemClickedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                // GoogleApiClient.
                buildGoogleApiClient();
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
    }

    //######################################################################################
    //--------------------------- GOOGLE API CLIENT LOCATION ------------------------------
    //######################################################################################
    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(getActivity(), this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    /**
     * Callback recibido cuando el resultado de una conexion es completada (@GoogleApiClient).
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(new LocationRequest());
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                statusGlobal = status;
                if (status.getStatusCode()== LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(getActivity(), REQUEST_LOCATION);
                    } catch (IntentSender.SendIntentException e) {}
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(!UtilsPreferences.getStateLocationUpdates(getActivity())) {
            mService.requestLocationUpdates();
        }
        mService.setModeEntregando();
    }

    /**
     * Callback recibido cuando una conexion fue suspendida (@GoogleApiClient).
     */
    @Override
    public void onConnectionSuspended(int i) {
        final String text = "Connection suspended";
        Log.w(TAG, text + ": Error code: " + i);
        showSnackbar("Connection suspended");
    }

    /**
     * Callback recibido cuando una conexion fue fallida  (@GoogleApiClient).
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        final String text = "Exception while connecting to Google Play services";
        Log.w(TAG, text + ": " + connectionResult.getErrorMessage());
        showSnackbar(text);
    }

    /**
     * Callback recibido cuando el resultado de una Resolucion es completada (@GoogleApiClient Connection).
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //--------------------------GOOGLE API CLIENT --------------------------
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        buildGoogleApiClient();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        try {
                            statusGlobal.startResolutionForResult(getActivity(), REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }
                    default: {
                        break;
                    }
                }
                break;
        }
    }


    private void showSnackbar(final String text) {
        View container = getView().findViewById(R.id.domiciliario_fragment);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
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
                Toast.makeText(getActivity(), Utils.getLocationText(location), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s.equals(UtilsPreferences.KEY_STATE_LOCATION_UPDATES)) {
            Toast.makeText(getActivity(), ""+sharedPreferences.getBoolean(UtilsPreferences.KEY_STATE_LOCATION_UPDATES, false), Toast.LENGTH_SHORT).show();
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
        markerDomiciliario = gMap.addMarker(new MarkerOptions()
                .position(pasto)
                .title(getResources().getString(R.string.user_position_title))
                .snippet(getResources().getString(R.string.user_position_message))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_origen))
        );

        markerAddressStart = gMap.addMarker(new MarkerOptions()
                .position(new LatLng(delivery.getPositionStart().getLat(),delivery.getPositionStart().getLng()))
                .title(getResources().getString(R.string.client_position_title))
                .snippet(getResources().getString(R.string.client_position_message))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_destino))
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
        if(routes.size()>0)dibujarRuta(routes.get(0).points);
        else Toast.makeText(getContext(), "Posicion del cliente DESCONOCIDA", Toast.LENGTH_LONG).show();

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

    //-------------------------- START AND STOP SERVICE FOREGROUND -------------------------
    public void gpsServiceAction(String action){
        Intent startIntent = new Intent(getActivity(), ForegroundService.class);
        //startIntent.putExtra("usuario",gson.toJson(pedido.getIdUsuario()));
        startIntent.setAction(action);
        getActivity().startService(startIntent);
    }

    @Override
    public void getDeliveries(List<Delivery> deliveries) {

    }

    @Override
    public void getDelivery(Delivery newDelivery) {
        if(newDelivery.getState()==0) {
            // Llamo al metodo StartDelivery del API Rest , le envio una id_domiciliario y una id_delivery
            // Este metodo hara lo siguiente:::
            // Actualiza al delivery con la id_domiciliario y su estado a 1
            // Actualiza el estado del domiciliario a 2
            // SI SU RESPUESTA ES CORRECTA :
                /*
                    Toast.makeText(getActivity(), "Tu has aceptado este pedido correctamente... Tu cliente te esta esperando", Toast.LENGTH_LONG).show();
                 */
            // SI SU RESPUESTA ES ERROR:
            /*
                Toast.makeText(getActivity(), "Ocurrio tal error", Toast.LENGTH_LONG).show();
                returnToDomiciliarioFragment();
             */
        }else{
            if(domiciliario.getState()!=2) {
                Toast.makeText(getActivity(), "Alguien mas acepto el pedido antes", Toast.LENGTH_LONG).show();
                returnToDomiciliarioFragment();
            }
        }
    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {

    }

    @Override
    public void getDeliveriesConditionSuccessful(List<Delivery> deliveries) {

    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {

    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DeliveryController.GET)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
            returnToDomiciliarioFragment();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DeliveryController.GET)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión" , Toast.LENGTH_LONG).show();
            returnToDomiciliarioFragment();
        }
    }

    public void returnToDomiciliarioFragment(){
        textCargando.setText("Finalizando Pedido");
        cargando.setVisibility(View.VISIBLE);
        UtilsPreferences.removeDelivery(preferences);
        UtilsPreferences.removeClient(preferences);
        gpsServiceAction(Constants.ACTION.STOP_FOREGROUND);
        cargando.setVisibility(View.INVISIBLE);
        listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
    }
}

