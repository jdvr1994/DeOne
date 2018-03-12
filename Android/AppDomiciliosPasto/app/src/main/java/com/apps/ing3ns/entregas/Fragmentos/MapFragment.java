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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements MostrarRutaListener,OnMapReadyCallback, DeliveryListener, DomiciliarioListener {

    private final int PHONE_CALL_CODE = 100;
    //----------------------- SHARED PREFERENCES-----------------
    public SharedPreferences preferences;
    FragmentsListener listener;
    DeliveryController deliveryController;
    DomiciliarioController domiciliarioController;
    Gson gson;

    PolylineOptions polylineOptionsRuta;
    Polyline lineCamino;

    private View rootView;
    private GoogleMap gMap;
    private MapView mapView;
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
        Log.d("MAP FRAGMENT","On Create View");
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("MAP FRAGMENT","On View Created");
        FirebaseMessaging.getInstance().subscribeToTopic(Utils.TOPIC_STATE_2);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_0);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_1);

        bindUI(view);
        deliveryController = new DeliveryController(this);
        domiciliarioController = new DomiciliarioController(this);
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        //delivery = gson.fromJson(UtilsPreferences.getDelivery(preferences),Delivery.class);
        //client = gson.fromJson(UtilsPreferences.getClient(preferences),Client.class);
        //domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        //deliveryController.getDelivery(delivery.get_id());

        //setCardViewDelivery(delivery,client);


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

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                llamarUsuario();
            }
        });

        Log.d("MAP FRAGMENT","On View Created 2");

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


    //------------------------------- PERMISOS TELEFONO ------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PHONE_CALL_CODE:
                String permission = permissions[0];
                int result = grantResults[0];
                if (permission.equals(Manifest.permission.CALL_PHONE)) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        String phoneNumber = delivery.getPhone();
                        Intent intentCall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return;
                        startActivity(intentCall);
                    }else {
                        Toast.makeText(getActivity(), "Tu negaste el permiso para llamadas", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private boolean CheckPermission(String permission) {
        int result = getActivity().checkCallingOrSelfPermission(permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void OlderVersions(String phoneNumber) {
        Intent intentCall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        if (CheckPermission(Manifest.permission.CALL_PHONE)) {
            startActivity(intentCall);
        } else {
            Toast.makeText(getActivity(), "Tu negaste el permiso para llamadas", Toast.LENGTH_SHORT).show();
        }
    }

    private void llamarUsuario() {
        String phoneNumber = delivery.getPhone();
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (CheckPermission(Manifest.permission.CALL_PHONE)) {
                    Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return;
                    startActivity(i);
                } else {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PHONE_CALL_CODE);
                    } else {
                        Toast.makeText(getActivity(), "Por favor habilita los permisos de Telefono o Llamadas", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getActivity(), "No se puede acceder al numero de telefono", Toast.LENGTH_SHORT).show();
        }
    }

    //-------------------------CLICLO DE VIDA FRAGMENT---------------------------------
    //---------------------------------------------------------------------------------

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
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

        // Si esta muy cerca del pedido entonces actualizo el DELIVERY A ESTADO 2
        // deliveryController.updateDelivery(delivery.get_id(),Utils.getHashMapState(2));
    }
    */

    public void MarkerInit(){
        Location location = gson.fromJson(UtilsPreferences.getLastLocation(preferences), Location.class);
        LatLng pasto = new LatLng(location.getLatitude(),location.getLongitude());
        markerDomiciliario = gMap.addMarker(new MarkerOptions()
                .position(pasto)
                .title(getResources().getString(R.string.user_position_title))
                .snippet(getResources().getString(R.string.user_position_message))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_marker_origen))
        );

        markerAddressStart = gMap.addMarker(new MarkerOptions()
                //.position(new LatLng(client.getPosition().getLat(),client.getPosition().getLng()))
                .position(pasto)
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
        if(routes!=null) {
            if (routes.size() > 0) dibujarRuta(routes.get(0).points);
            else Toast.makeText(getContext(), "Posicion del cliente DESCONOCIDA", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getContext(), "No tienes conexion a internet", Toast.LENGTH_LONG).show();
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
        lineCamino = gMap.addPolyline(polylineOptionsRuta);
    }

    public  void borrarRutas(){
        if(lineCamino != null)lineCamino.remove();
    }

    //-------------------------- START AND STOP SERVICE FOREGROUND -------------------------
    public void gpsServiceAction(String action){
        Intent startIntent = new Intent(getActivity(), ForegroundService.class);
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
        }else{
            if(domiciliario.getState()!=2) {
                Toast.makeText(getActivity(), "Alguien mas acepto el pedido antes", Toast.LENGTH_LONG).show();
                returnToDomiciliarioFragment();
            }
        }
    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {
        if(delivery.getState()==1){
            domiciliarioController.updateDomiciliario(domiciliario.get_id(), Utils.getHashMapState(2));
        }

        if(delivery.getState()==3) {
            cargando.setVisibility(View.INVISIBLE);
            UtilsPreferences.removeDelivery(preferences);
            UtilsPreferences.removeClient(preferences);
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
            deliveryController.updateDelivery(delivery.get_id(),Utils.getHashMapState(3));
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
        cargando.setVisibility(View.INVISIBLE);
        listener.setOnChangeToDomiciliario(Utils.KEY_MAP_FRAGMENT, R.id.btn_ok);
    }
}

