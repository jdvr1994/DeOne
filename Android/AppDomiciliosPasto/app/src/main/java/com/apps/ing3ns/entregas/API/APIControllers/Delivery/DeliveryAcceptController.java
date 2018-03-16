package com.apps.ing3ns.entregas.API.APIControllers.Delivery;

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
 * Created by JuanDa on 16/03/2018.
 */

public class DeliveryAcceptController {
    DeliveryAcceptListener listener;

    public static final String  GET= "getdelivery";
    public static final String  START= "start_delivery";
    public static final String  FINISH= "finish_delivery";

    public DeliveryAcceptController(DeliveryAcceptListener listener) {
        this.listener = listener;
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

    //------------------------ HTTP POST REQUEST RETROFIT --------------------
    public void startDelivery(String domiciliarioId, String deliveryId){
        DeliveryService service = API.getApiPost().create(DeliveryService.class);
        Call<ResponseBody> deliveryCall = service.startDelivery(domiciliarioId,deliveryId);

        deliveryCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(START,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                ResponseBody message = response.body();
                if(message != null) {
                    try {
                        listener.startDeliverySuccessful(message.string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                listener.getErrorConnection(START,t);
            }
        });
    }

    //------------------------ HTTP POST REQUEST RETROFIT --------------------
    public void finishDelivery(String domiciliarioId, String deliveryId){
        DeliveryService service = API.getApiPost().create(DeliveryService.class);
        Call<ResponseBody> deliveryCall = service.finishDelivery(domiciliarioId,deliveryId);

        deliveryCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.code()!=200) try {
                    listener.getErrorMessage(FINISH,response.code(),response.errorBody().string());
                } catch (IOException e) {e.printStackTrace();}

                ResponseBody message = response.body();
                if(message != null) {
                    try {
                        listener.finishDeliverySuccessful(message.string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                listener.getErrorConnection(FINISH,t);
            }
        });
    }
}
