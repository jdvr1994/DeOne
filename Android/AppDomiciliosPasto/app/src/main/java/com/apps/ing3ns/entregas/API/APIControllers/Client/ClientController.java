package com.apps.ing3ns.entregas.API.APIControllers.Client;

import android.util.Log;

import com.apps.ing3ns.entregas.API.API;
import com.apps.ing3ns.entregas.API.APIServices.ClientService;
import com.apps.ing3ns.entregas.Modelos.Client;

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

public class ClientController {
    ClientListener listener;
    public static final String  GET= "getclient";

    public ClientController(ClientListener listener) {
        this.listener = listener;
    }

    //------------------------ HTTP GET REQUEST RETROFIT --------------------
    public void getClient(String token, String id){
        ClientService service = API.getApiGet().create(ClientService.class);
        Call<Client> clientCall = service.getClient(token,id);

        clientCall.enqueue(new Callback<Client>() {
            @Override
            public void onResponse(Call<Client> call, Response<Client> response) {

                if(response.code()!=200) try {
                    listener.getErrorMessage(GET,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                Client client = response.body();
                if(client != null) listener.getClientSuccessful(client);
            }

            @Override
            public void onFailure(Call<Client> call, Throwable t) {
                listener.getErrorConnection(GET,t);
            }
        });
    }
}
