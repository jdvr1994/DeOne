package com.apps.ing3ns.entregas.Actividades;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Fragmentos.DomiciliarioFragment;
import com.apps.ing3ns.entregas.Fragmentos.LoginFragment;
import com.apps.ing3ns.entregas.Fragmentos.MapFragment;
import com.apps.ing3ns.entregas.Listeners.FragmentsListener;
import com.apps.ing3ns.entregas.Menu.MenuController;
import com.apps.ing3ns.entregas.Menu.MenuListener;
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

public class MainActivity extends AppCompatActivity implements MenuListener, DomiciliarioListener, FragmentsListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Variables usadas para activar el GPS del dispositivo por medio de @GoogleApiClient.
     */
    final static int REQUEST_LOCATION = 198;
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> result;
    private Status statusGlobal;

    /**
     * Variables usadas para activar el Administrar los Fragments
     */
    FragmentManager fragmentManager = getSupportFragmentManager();
    LoginFragment loginFragment = new LoginFragment();
    DomiciliarioFragment domiciliarioFragment = new DomiciliarioFragment();
    MapFragment mapFragment = new MapFragment();

    //######################################################################################
    //------------------------------ VARIABLES DE PROCESO ----------------------------------
    //######################################################################################
    /**
     * Controladores de Menu y Domiciliario
     */
    MenuController menuController;
    DomiciliarioController domiciliarioController;

    /**
     * Otras varibales de proceso.
     */
    SharedPreferences prefs;
    Gson gson;
    Domiciliario domiciliario;
    String fragmentActive;

    //#########################################################################################################
    //----------------------------------------- CICLO DE VIDA ACTIVITY ----------------------------------------
    //#########################################################################################################
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        prefs = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        menuController = new MenuController(this);
        fragmentActive = getIntent().getExtras().getString("Fragment");
        domiciliarioController = new DomiciliarioController(this);
        gson = new GsonBuilder().create();
        //--------------------------- TOOLBAR AND MENU ------------------------
        menuController.setToolbar();
        menuController.setNavigationDrawer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient!=null) {
            if(!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();
        }

        buildGoogleApiClient();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    //----------------------------PRESIONAR BOTON ATRAS --------------------------------
    @Override
    public void onBackPressed() {
        if (!menuController.closeNavigationDrawer()) super.onBackPressed();
    }


    //#########################################################################################################
    //-------------------------------------- NAVIGATION VIEW AND TOOLBAR --------------------------------------
    //#########################################################################################################
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuController.inflaterMenuToolbar(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(menuController.selectItemToolbar(item)) return true;
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public void changeOptionMenu(int itemId) {
        switch (itemId){
            case R.id.menu_toolbar_usuario:
                Toast.makeText(this, "PRESIONO ADD", Toast.LENGTH_SHORT).show();
                setViewMapFragment();
                break;

            case R.id.menu_home:
                if(fragmentActive.contains(Utils.KEY_LOGIN_FRAGMENT)) setViewLoginFragment();
                else if(fragmentActive.contains(Utils.KEY_MAP_FRAGMENT)) setViewMapFragment();
                else setViewDomiciliarioFragment();
                break;

            case R.id.menu_cerrar_sesion:
                domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(prefs),Domiciliario.class);
                domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapState(Utils.DOMICILIARIO_INACTIVO));
                UtilsPreferences.removeToken(prefs);
                UtilsPreferences.removeDomiciliario(prefs);
                UtilsPreferences.removeDelivery(prefs);
                UtilsPreferences.removeNearbyDeliveries(prefs);

                setViewLoginFragment();

                // Terminamos el servicio para dejar de usar
                Intent intent = new Intent(this, ForegroundLocationService.class);
                intent.putExtra(ForegroundLocationService.EXTRA_STARTED_FROM_NOTIFICATION, true);
                startService(intent);
                break;
        }
    }

    @Override
    public void menuCreated() {
        if(fragmentActive.contains(Utils.KEY_LOGIN_FRAGMENT)) setViewLoginFragment();
        else if(fragmentActive.contains(Utils.KEY_MAP_FRAGMENT)) setViewMapFragment();
        else setViewDomiciliarioFragment();
    }

    //######################################################################################
    //--------------------------- GOOGLE API CLIENT LOCATION ------------------------------
    //######################################################################################
    private void buildGoogleApiClient() {
        Log.i(TAG, "Inicio buildGoogleApliClient");
        if (mGoogleApiClient != null) {
            return;
        }

        Log.i(TAG, "Creando nuevo API CLIENT");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Log.i(TAG, "Conectando el google Api client");
        mGoogleApiClient.connect();
    }

    /**
     * Callback recibido cuando el resultado de una conexion es completada (@GoogleApiClient).
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"onConnected Google Api Client");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(new LocationRequest().setPriority(LocationRequest.PRIORITY_LOW_POWER));
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(this);
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
                            statusGlobal.startResolutionForResult(this, REQUEST_LOCATION);
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
        View container = findViewById(R.id.main_content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    //#########################################################################################################
    //--------------------------------------- CAMBIAR VISTA DE FRAGMENTS --------------------------------------
    //#########################################################################################################

    public void setViewLoginFragment(){
        if(!loginFragment.isVisible()) {
            Bundle args = new Bundle();
            args.putBoolean(LoginFragment.DATA, false);
            loginFragment.setArguments(args);
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, loginFragment,Utils.KEY_LOGIN_FRAGMENT)
                    .commit();

            menuController.toolbarWithLogo();
            menuController.setMenuLateralLogOut();
        }
    }

    public void setViewDomiciliarioFragment(){
        if(!domiciliarioFragment.isVisible()) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, domiciliarioFragment,Utils.KEY_DOMICILIARIO_FRAGMENT)
                    .commit();

            menuController.toolbarWithLogo();
            menuController.setMenuLateralLogIn();
        }
    }

    public void setViewMapFragment(){
        if(!mapFragment.isVisible()) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, mapFragment,Utils.KEY_MAP_FRAGMENT)
                    .commit();

            menuController.toolbarWithLogo();
            menuController.setMenuLateralLogOut();
        }
    }


    //#########################################################################################################
    //------------------------------ ANIMACIONES DE TRANSICION PARA FRAGMENTS ---------------------------------
    //#########################################################################################################

    private static final long MOVE_DEFAULT_TIME = 150;
    private static final long FADE_DEFAULT_TIME = 350;

    public void performTransition(Fragment actualFragment,String tagNewFragment,Fragment newFragment, int id) {
        fragmentActive = tagNewFragment;

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        View itemView = findViewById(id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Fade exitAnimation = new Fade();
            exitAnimation.setDuration(2*FADE_DEFAULT_TIME + MOVE_DEFAULT_TIME);
            actualFragment.setExitTransition(exitAnimation);


            TransitionSet enterTransitionSet = new TransitionSet();
            enterTransitionSet.addTransition(new Fade());
            enterTransitionSet.setDuration(MOVE_DEFAULT_TIME);
            enterTransitionSet.setStartDelay(FADE_DEFAULT_TIME);
            newFragment.setSharedElementEnterTransition(enterTransitionSet);


            Explode enterAnimation = new Explode();
            enterAnimation.setStartDelay(MOVE_DEFAULT_TIME);
            enterAnimation.setDuration(FADE_DEFAULT_TIME);
            newFragment.setEnterTransition(enterAnimation);

            fragmentTransaction.addSharedElement(itemView, itemView.getTransitionName());
            fragmentTransaction.replace(R.id.main_content, newFragment,tagNewFragment);
            fragmentTransaction.commitAllowingStateLoss();
        }else{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_content, newFragment,tagNewFragment).commit();
        }
    }

    //#########################################################################################################
    //------------------------------ EVENTOS LISTENER DOMICILIARIO CONTROLLER ---------------------------------
    //#########################################################################################################
    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated) {
        UtilsPreferences.saveDomiciliario(prefs,gson.toJson(domiciliarioUpdated));
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(this, errorMessage , Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DomiciliarioController.UPDATE)){
            Toast.makeText(this, "Ha ocurrido un error de conexión", Toast.LENGTH_LONG).show();
        }
    }

    //#########################################################################################################
    //----------------------------------- SETS VIEW CHANGE FRAGMENTS ------------------------------------------
    //#########################################################################################################

    @Override
    public void setOnChangeToLogin(String tagPrevFragment, int idObjectUI) {
        if(domiciliarioFragment.isVisible()) performTransition(domiciliarioFragment,Utils.KEY_LOGIN_FRAGMENT,loginFragment,idObjectUI);
        menuController.setMenuLateralLogOut();
    }

    @Override
    public void setOnChangeToDomiciliario(String tagPrevFragment, int idObjectUI) {
        if(loginFragment.isVisible()) performTransition(loginFragment,Utils.KEY_DOMICILIARIO_FRAGMENT,domiciliarioFragment,idObjectUI);
        if(mapFragment.isVisible()) performTransition(mapFragment,Utils.KEY_DOMICILIARIO_FRAGMENT,domiciliarioFragment,idObjectUI);
        menuController.setMenuLateralLogIn();
    }

    @Override
    public void setOnChangeToMap(String tagPrevFragment, int idObjectUI) {
        if(domiciliarioFragment.isVisible()) performTransition(domiciliarioFragment,Utils.KEY_MAP_FRAGMENT,mapFragment,idObjectUI);
        if(loginFragment.isVisible()) performTransition(loginFragment,Utils.KEY_MAP_FRAGMENT,mapFragment,idObjectUI);
        menuController.setMenuLateralLogOut();
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult result) {
        final Status status = result.getStatus();
        statusGlobal = status;
        if (status.getStatusCode()== LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
            try {
                status.startResolutionForResult(this, REQUEST_LOCATION);
            } catch (IntentSender.SendIntentException e) {}
        }else{
            Log.i(TAG,"Google API Client Conexion Exitosa");
        }
    }
}


