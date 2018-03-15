package com.apps.ing3ns.entregas.API.APIControllers.Domiciliario;

import com.apps.ing3ns.entregas.Modelos.Domiciliario;

import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public interface DomiciliarioListener {
    void getDomiciliario(Domiciliario domiciliario);
    void updateDomiciliarioSuccessful(Domiciliario domiciliarioUpdated);
    void signInDomiciliarioSuccessful(Domiciliario domiciliario, String token);
    void getErrorMessage(String nameEvent,int code, String errorMessage);
    void getErrorConnection(String nameEvent, Throwable t);
}
