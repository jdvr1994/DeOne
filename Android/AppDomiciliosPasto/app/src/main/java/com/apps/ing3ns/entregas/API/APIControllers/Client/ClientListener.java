package com.apps.ing3ns.entregas.API.APIControllers.Client;

import com.apps.ing3ns.entregas.Modelos.Client;

import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public interface ClientListener {
    void getClientSuccessful(Client client);
    void getErrorMessage(String nameEvent,int code, String errorMessage);
    void getErrorConnection(String nameEvent, Throwable t);
}
