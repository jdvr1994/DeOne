package com.apps.ing3ns.entregas.API.APIControllers.Delivery;

import android.util.Log;

import com.apps.ing3ns.entregas.API.API;
import com.apps.ing3ns.entregas.API.APIServices.DeliveryService;
import com.apps.ing3ns.entregas.Modelos.Delivery;

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

public class DeliveryController {
    DeliveryListener listener;

    public static final String  GET= "getdelivery";
    public static final String  GETALL= "getalldeliveries";
    public static final String  UPDATE= "updatedelivery";
    public static final String  GETCONDITION= "getcondition";

    public DeliveryController(DeliveryListener listener) {
        this.listener = listener;
    }

    //------------------------ HTTP GET REQUEST RETROFIT --------------------
    public void getDeliveries(){
        DeliveryService service = API.getApiGet().create(DeliveryService.class);
        Call<List<Delivery>> deliveryCall = service.getDeliveries();

        deliveryCall.enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(GETALL,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                List<Delivery> deliveries = response.body();
                if(deliveries != null) {
                    listener.getDeliveries(deliveries);
                }
            }

            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                listener.getErrorConnection(GETALL,t);
            }
        });
    }

    //------------------------ HTTP GET REQUEST RETROFIT --------------------
    public void getDeliveriesCondition(HashMap<String,String> map){
        DeliveryService service = API.getApiGet().create(DeliveryService.class);
        Call<List<Delivery>> deliveryCall = service.getDeliveriesCondition(map);

        deliveryCall.enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(GETCONDITION,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                List<Delivery> deliveries = response.body();
                if(deliveries != null) {
                    listener.getDeliveriesConditionSuccessful(deliveries);
                }
            }

            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                listener.getErrorConnection(GETCONDITION,t);
            }
        });
    }

    //------------------------ HTTP GET REQUEST RETROFIT --------------------
    public void getDelivery(String id){
        DeliveryService service = API.getApiGet().create(DeliveryService.class);
        Call<Delivery> deliveryCall = service.getDelivery(id);

        deliveryCall.enqueue(new Callback<Delivery>() {
            @Override
            public void onResponse(Call<Delivery> call, Response<Delivery> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(GET,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                Delivery delivery = response.body();
                if(delivery != null) {
                    listener.getDelivery(delivery);
                }
            }

            @Override
            public void onFailure(Call<Delivery> call, Throwable t) {
                listener.getErrorConnection(GET,t);
            }
        });
    }

    //--------------------- HTTP PUT REQUEST RETROFIT -----------------
    public void updateDelivery(String id, HashMap<String,String> map){
        DeliveryService service = API.getApiPost().create(DeliveryService.class);

        Call<Delivery> deliveryCall = service.updateDelivery(id,map);

        deliveryCall.enqueue(new Callback<Delivery>() {
            @Override
            public void onResponse(Call<Delivery> call, Response<Delivery> response){
                if(response.code()!=200) try {
                    listener.getErrorMessage(UPDATE,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                Delivery delivery = response.body();
                if(delivery != null) {
                    listener.updateDeliverySuccessful(delivery);
                }
            }

            @Override
            public void onFailure(Call<Delivery> call, Throwable t) {
                listener.getErrorConnection(UPDATE,t);
            }
        });
    }
}
