package com.apps.ing3ns.entregas.Fragmentos;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
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
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Client;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.Constants;
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
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
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
public class MapFragment extends Fragment implements MostrarRutaListener,OnMapReadyCallback,LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DeliveryListener, DomiciliarioListener {
    //----------------------- SHARED PREFERENCES-----------------
    public SharedPreferences preferences;
    FragmentsListener listener;
    DeliveryController deliveryController;
    DomiciliarioController domiciliarioController;
    Gson gson;
    //-------------------------------------- GOOGLE API CLIENT ----------------------------
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    FusedLocationProviderApi fusedLocationProviderApi;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 198;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3;
    Status statusGlobal;

    PolylineOptions polylineOptionsRuta;
    Polyline lineCamino;

    private View rootView;
    private GoogleMap gMap;
    private MapView mapView;
    LocationManager locationManager;
    private Marker markerDomiciliario;
    private Marker markerAddressStart;
    Delivery delivery;
    Client client;
    Domiciliario domiciliario;

    //---------------OBEJTOS UI--------------------------
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

    public MapFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        gson = new GsonBuilder().create();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindUI(view);
        deliveryController = new DeliveryController(this);
        domiciliarioController = new DomiciliarioController(this);
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences),Delivery.class);
        client = gson.fromJson(UtilsPreferences.getClient(preferences),Client.class);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        deliveryController.getDelivery(delivery.get_id());

        setCardViewDelivery(delivery,client);


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

        //--------------------------- INICIAMOS Y ACTIVAMOS EL GOOGLE API CLIENT ------------------------
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        googleApiClientInit();
        PermissionsGPS();

        btnDeliveryOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textCargando.setText("Finalizando Pedido");
                cargando.setVisibility(View.VISIBLE);
                domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapAddDelivery(gson.toJson(domiciliario.getDeliveries()),delivery.get_id()));
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
                textCargando.setText("Cargando solicitud");
                cargando.setVisibility(View.VISIBLE);
                domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.deleteDeliveries());
            }
        });
    }

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

    //------------------------------------------PERMISOS DE UBICACION GPS -----------------------------------
    //------------------------------------------------------------------------------------------------------
    public void PermissionsGPS() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
            return;
        } else {
            googleApiLocationActive();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    googleApiLocationActive();
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
                return;
            }
        }
    }

    //-------------------------CLICLO DE VIDA FRAGMENT---------------------------------
    //---------------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //--------------------------GOOGLE API CLIENT --------------------------
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        googleApiLocationActive();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        try {
                            if(!mGoogleApiClient.isConnected()) statusGlobal.startResolutionForResult(getActivity(), REQUEST_LOCATION);
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

    @Override
    public void onResume() {
        super.onResume();
        googleApiClientInit();
        if (!mGoogleApiClient.isConnected()) {
            PermissionsGPS();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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

    //---------------------------- MAP READY AND LOCATION CHANGED -----------------------------
    //-----------------------------------------------------------------------------------------

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMinZoomPreference(13);
        gMap.setMaxZoomPreference(18);
        MarkerInit();
    }

    @Override
    public void onLocationChanged(Location location) {
        markerDomiciliario.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
    }

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

    //------------------------------------GOOGLE API CLIENT LOCATION -----------------------------
    //-----------------------------------------------------------------------------------------
    public void googleApiLocationActive() {
        mLocationRequest = null;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(30* 1000);

        fusedLocationProviderApi = LocationServices.FusedLocationApi;

        googleApiClientInit();
        mGoogleApiClient.connect();
    }

    public void googleApiClientInit(){
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
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
                final Status status = result.getStatus();
                statusGlobal = status;
                if (status.getStatusCode()==LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        status.startResolutionForResult(getActivity(), REQUEST_LOCATION);
                    } catch (IntentSender.SendIntentException e) {}
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(mGoogleApiClient.isConnected()){
            fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            gpsServiceAction(Constants.ACTION.START_FOREGROUND_SHARE);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
        lineCamino = gMap.addPolyline(polylineOptionsRuta);
    }

    public  void borrarRutas(){
        if(lineCamino != null)lineCamino.remove();
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
            delivery.setDomiciliario(domiciliario.get_id());
            delivery.setState(1);

            HashMap<String, String> map = new HashMap<>();
            map.put("domiciliario", delivery.getDomiciliario());
            map.put("state", String.valueOf(delivery.getState()));
            deliveryController.updateDelivery(delivery.get_id(), map);

            domiciliarioController.updateDomiciliario(domiciliario.get_id(), Utils.getHashMapState(2));
        }else{
            if(domiciliario.getState()!=2) {
                Toast.makeText(getActivity(), "Alguien mas acepto el pedido antes", Toast.LENGTH_LONG).show();
                returnToDomiciliarioFragment();
            }
        }
    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {
        if(delivery.getState()==2) {
            cargando.setVisibility(View.INVISIBLE);
            UtilsPreferences.removeDelivery(preferences);
            UtilsPreferences.removeClient(preferences);
            gpsServiceAction(Constants.ACTION.STOP_FOREGROUND);
            listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
        }
    }

    @Override
    public void getDeliveriesConditionSuccessful(List<Delivery> deliveries) {

    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliarioUpdated));
        if(domiciliario.getDeliveries().size()!=domiciliarioUpdated.getDeliveries().size()) {
            deliveryController.updateDelivery(delivery.get_id(),Utils.getHashMapState(2));
        }
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DeliveryController.UPDATE)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(DeliveryController.GET)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
            returnToDomiciliarioFragment();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DeliveryController.UPDATE)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
        }

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

