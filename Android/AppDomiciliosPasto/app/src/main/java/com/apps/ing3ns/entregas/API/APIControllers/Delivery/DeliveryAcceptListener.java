package com.apps.ing3ns.entregas.API.APIControllers.Delivery;

import com.apps.ing3ns.entregas.Modelos.Delivery;

import java.util.List;

/**
 * Created by JuanDa on 16/03/2018.
 */

public interface DeliveryAcceptListener {
    void getDelivery(Delivery delivery);
    void updateDeliverySuccessful(Delivery deliveryUpdated);
    void startDeliverySuccessful(String message);
    void finishDeliverySuccessful(String message);
    void getErrorMessage(String nameEvent,int code, String errorMessage);
    void getErrorConnection(String nameEvent, Throwable t);
}
