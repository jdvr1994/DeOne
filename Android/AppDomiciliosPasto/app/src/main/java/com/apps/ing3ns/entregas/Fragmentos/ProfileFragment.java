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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientController;
import com.apps.ing3ns.entregas.API.APIControllers.Client.ClientListener;
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by JuanDa on 14/02/2018.
 */

public class ProfileFragment extends Fragment implements  DomiciliarioListener {

    //######################################################################################
    //---------------- NUEVO SERVICIO DE UBICACION BACKGROUND/FOREGROUND -------------------
    //######################################################################################
    /**
     * Variables usadas para peticion de Permisos de Ubicacion.
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Variables usadas para administrar la dinamica de Vinculacion y Conexion del Servicio.
     */
    private ForegroundLocationService mService = null;

    //######################################################################################
    //------------------------------ VARIABLES DE PROCESO ----------------------------------
    //######################################################################################
    /**
     * Objetos y Controladores de eventos API REST.
     */
    private DomiciliarioController domiciliarioController;
    private Domiciliario domiciliario;

    /**
     * GSON y Variables para RecyclerView.
     */
    private Gson gson = new GsonBuilder().create();

    /**
     * Otras varibales de proceso.
     */
    private SharedPreferences preferences;

    //######################################################################################
    //---------------------------- DEFINICION DE OBJETOS UI --------------------------------
    //######################################################################################
    ImageView imgDomiciliario;
    TextView nameDomiciliario;
    TextView puntosDomiciliario;
    TextView pedidosDomiciliario;
    RelativeLayout cargando;
    EditText etChangePass;
    EditText etChangePass2;
    Button btnChange;

    //######################################################################################
    //------------------------------ ON CREATE Y SIMILARES----------------------------------
    //######################################################################################

    public ProfileFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.i(TAG,"On CreateView Fragment Domiciliario");
        // Inicializo los controladores de eventos API REST
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

        imgDomiciliario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Eres un gran Domiciliario", Toast.LENGTH_SHORT).show();
            }
        });

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String pass = etChangePass.getText().toString();
                String pass2 = etChangePass2.getText().toString();

                if(pass.length()>0 & pass2.length()>0){
                    if(pass.equals(pass2)){
                        domiciliarioController.updateDomiciliario(domiciliario.get_id(),new HashMap<String, String>(){{put("password",pass);}});
                        cargando.setVisibility(View.VISIBLE);
                    }else Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                }

            }
        });

        Log.i(TAG,"On ViewCreated 2 Fragment Domiciliario");
    }

    //######################################################################################
    //------------------------------ CICLO DE VIDA FRAGMENT --------------------------------
    //######################################################################################

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"On Start Fragment Domiciliario");
    }

    @Override
    public void onStop() {
        Log.i(TAG,"On Stop Fragment Domiciliario");
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"On Resume Fragment Domiciliario");
    }

    @Override
    public void onPause() {
        Log.i(TAG,"On Pause Fragment Domiciliario");
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG,"On Attach Fragment Domiciliario");
    }

    @Override
    public void onDetach() {
        Log.i(TAG,"Fragment Domiciliario Detach");
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
        cargando = view.findViewById(R.id.layout_pb_delivery);
        etChangePass = view.findViewById(R.id.editTextPassChange);
        etChangePass2 = view.findViewById(R.id.editTextPassChange2);
        btnChange = view.findViewById(R.id.boton_cambiar);
    }

    public void setCardViewDomiciliario(Domiciliario domiciliario){
        Utils.imagePicassoRounded(getContext(),domiciliario.getAvatar(),imgDomiciliario);
        nameDomiciliario.setText(domiciliario.getName());
        puntosDomiciliario.setText(String.valueOf(domiciliario.getCoins()));
        pedidosDomiciliario.setText(String.valueOf(domiciliario.getDeliveries().size()));
    }

    //######################################################################################
    //--------------------------------- Funciones Api Rest ---------------------------------
    //######################################################################################

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliarioUpdated));
        cargando.setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(), "Nueva contraseña establecida con exito!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), errorMessage , Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(getActivity(), "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
            cargando.setVisibility(View.INVISIBLE);
        }
    }
}
