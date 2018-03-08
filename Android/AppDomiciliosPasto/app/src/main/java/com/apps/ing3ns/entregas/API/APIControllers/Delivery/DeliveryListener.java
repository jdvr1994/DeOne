package com.apps.ing3ns.entregas.API.APIControllers.Delivery;

import com.apps.ing3ns.entregas.Modelos.Delivery;

import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public interface DeliveryListener {
    void getDeliveries(List<Delivery> deliveries);
    void getDelivery(Delivery delivery);
    void updateDeliverySuccessful(Delivery delivery);
    void getDeliveriesConditionSuccessful(List<Delivery> deliveries);
    void getErrorMessage(String nameEvent,int code, String errorMessage);
    void getErrorConnection(String nameEvent, Throwable t);
}
