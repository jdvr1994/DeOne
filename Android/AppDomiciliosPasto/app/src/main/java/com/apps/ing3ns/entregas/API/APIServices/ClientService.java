package com.apps.ing3ns.entregas.API.APIServices;

import com.apps.ing3ns.entregas.Modelos.Client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by JuanDa on 12/07/2017.
 */

public interface ClientService {

    @GET("client/{clientId}")
    Call<Client> getClient(@Header("Authorization") String token,@Path("clientId") String clientId);

}
