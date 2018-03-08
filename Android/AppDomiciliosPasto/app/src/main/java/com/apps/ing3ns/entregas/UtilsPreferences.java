package com.apps.ing3ns.entregas;

import android.content.SharedPreferences;

/**
 * Created by JuanDa on 06/03/2018.
 */

public class UtilsPreferences {
    public static final String  KEY_FIRSTTIME= "firstTime";
    public static final String  KEY_TOKEN= "token";
    public static final String  KEY_DOMICILIARIO= "domiciliario";
    public static final String  KEY_DELIVERY= "delivery";
    public static final String  KEY_DELIVERIES= "deliveries";
    public static final String  KEY_NEARBY_DELIVERIES= "deliveries";
    public static final String  KEY_CLIENT= "client";
    public static final String KEY_LOCATION = "location";

    public static boolean getFirstTime(SharedPreferences preferences) {
        return preferences.getBoolean(KEY_FIRSTTIME,false);
    }

    public static void saveFirstTime(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FIRSTTIME,true);
        editor.apply();
    }

    public static String getToken(SharedPreferences preferences) {
        return preferences.getString(KEY_TOKEN,null);
    }

    public static void saveToken(SharedPreferences preferences, String token) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN,token);
        editor.apply();
    }

    public static void removeToken(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TOKEN);
        editor.apply();
    }

    public static String getDomiciliario(SharedPreferences preferences) {
        return preferences.getString(KEY_DOMICILIARIO,null);
    }

    public static void saveDomiciliario(SharedPreferences preferences, String domiciliario) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DOMICILIARIO,domiciliario);
        editor.commit();
    }

    public static void removeDomiciliario(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DOMICILIARIO);
        editor.apply();
    }

    public static String getDelivery(SharedPreferences preferences) {
        return preferences.getString(KEY_DELIVERY,null);
    }

    public static void saveDelivery(SharedPreferences preferences, String delivery) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DELIVERY,delivery);
        editor.apply();
    }

    public static void removeDelivery(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DELIVERY);
        editor.apply();
    }

    public static String getDeliveries(SharedPreferences preferences) {
        return preferences.getString(KEY_DELIVERIES,null);
    }

    public static void saveDeliveries(SharedPreferences preferences, String deliveries) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DELIVERIES,deliveries);
        editor.apply();
    }

    public static void removeDeliveries(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DELIVERIES);
        editor.apply();
    }

    public static String getNearbyDeliveries(SharedPreferences preferences) {
        return preferences.getString(KEY_DELIVERIES,null);
    }

    public static void saveNearbyDeliveries(SharedPreferences preferences, String deliveries) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DELIVERIES,deliveries);
        editor.commit();
    }

    public static void removeNearbyDeliveries(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DELIVERIES);
        editor.apply();
    }

    public static String getLastLocation(SharedPreferences preferences) {
        return preferences.getString(KEY_LOCATION,null);
    }

    public static void saveLastLocation(SharedPreferences preferences, String location) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LOCATION,location);
        editor.commit();
    }

    public static void removeLastLocation(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_LOCATION);
        editor.apply();
    }

    public static String getClient(SharedPreferences preferences) {
        return preferences.getString(KEY_CLIENT,null);
    }

    public static void saveClient(SharedPreferences preferences, String client) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CLIENT,client);
        editor.commit();
    }

    public static void removeClient(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_CLIENT);
        editor.apply();
    }
}
