package com.apps.ing3ns.entregas.Modelos;

import android.location.Location;

import com.apps.ing3ns.entregas.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public class Delivery {
    private String _id;
    private String client;
    private String domiciliario;
    private String addressStart;
    private String addressEnd;
    private Position positionStart;
    private Position positionEnd;
    private Date date;
    private String category;
    private String phone;
    private int state;

    public Delivery() {
    }

    public Delivery(String _id, String client, String domiciliario, String addressStart, String addressEnd, Position positionStart, Position positionEnd, Date date, String category, String phone, int state) {
        this._id = _id;
        this.client = client;
        this.domiciliario = domiciliario;
        this.addressStart = addressStart;
        this.addressEnd = addressEnd;
        this.positionStart = positionStart;
        this.positionEnd = positionEnd;
        this.date = date;
        this.category = category;
        this.phone = phone;
        this.state = state;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getDomiciliario() {
        return domiciliario;
    }

    public void setDomiciliario(String domiciliario) {
        this.domiciliario = domiciliario;
    }

    public String getAddressStart() {
        return addressStart;
    }

    public void setAddressStart(String addressStart) {
        this.addressStart = addressStart;
    }

    public String getAddressEnd() {
        return addressEnd;
    }

    public void setAddressEnd(String addressEnd) {
        this.addressEnd = addressEnd;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Position getPositionStart() {
        return positionStart;
    }

    public void setPositionStart(Position positionStart) {
        this.positionStart = positionStart;
    }

    public Position getPositionEnd() {
        return positionEnd;
    }

    public void setPositionEnd(Position positionEnd) {
        this.positionEnd = positionEnd;
    }

    public static List<Delivery> getNearbyDeliveries(List<Delivery> deliveries, Location location, double rangeKm){
        List<Delivery> nearbyDeliveries = new ArrayList<>();
        for (Delivery delivery:deliveries) {
            if (Utils.distance(delivery.getPositionStart().getLat(), delivery.getPositionStart().getLng(), location.getLatitude(), location.getLongitude()) < rangeKm) {
                nearbyDeliveries.add(delivery);
            }else if(delivery.getPositionStart().getLng()==0)nearbyDeliveries.add(delivery);
        }
        return nearbyDeliveries;
    }

    public static List<Delivery> removeDelivery(List<Delivery> deliveries, String _id){
        List<Delivery> newDeliveries = deliveries;
        Iterator<Delivery> iter = newDeliveries.iterator();

        while (iter.hasNext()) {
            Delivery delivery = iter.next();
            if(delivery.get_id().equals(_id)) iter.remove();
        }

        return newDeliveries;
    }

    public static boolean compareListDeliveries(List<Delivery> deliveries1, List<Delivery> deliveries2){
        boolean equal = true;
        if(deliveries1!=null & deliveries2!=null) {
            if (deliveries1.size() != deliveries2.size()) equal = false;
            else {
                int index = 0;
                for (Delivery delivery : deliveries1) {
                    if (!delivery.get_id().equals(deliveries2.get(index).get_id())) equal = false;
                    index++;
                }
            }
        }else equal = false;

        return equal;
    }
}
