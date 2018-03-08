package com.apps.ing3ns.entregas.Modelos;

/**
 * Created by JuanDa on 10/02/2018.
 */

public class Position {
    double lat;
    double lng;

    public Position() {
    }

    public Position(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
