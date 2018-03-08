package com.apps.ing3ns.entregas.modelsRoutes;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by JuanDa on 31/05/2017.
 */

public class RutaEscogida {
    private String nameRuta;
    private List<LatLng> routeOrigenBus;
    private List<LatLng> trazoBus;
    private List<LatLng> routeBusDetino;

    public void RutaEscogida(String nameRuta, List<LatLng> routeOrigenBus, List<LatLng> trazoBus, List<LatLng> routeBusDetino){
        this.nameRuta = nameRuta;
        this.routeOrigenBus = routeOrigenBus;
        this.trazoBus = trazoBus;
        this.routeBusDetino = routeBusDetino;
    }

    public void RutaEscogida(){
        this.nameRuta = null;
        this.routeOrigenBus = null;
        this.trazoBus = null;
        this.routeBusDetino = null;
    }

    public String getNameRuta() {
        return nameRuta;
    }

    public void setNameRuta(String nameRuta) {
        this.nameRuta = nameRuta;
    }

    public List<LatLng> getRouteOrigenBus() {
        return routeOrigenBus;
    }

    public void setRouteOrigenBus(List<LatLng> routeOrigenBus) {
        this.routeOrigenBus = routeOrigenBus;
    }

    public List<LatLng> getTrazoBus() {
        return trazoBus;
    }

    public void setTrazoBus(List<LatLng> trazoBus) {
        this.trazoBus = trazoBus;
    }

    public List<LatLng> getRouteBusDetino() {
        return routeBusDetino;
    }

    public void setRouteBusDetino(List<LatLng> routeBusDetino) {
        this.routeBusDetino = routeBusDetino;
    }

    public void clear(){
        this.nameRuta = null;
        this.routeOrigenBus = null;
        this.trazoBus = null;
        this.routeBusDetino = null;
    }
}
