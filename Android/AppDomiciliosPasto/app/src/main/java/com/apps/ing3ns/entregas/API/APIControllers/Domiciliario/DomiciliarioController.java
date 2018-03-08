package com.apps.ing3ns.entregas.API.APIControllers.Domiciliario;

import android.util.Log;

import com.apps.ing3ns.entregas.API.API;
import com.apps.ing3ns.entregas.API.APIControllers.Domiciliario.DomiciliarioListener;
import com.apps.ing3ns.entregas.API.APIServices.DomiciliarioService;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.Modelos.DomiciliarioSignIn;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by JuanDa on 10/02/2018.
 */

public class DomiciliarioController {
    public static final String  GET= "getdom";
    public static final String  SIGNIN= "signindom";
    public static final String  UPDATE= "updatedom";

    DomiciliarioListener listener;

    public DomiciliarioController(DomiciliarioListener listener) {
        this.listener = listener;
    }

    //------------------------ HTTP GET REQUEST RETROFIT --------------------
    public void getDomiciliario(String token, String id){
        DomiciliarioService service = API.getApiGet().create(DomiciliarioService.class);
        final Call<Domiciliario> domiciliarioCall = service.getDomiciliario(token,id);

        domiciliarioCall.enqueue(new Callback<Domiciliario>() {
            @Override
            public void onResponse(Call<Domiciliario> call, Response<Domiciliario> response) {

                if(response.code()!=200) try {
                    listener.getErrorMessage(GET,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                Domiciliario domiciliario = response.body();
                if(domiciliario != null) listener.getDomiciliario(domiciliario);
            }

            @Override
            public void onFailure(Call<Domiciliario> call, Throwable t) {
                listener.getErrorConnection(GET,t);
            }
        });
    }

    //--------------------- HTTP POST REQUEST RETROFIT -----------------
    public void signInDomiciliario(HashMap<String,String> map){
        DomiciliarioService service = API.getApiPost().create(DomiciliarioService.class);
        final Call<DomiciliarioSignIn> domiciliarioCall = service.signInDomiciliario(map);

        domiciliarioCall.enqueue(new Callback<DomiciliarioSignIn>() {
            @Override
            public void onResponse(Call<DomiciliarioSignIn> call, Response<DomiciliarioSignIn> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(SIGNIN,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                DomiciliarioSignIn domiciliarioSignIn = response.body();
                if(domiciliarioSignIn != null) listener.signInDomiciliarioSuccessful(domiciliarioSignIn.getDomiciliario(),domiciliarioSignIn.getToken());
            }

            @Override
            public void onFailure(Call<DomiciliarioSignIn> call, Throwable t) {
                listener.getErrorConnection(SIGNIN,t);
            }
        });
    }

    //--------------------- HTTP PUT REQUEST RETROFIT -----------------
    public void updateDomiciliario(String id, HashMap<String,String> map){
        DomiciliarioService service = API.getApiPost().create(DomiciliarioService.class);

        Call<Domiciliario> domiciliarioCall = service.updateDomiciliario(id,map);

        domiciliarioCall.enqueue(new Callback<Domiciliario>() {
            @Override
            public void onResponse(Call<Domiciliario> call, Response<Domiciliario> response){
                if(response.code()!=200) try {
                    listener.getErrorMessage(UPDATE,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                Domiciliario domiciliario = response.body();
                if(domiciliario != null) listener.updateDomiciliarioSuccessful(domiciliario);
            }

            @Override
            public void onFailure(Call<Domiciliario> call, Throwable t) {
                listener.getErrorConnection(UPDATE,t);
            }
        });
    }
}
