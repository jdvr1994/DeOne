package com.apps.ing3ns.entregas;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.apps.ing3ns.entregas.Modelos.Delivery;
import com.apps.ing3ns.entregas.Modelos.Domiciliario;
import com.apps.ing3ns.entregas.transforms.RoundedTransformation;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Response;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by JuanDa on 15/02/2018.
 */

public class Utils {
    public static final String KEY_LOGIN_FRAGMENT = "loginFragment";
    public static final String KEY_DOMICILIARIO_FRAGMENT = "domiciliarioFragment";
    public static final String KEY_MAP_FRAGMENT = "mapFragment";
    public static final String KEY_PROFILE_FRAGMENT = "profileFragment";

    public static final String TOPIC_STATE_0 = "STATE_0";
    public static final String TOPIC_STATE_1 = "STATE_1";
    public static final String TOPIC_STATE_2 = "STATE_2";

    public static final int DELIVERY_DISPONIBLE = 0;
    public static final int DELIVERY_RECOGIENDO = 1;
    public static final int DELIVERY_ENTREGANDO = 2;
    public static final int DELIVERY_FINALIZADO = 3;

    public static final int DOMICILIARIO_INACTIVO = 0;
    public static final int DOMICILIARIO_ACTIVO = 1;
    public static final int DOMICILIARIO_ENTREGANDO = 2;

    //-----------------------------------------------------------------------------------------
    //--------------------- FUNCIONES DISTANCIA CON CLIENTE -----------------------------------

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    public static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    public static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    //---------------------------------------FUNCIONES AUXILIARES ---------------------------------
    //---------------------------------------------------------------------------------------------
    public static HashMap<String,String> getHashMapState(int state){
        HashMap<String,String> map = new HashMap<>();
        map.put("state",String.valueOf(state));
        return map;
    }

    public static HashMap<String,String> getHashMapTokenAndState(int state){
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        HashMap<String,String> map = new HashMap<>();
        map.put("tokenNotification",refreshedToken);
        map.put("state",String.valueOf(state));
        return map;
    }

    public static HashMap<String,String> getHashMapAddDelivery(String listDeliveries, String newDelivery){
        Gson gson = new GsonBuilder().create();
        List<String> deliveries = gson.fromJson(listDeliveries,new TypeToken<List<String>>(){}.getType());
        deliveries.add(newDelivery);
        HashMap<String,String> map = new HashMap<>();
        map.put("deliveries",gson.toJson(deliveries));
        return map;
    }

    public static HashMap<String,String> deleteDeliveries(){
        Gson gson = new GsonBuilder().create();
        List<String> deliveries = new ArrayList<>();
        HashMap<String,String> map = new HashMap<>();
        map.put("deliveries",gson.toJson(deliveries));
        return map;
    }


    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    public static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }
    //------------------------------------------------------------------------------------
    //------------------------------IMAGE PICASSO-----------------------------------------
    //------------------------------------------------------------------------------------
    public static void imagePicasso(final Context context, String imageURL, final ImageView imageView){
        if(imageURL!=null){if(imageURL.length()==0) imageURL = "http://imagen";}
        else imageURL = "http://imagen";
        final String finalImageURL = imageURL;
        Picasso.with(context).load(imageURL).networkPolicy(NetworkPolicy.OFFLINE).fit().placeholder(R.mipmap.slide1).into(imageView, new com.squareup.picasso.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError() {
                Picasso.with(context)
                        .load(finalImageURL)
                        .fit()
                        .placeholder(R.mipmap.slide1)
                        .into(imageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {

                            }

                        });
            }
        });
    }

    public static void imagePicassoRounded(final Context context, String imageURL, final ImageView imageView){
        if(imageURL!=null){if(imageURL.length()==0) imageURL = "http://imagen";}
        else imageURL = "http://imagen";
        final String finalImageURL = imageURL;
        Picasso.with(context).load(imageURL).networkPolicy(NetworkPolicy.OFFLINE).fit().transform(new RoundedTransformation()).placeholder(R.mipmap.slide1).into(imageView, new com.squareup.picasso.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError() {
                Picasso.with(context)
                        .load(finalImageURL)
                        .fit()
                        .transform(new RoundedTransformation())
                        .placeholder(R.mipmap.slide1)
                        .into(imageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {

                            }

                        });
            }
        });
    }
}
