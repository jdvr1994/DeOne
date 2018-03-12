package com.apps.ing3ns.entregas.Actividades;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.TransitionSet;
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
import com.apps.ing3ns.entregas.Services.GpsServices.Constants;
import com.apps.ing3ns.entregas.Services.GpsServices.ForegroundService;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity implements MenuListener, DomiciliarioListener, FragmentsListener {

    FragmentManager fragmentManager = getSupportFragmentManager();
    LoginFragment loginFragment = new LoginFragment();
    DomiciliarioFragment domiciliarioFragment = new DomiciliarioFragment();
    MapFragment mapFragment = new MapFragment();

    SharedPreferences prefs;
    Gson gson;
    Domiciliario domiciliario;
    String fragmentActive;
    //------Menus----------
    MenuController menuController;
    DomiciliarioController domiciliarioController;

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
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                domiciliarioController.updateDomiciliario(domiciliario.get_id(),Utils.getHashMapState(0));
                UtilsPreferences.removeToken(prefs);
                UtilsPreferences.removeDomiciliario(prefs);
                UtilsPreferences.removeDelivery(prefs);
                UtilsPreferences.removeNearbyDeliveries(prefs);

                setViewLoginFragment();
                gpsServiceAction(Constants.ACTION.STOP_FOREGROUND);
                break;
        }
    }

    @Override
    public void menuCreated() {
        if(fragmentActive.contains(Utils.KEY_LOGIN_FRAGMENT)) setViewLoginFragment();
        else if(fragmentActive.contains(Utils.KEY_MAP_FRAGMENT)) setViewMapFragment();
        else setViewDomiciliarioFragment();
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
    public void updateDomiciliarioSuccessful(Domiciliario domiciliario) {
        UtilsPreferences.saveDomiciliario(prefs,gson.toJson(domiciliario));
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
        menuController.setMenuLateralLogOut();
    }

    //-------------------------- START AND STOP SERVICE FOREGROUND -------------------------
    public void gpsServiceAction(String action){
        Intent startIntent = new Intent(this, ForegroundService.class);
        startIntent.setAction(action);
        startService(startIntent);
    }
}


