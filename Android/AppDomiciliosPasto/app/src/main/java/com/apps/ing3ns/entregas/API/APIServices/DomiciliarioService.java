package com.apps.ing3ns.entregas.API.APIServices;

import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.Modelos.DomiciliarioSignIn;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
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

public interface DomiciliarioService {

    @GET("domiciliario/{domiciliarioId}")
    Call<Domiciliario> getDomiciliario(@Header("Authorization") String token, @Path("domiciliarioId") String domiciliarioId);

    @PUT("domiciliario/{domiciliarioId}")
    @FormUrlEncoded
    Call<Domiciliario> updateDomiciliario(@Path("domiciliarioId") String domiciliarioId, @FieldMap HashMap<String, String> fields);

    @POST("domiciliario/signin")
    @FormUrlEncoded
    Call<DomiciliarioSignIn> signInDomiciliario(@FieldMap HashMap<String, String> fields);

}
