package com.apps.ing3ns.entregas.Fragmentos;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientController;
import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientListener;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryController;
import com.apps.ing3ns.entregas.API.APIControllers.Delivery.DeliveryListener;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Actividades.MainActivity;
import com.apps.ing3ns.entregas.Adaptadores.AdaptadorPedidos;
import com.apps.ing3ns.entregas.BuildConfig;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Modelos.Client;
import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.NuevoGPS.LocationStatePreferences;
import com.apps.ing3ns.entregas.NuevoGPS.LocationResultNotification;
import com.apps.ing3ns.entregas.NuevoGPS.LocationUpdatesBroadcastReceiver;
import com.apps.ing3ns.entregas.NuevoGPS.LocationUpdatesIntentService;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.Constants;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuanDa on 14/02/2018.
 */

public class DomiciliarioFragment extends Fragment implements DeliveryListener, ClientListener, DomiciliarioListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    //-------------------Nuevo Servicio de Posicionamiento --------------------------
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static final long UPDATE_INTERVAL = 10 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;

    //-------------------------------------------------------------------------------

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

    FusedLocationProviderApi fusedLocationProviderApi;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 198;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_domiciliario, container, false);
        bindUI(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        setCardViewDomiciliario(domiciliario);

        deliveryController = new DeliveryController(this);
        clientController = new ClientController(this);
        domiciliarioController = new DomiciliarioController(this);

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

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

        recyclerViewPedidos.setHasFixedSize(true);
        recyclerViewPedidos.setLayoutManager(layoutManager);
        recyclerViewPedidos.setAdapter(adapter);

        imgDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestLocationUpdates();
            }
        });

        puntosDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(getActivity(), ForegroundLocationService.class);
                startIntent.setAction(Constants.ACTION.START_FOREGROUND);
                getActivity().startService(startIntent);
            }
        });

        pedidosDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeLocationUpdates();
                Intent startIntent = new Intent(getActivity(), ForegroundLocationService.class);
                startIntent.setAction(Constants.ACTION.STOP_FOREGROUND);
                getActivity().startService(startIntent);
            }
        });

        // Check if the user revoked runtime permissions.
        if (!checkPermissions()) {
            requestPermissions();
        }

        buildGoogleApiClient();
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
     * Callback received when a permissions request has been completed.
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

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver((mMessageReceiver), new IntentFilter(ForegroundService.INTENT_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Toast.makeText(getContext(), LocationResultNotification.getSavedLocationResult(getActivity()), Toast.LENGTH_SHORT).show();
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
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    //######################################################################################
    //--------------------------- GOOGLE API CLIENT LOCATION ------------------------------
    //######################################################################################
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);
    }

    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        createLocationRequest();

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(getActivity(), this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
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

        if (!LocationStatePreferences.getRequesting(getActivity())) {
            requestLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        final String text = "Connection suspended";
        Log.w(TAG, text + ": Error code: " + i);
        showSnackbar("Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        final String text = "Exception while connecting to Google Play services";
        Log.w(TAG, text + ": " + connectionResult.getErrorMessage());
        showSnackbar(text);
    }

    private void showSnackbar(final String text) {
        View container = getView().findViewById(R.id.domiciliario_fragment);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(LocationResultNotification.KEY_LOCATION_UPDATES_RESULT)) {
            Toast.makeText(getContext(), LocationResultNotification.getSavedLocationResult(getContext()), Toast.LENGTH_SHORT).show();
        } else if (s.equals(LocationStatePreferences.KEY_LOCATION_UPDATES_REQUESTED)) {
            updateButtonsState(LocationStatePreferences.getRequesting(getContext()));
        }
    }


    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(getActivity(), LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates");
            LocationStatePreferences.setRequesting(getContext(), true);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, getPendingIntent());
        } catch (SecurityException e) {
            LocationStatePreferences.setRequesting(getContext(), false);
            e.printStackTrace();
        }
    }

    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        LocationStatePreferences.setRequesting(getContext(), false);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getPendingIntent());
    }

    private void updateButtonsState(boolean requestingLocationUpdates) {

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
        setRecyclerViewPedidos(deliveries);
    }

    @Override
    public void getDelivery(Delivery delivery) {

    }

    @Override
    public void updateDeliverySuccessful(Delivery delivery) {

    }

    @Override
    public void getDeliveriesConditionSuccessful(List<Delivery> deliveries) {
        setRecyclerViewPedidos(deliveries);
        UtilsPreferences.saveDeliveries(preferences,gson.toJson(deliveries));
    }

    @Override
    public void getClientSuccessful(Client client) {
        UtilsPreferences.saveClient(preferences,gson.toJson(client));
        removeLocationUpdates();
        listener.setOnChangeToMap(Utils.KEY_DOMICILIARIO_FRAGMENT,R.id.cardview_dom);
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
