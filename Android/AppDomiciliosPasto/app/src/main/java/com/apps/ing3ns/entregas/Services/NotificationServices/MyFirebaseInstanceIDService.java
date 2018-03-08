package com.apps.ing3ns.entregas.Services.NotificationServices;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioController;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.Modelos.Position;
import com.apps.ing3ns.entregas.Utils;
import com.apps.ing3ns.entregas.UtilsPreferences;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

/**
 * Created by JuanDa on 04/03/2018.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService implements DomiciliarioListener {
    public SharedPreferences preferences;
    private static final String TAG = "tokenTag";
    DomiciliarioController domiciliarioController;
    Domiciliario domiciliario;
    Gson gson;

    @Override
    public void onTokenRefresh() {
        domiciliarioController = new DomiciliarioController(this);
        preferences = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        domiciliario = gson.fromJson(UtilsPreferences.getDomiciliario(preferences), Domiciliario.class);
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken) {
        HashMap<String,String> map = new HashMap<>();
        map.put("tokenNotification",refreshedToken);
        if(domiciliario!=null) domiciliarioController.updateDomiciliario(domiciliario.get_id(),map);
    }

    @Override
    public void getDomiciliario(Domiciliario domiciliario) {

    }

    @Override
    public void updateDomiciliarioSuccessful(Domiciliario domiciliario) {
        UtilsPreferences.saveDomiciliario(preferences,gson.toJson(domiciliario));
        Toast.makeText(this, "Token de notificationes actualizado correctamente", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token) {

    }

    @Override
    public void getErrorMessage(String nameEvent, int code, String errorMessage) {

    }

    @Override
    public void getErrorConnection(String nameEvent, Throwable t) {

    }
}
