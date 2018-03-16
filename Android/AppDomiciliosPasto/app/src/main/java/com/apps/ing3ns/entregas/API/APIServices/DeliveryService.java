package com.apps.ing3ns.entregas.API.APIServices;

import com.apps.ing3ns.entregas.Modelos.Delivery;

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
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Created by JuanDa on 12/07/2017.
 */

public interface DeliveryService {

    @GET("deliveries")
    Call<List<Delivery>> getDeliveries();

    @POST("delivery/search/state")
    @FormUrlEncoded
    Call<List<Delivery>> getDeliveriesCondition(@FieldMap HashMap<String, String> fields);

    @GET("delivery/{deliveryId}")
    Call<Delivery> getDelivery(@Path("deliveryId") String deliveryId);

    @PUT("delivery/{deliveryId}")
    @FormUrlEncoded
    Call<Delivery> updateDelivery(@Path("deliveryId") String deliveryId, @FieldMap HashMap<String, String> fields);

    @POST("delivery/start")
    @FormUrlEncoded
    Call<ResponseBody> startDelivery(@Field("domiciliario") String domiciliarioId, @Field("delivery") String deliveryId);

    @POST("delivery/finish")
    @FormUrlEncoded
    Call<ResponseBody> finishDelivery(@Field("domiciliario") String domiciliarioId, @Field("delivery") String deliveryId);
}
