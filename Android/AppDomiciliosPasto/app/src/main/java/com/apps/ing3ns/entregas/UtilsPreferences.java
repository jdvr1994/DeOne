package com.apps.ing3ns.entregas;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by JuanDa on 06/03/2018.
 */

public class UtilsPreferences {
    public static final String KEY_STATE_LOCATION_UPDATES = "requesting_locaction_updates";
    public static final String KEY_FIRSTTIME = "firstTime";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_DOMICILIARIO = "domiciliario";
    public static final String KEY_DELIVERY = "delivery";
    public static final String KEY_DELIVERIES = "deliveries";
    public static final String KEY_NEARBY_DELIVERIES = "deliveries";
    public static final String KEY_CLIENT = "client";
    public static final String KEY_LOCATION = "location";
    private static final String KEY_DELIVERIES_UPDATED = "deliveries_updated";
    private static final String KEY_DELIVERY_ID = "last_delivery_id";
    private static final String KEY_DELIVERIES_NUMBER = "last_deliveries_number";

    public static boolean isFirstTime(SharedPreferences preferences) {
        return preferences.getBoolean(KEY_FIRSTTIME, false);
    }

    public static void setFirstTime(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FIRSTTIME, true);
        editor.apply();
    }

    public static String getToken(SharedPreferences preferences) {
        return preferences.getString(KEY_TOKEN, null);
    }

    public static void saveToken(SharedPreferences preferences, String token) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public static void removeToken(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TOKEN);
        editor.apply();
    }

    public static String getDomiciliario(SharedPreferences preferences) {
        return preferences.getString(KEY_DOMICILIARIO, null);
    }

    public static void saveDomiciliario(SharedPreferences preferences, String domiciliario) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DOMICILIARIO, domiciliario);
        editor.apply();
    }

    public static void removeDomiciliario(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DOMICILIARIO);
        editor.apply();
    }

    public static String getDelivery(SharedPreferences preferences) {
        return preferences.getString(KEY_DELIVERY, null);
    }

    public static void saveDelivery(SharedPreferences preferences, String delivery) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DELIVERY, delivery);
        editor.apply();
    }

    public static void removeDelivery(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DELIVERY);
        editor.apply();
    }

    public static String getNearbyDeliveries(SharedPreferences preferences) {
        return preferences.getString(KEY_DELIVERIES, null);
    }

    public static void saveNearbyDeliveries(SharedPreferences preferences, String deliveries) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DELIVERIES, deliveries);
        editor.apply();
    }

    public static void removeNearbyDeliveries(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_DELIVERIES);
        editor.apply();
    }

    public static String getLastLocation(SharedPreferences preferences) {
        return preferences.getString(KEY_LOCATION, null);
    }

    public static void saveLastLocation(SharedPreferences preferences, String location) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_LOCATION, location);
        editor.apply();
    }

    public static String getClient(SharedPreferences preferences) {
        return preferences.getString(KEY_CLIENT, null);
    }

    public static void saveClient(SharedPreferences preferences, String client) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CLIENT, client);
        editor.apply();
    }

    public static void removeClient(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_CLIENT);
        editor.apply();
    }

    /**
     * Retorna la ID del ultimo delivery agragado a la lista de deliveries (State=0).
     * @param context el {@link Context} para recuperar las SharedPreferences.
     */
    public static String getLastDeliveryId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_DELIVERY_ID, null);
    }

    /**
     * Almacena la ID del ultimo delivery agregado a la lista de deliveries (State=0) en SharedPreferences.
     * @param deliveryId la ID del delivery.
     */
    public static void setLastDeliveryId(Context context, String deliveryId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_DELIVERY_ID, deliveryId)
                .apply();
    }

    /**
     * Retorna el numero de Deliveries (State=0).
     * @param context el {@link Context} para recuperar las SharedPreferences.
     */
    public static int getNumberLastDeliveries(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_DELIVERIES_NUMBER, 0);
    }

    /**
     * Almacena el numero de Deliveries (State=0) en SharedPreferences.
     * @param numDeliveries el numero de deliveries en estado 0.
     */
    public static void setNumberLastDeliveries(Context context, int numDeliveries) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_DELIVERIES_NUMBER, numDeliveries)
                .apply();
    }

    /**
     * Retorna true si los deliveries (State=0) estan actualizados desde el servidor, en otro caso false.
     * @param context el {@link Context} para recuperar las SharedPreferences.
     */
    public static boolean isDeliveriesUpdated(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_DELIVERIES_UPDATED, false);
    }

    /**
     * Almacena el estado de actualizacion de los deliveries (State=0) en SharedPreferences.
     *  true = ya fueron actualizados desde el servidor
     *  false =  aun no han sido actualizados
     * @param state el numero de deliveries en estado 0.
     */
    public static void setDeliveriesUpdated(Context context, boolean state) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_DELIVERIES_UPDATED, state)
                .apply();
    }

    /**
     * Retorna true si esta activado el servicio de actualizacion de localizacion, de otra forma retorna false.
     * @param context el {@link Context} para recuperar las SharedPreferences.
     */
    public static boolean getStateLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_STATE_LOCATION_UPDATES, false);
    }

    /**
     * Almacena el estado del servicio de actualizacion de localizacion en SharedPreferences.
     * @param requestingLocationUpdates el estado del servicio de actualizacion de localizacion.
     */
    public static void setStateLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_STATE_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }
}
