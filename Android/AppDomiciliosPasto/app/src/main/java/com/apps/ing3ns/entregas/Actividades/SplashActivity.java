package com.apps.ing3ns.entregas.Actividades;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.R;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

import okhttp3.internal.Util;

public class SplashActivity extends AppCompatActivity implements DomiciliarioListener{

    Intent intent;
    private SharedPreferences prefs;
    private Gson gson;

    String token;
    String domiciliario;
    String delivery;
    Boolean permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent(this, MainActivity.class);

        DomiciliarioController domiciliarioController = new DomiciliarioController(this);
        gson = new GsonBuilder().create();

        //------------ Rescato de las preferences el token-----------
        prefs = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        token = UtilsPreferences.getToken(prefs);
        domiciliario = UtilsPreferences.getDomiciliario(prefs);
        delivery = UtilsPreferences.getDelivery(prefs);
        permission = UtilsPreferences.getFirstTime(prefs);

        if(permission) {
            if (token != null && domiciliario != null) {
                domiciliarioController.getDomiciliario("Bearer " + token, gson.fromJson(domiciliario, Domiciliario.class).get_id());
            }else {
                intent.putExtra("Fragment", Utils.KEY_LOGIN_FRAGMENT);
                changeActivity();
            }
        }else{
            intent = new Intent(this, IntroActivity.class);
            changeActivity();
        }
        //------------ En caso contrario borro de las preferences el token y compruebo logearme con los datos de usuario guardados en preferences (JWT) --------------
        //------------ Si se logea prosigo con fragment Domiciliario -----------------
        //------------- En caso contrario borro todas las preferences y llevo al usuario al fragment Login ------------------

    }

    public void changeActivity(){
        startActivity(intent);
        finish();
    }

    //#########################################################################################################
    //-------------------------------- EVENTS LISTENER DOMICILIARIO CONTROLLER ---------------------------------
    //#########################################################################################################

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {
        UtilsPreferences.saveDomiciliario(prefs,gson.toJson(domiciliario));
        if(delivery!=null) intent.putExtra("Fragment",Utils.KEY_MAP_FRAGMENT);
        else intent.putExtra("Fragment",Utils.KEY_DOMICILIARIO_FRAGMENT);
        changeActivity();
    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliario) {

    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {
        if(nameEvent.contentEquals(DomiciliarioController.GET)){
            if(code==401) {
                UtilsPreferences.removeToken(prefs);
                Toast.makeText(this, "La conexion caduco, por favor intentalo de nuevo", Toast.LENGTH_LONG).show();
            }else   Toast.makeText(this, "Hubo un error: "+code + "  "+errorMessage, Toast.LENGTH_LONG).show();

            intent.putExtra("Fragment",Utils.KEY_LOGIN_FRAGMENT);
            changeActivity();
        }
    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {
        if(nameEvent.contentEquals(DomiciliarioController.GET)){
            Toast.makeText(this, "Ha ocurrido un error de conexión", Toast.LENGTH_SHORT).show();
            intent.putExtra("Fragment",Utils.KEY_LOGIN_FRAGMENT);
            changeActivity();
        }
    }
}
