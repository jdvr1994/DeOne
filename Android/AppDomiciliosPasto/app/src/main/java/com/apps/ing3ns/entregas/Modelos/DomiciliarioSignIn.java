package com.apps.ing3ns.entregas.Modelos;

/**
 * Created by JuanDa on 05/03/2018.
 */

public class DomiciliarioSignIn {
    private String token;
    private Domiciliario domiciliario;

    public DomiciliarioSignIn() {
    }

    public DomiciliarioSignIn(String token, Domiciliario domiciliario) {
        this.token = token;
        this.domiciliario = domiciliario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Domiciliario getDomiciliario() {
        return domiciliario;
    }

    public void setDomiciliario(Domiciliario domiciliario) {
        this.domiciliario = domiciliario;
    }
}
