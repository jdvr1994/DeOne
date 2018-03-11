package com.apps.ing3ns.entregas.Fragmentos;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.API;
import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientController;
import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientListener;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryController;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryListener;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.API.APIServices.DomiciliarioService;
import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.Adaptadores.AdaptadorPedidos;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Client;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.Constants;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.android.gms.common.ConnectionResult;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;

/**
 * Created by JuanDa on 14/02/2018.
 */

public class DomiciliarioFragment extends Fragment implements DeliveryListener, ClientListener, DomiciliarioListener, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public SharedPreferences preferences;
    FragmentsListener listener;
    Gson gson = new GsonBuilder().create();
    private AdaptadorPedidos adapter;
    private LinearLayoutManager layoutManager;
    DeliveryController deliveryController;
    ClientController clientController;
    DomiciliarioController domiciliarioController;
    Domiciliario domiciliario;

    //-------------------------------------- GOOGLE API CLIENT ----------------------------
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    FusedLocationProviderApi fusedLocationProviderApi;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 198;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 3;
    Status statusGlobal;
    LocationManager locationManager;

    //---------------------Objetos UI ---------------------
    ImageView imgDomiciliario;
    TextView nameDomiciliario;
    TextView puntosDomiciliario;
    TextView pedidosDomiciliario;
    RecyclerView recyclerViewPedidos;
    RelativeLayout cargando;
    List<Delivery> deliveriesGlobal = new ArrayList<>();

    public DomiciliarioFragment() {
        // Required empty public constructor
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Deliveries Compare","Actualizacion");
            Toast.makeText(getContext(), "Pedidos actualizados", Toast.LENGTH_SHORT).show();
            String deliveriesJson = UtilsPreferences.getNearbyDeliveries(preferences);
            List<Delivery> deliveries = gson.fromJson(deliveriesJson,new TypeToken<List<Delivery>>(){}.getType());
            setRecyclerViewPedidos(deliveries);
        }
    };

    public void bindUI(View view){
        imgDomiciliario = view.findViewById(R.id.imagen_dom);
        nameDomiciliario = view.findViewById(R.id.nombre_dom);
        puntosDomiciliario = view.findViewById(R.id.puntos_dom);
        pedidosDomiciliario = view.findViewById(R.id.total_dom);
        recyclerViewPedidos = view.findViewById(R.id.recyclerViewPedidos);
        cargando = view.findViewById(R.id.layout_pb_delivery);
    }

    public void setCardViewDomiciliario(Domiciliario domiciliario){
        Utils.imagePicassoRounded(getContext(),domiciliario.getAvatar(),imgDomiciliario);
        nameDomiciliario.setText(domiciliario.getName());
        puntosDomiciliario.setText(String.valueOf(domiciliario.getCoins()));
        pedidosDomiciliario.setText(String.valueOf(domiciliario.getDeliveries().size()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_domiciliario, container, false);
        bindUI(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        FirebaseMessaging.getInstance().subscribeToTopic(Utils.TOPIC_STATE_1);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_0);
        FirebaseMessaging.getInstance().unsubscribeFromTopic(Utils.TOPIC_STATE_2);

        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        setCardViewDomiciliario(domiciliario);

        deliveryController = new DeliveryController(this);
        clientController = new ClientController(this);
        domiciliarioController = new DomiciliarioController(this);

        List<Delivery> deliveries = new ArrayList<>();
        adapter = new AdaptadorPedidos(deliveries, R.layout.cardview_pedido, getActivity(), new AdaptadorPedidos.OnItemClickListener() {
            @Override
            public void onItemClick(Delivery delivery, int position) {
                cargando.setVisibility(View.VISIBLE);
                UtilsPreferences.saveDelivery(preferences,gson.toJson(delivery));
                clientController.getClient(UtilsPreferences.getToken(preferences),delivery.getClient());
            }
        });

        domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapTokenAndState(1));

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerViewPedidos.setHasFixedSize(true);
        recyclerViewPedidos.setLayoutManager(layoutManager);
        recyclerViewPedidos.setAdapter(adapter);

        gpsServiceAction(Constants.ACTION.START_FOREGROUND);
        //--------------------------- INICIAMOS Y ACTIVAMOS EL GOOGLE API CLIENT ------------------------
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        PermissionsGPS();
    }

    //######################################################################################
    //------------------------------ PERMISOS DE UBICACION ---------------------------------
    //######################################################################################
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

    //######################################################################################
    //--------------------------------- CICLO DE VIDA FRAGMENT ---------------------------------
    //######################################################################################
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
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        Log.d("DOMICILIARIO FRAG","On Stop");
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
        super.onStop();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //######################################################################################
    //--------------------------------- LOCATION CHANGE ---------------------------------
    //######################################################################################
    @Override
    public void onLocationChanged(Location location) {

    }

    //######################################################################################
    //--------------------------- GOOGLE API CLIENT LOCATION ------------------------------
    //######################################################################################
    public void googleApiLocationActive() {
        mLocationRequest = null;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mLocationRequest.setInterval(30*60*1000);
        mLocationRequest.setFastestInterval(30*60*1000);

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

    //-------------------------- START AND STOP SERVICE FOREGROUND -------------------------
    public void gpsServiceAction(String action){
        Intent startIntent = new Intent(getActivity(), ForegroundService.class);
        startIntent.setAction(action);
        getActivity().startService(startIntent);
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

        if(mGoogleApiClient.isConnected()){
            fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            gpsServiceAction(Constants.ACTION.START_FOREGROUND_SHARE);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver((mMessageReceiver),
                    new IntentFilter(ForegroundService.INTENT_ACTION)
            );
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    //######################################################################################
    //--------------------------------- SET RECYCLER VIEW ---------------------------------
    //######################################################################################

    public void setRecyclerViewPedidos(List<Delivery> deliveries){
        adapter.setDeliveries(deliveries);
        adapter.notifyDataSetChanged();
        recyclerViewPedidos.smoothScrollToPosition(deliveries.size() - 1);
    }

    //######################################################################################
    //--------------------------------- Funciones Api Rest ---------------------------------
    //######################################################################################

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
    }

    @Override
    public void getClientSuccessful(Client client) {
        UtilsPreferences.saveClient(preferences,gson.toJson(client));
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
        listener.setOnChangeToMap(Utils.KEY_DOMICILIARIO_FRAGMENT,R.id.cardview_dom);
    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliarioUpdated));
        domiciliario = domiciliarioUpdated;
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DeliveryController.GETALL)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(ClientController.GET)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }

        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(DeliveryController.GETCONDITION)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DeliveryController.GETALL)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
            deliveryController.getDeliveries();
        }

        if(nameEvent.contentEquals(ClientController.GET)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }

        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
        }

        if(nameEvent.contentEquals(DeliveryController.GETCONDITION)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
        }
    }
}
