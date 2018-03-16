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
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundLocationService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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

public class DomiciliarioFragment extends Fragment implements DeliveryListener, ClientListener, DomiciliarioListener, SharedPreferences.OnSharedPreferenceChangeListener {

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
            Log.i(TAG,"On Service Connected Fragment Domiciliario");
            ForegroundLocationService.LocalBinder binder = (ForegroundLocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Comienzo la rutina para peticion de PERMISOS y Acceso a la UBICACION
            if (!checkPermissions()) requestPermissions();
            else initServiceLocationSearchMode();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,"On Services Disconnected Fragment Domiciliario");
            mService = null;
            mBound = false;
        }
    };

    //######################################################################################
    //------------------------------ VARIABLES DE PROCESO ----------------------------------
    //######################################################################################
    /**
     * Objetos y Controladores de eventos API REST.
     */
    private DeliveryController deliveryController;
    private ClientController clientController;
    private DomiciliarioController domiciliarioController;
    private Domiciliario domiciliario;

    /**
     * GSON y Variables para RecyclerView.
     */
    private Gson gson = new GsonBuilder().create();
    private AdaptadorPedidos adapter;
    private LinearLayoutManager layoutManager;

    /**
     * Otras varibales de proceso.
     */
    private SharedPreferences preferences;
    private FragmentsListener listener;

    //######################################################################################
    //---------------------------- DEFINICION DE OBJETOS UI --------------------------------
    //######################################################################################
    ImageView imgDomiciliario;
    TextView nameDomiciliario;
    TextView puntosDomiciliario;
    TextView pedidosDomiciliario;
    RecyclerView recyclerViewPedidos;
    RelativeLayout cargando;

    //######################################################################################
    //------------------------------ ON CREATE Y SIMILARES----------------------------------
    //######################################################################################

    public DomiciliarioFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_domiciliario, container, false);
        Log.i(TAG,"On CreateView Fragment Domiciliario");
        myReceiver = new MyReceiver();
        // Inicializo los controladores de eventos API REST
        deliveryController = new DeliveryController(this);
        clientController = new ClientController(this);
        domiciliarioController = new DomiciliarioController(this);
        bindUI(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"On View Created Fragment Domiciliario");
        // Rescato informacion del domiciliario atravez de las SharedPreferences y la VISUALIZO.
        preferences = getActivity().getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences),Domiciliario.class);
        setCardViewDomiciliario(domiciliario);

        // Actualizo el estado del DOMICILIARIO.
        domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapTokenAndState(1));

        // Configuro e inicializo lo necesario para la visualizacion del RecyclerView
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

        recyclerViewPedidos.setHasFixedSize(true);
        recyclerViewPedidos.setLayoutManager(layoutManager);
        recyclerViewPedidos.setAdapter(adapter);

        // Eventos enlazados a los Elementos UI
        imgDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!UtilsPreferences.getStateLocationUpdates(getActivity())) mService.requestLocationUpdates();
            }
        });

        puntosDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(UtilsPreferences.getStateLocationUpdates(getActivity()))mService.removeLocationUpdates();
            }
        });

        pedidosDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        Log.i(TAG,"On ViewCreated 2 Fragment Domiciliario");
    }

    private void initServiceLocationSearchMode(){
        if(mBound) {
            if (!UtilsPreferences.getStateLocationUpdates(getActivity())) mService.requestLocationUpdates();
            mService.setModeSearch();
        }
    }

    //######################################################################################
    //------------------------------ CICLO DE VIDA FRAGMENT --------------------------------
    //######################################################################################

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"On Start Fragment Domiciliario");
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

        // Se vincula con el Servicio. Si el servicio se encuentra en modo FOREGROUND,
        // esta señal indica al Servicio que el Fragment esta en PrimerPlano y que
        // el Servicio debe pasar a trabajar BACKGROUND
        getActivity().bindService(new Intent(getActivity(), ForegroundLocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        Log.i(TAG,"On Stop Fragment Domiciliario");
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
        Log.i(TAG,"On Resume Fragment Domiciliario");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myReceiver, new IntentFilter(ForegroundLocationService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        Log.i(TAG,"On Pause Fragment Domiciliario");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG,"On Attach Fragment Domiciliario");
        try {
            listener = (FragmentsListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement OnItemClickedListener");
        }
    }

    @Override
    public void onDetach() {
        Log.i(TAG,"Fragment Domiciliario Detach");
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
                Log.i(TAG, "Permisos Garantizados Fragment Domiciliario");
                initServiceLocationSearchMode();
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

            String nearbyDeliveriesJson = intent.getStringExtra(ForegroundLocationService.EXTRA_NEARBY_DELIVERIES);
            if(nearbyDeliveriesJson!=null){
                Toast.makeText(getActivity(), "Pedidos Cercanos Actualizados", Toast.LENGTH_SHORT).show();
                List<Delivery> nearbyDeliveries =  gson.fromJson(nearbyDeliveriesJson,new TypeToken<List<Delivery>>(){}.getType());
                adapter.setDeliveries(nearbyDeliveries);
                adapter.notifyDataSetChanged();
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

    }

    @Override
    public void getClientSuccessful(Client client) {
        UtilsPreferences.saveClient(preferences,gson.toJson(client));
        listener.setOnChangeToMap(Utils.KEY_DOMICILIARIO_FRAGMENT,R.id.cardview_dom);
    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliarioUpdated));
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
