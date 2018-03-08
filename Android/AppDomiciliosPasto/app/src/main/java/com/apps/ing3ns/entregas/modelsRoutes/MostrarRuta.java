package com.apps.ing3ns.entregas.modelsRoutes;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuanDa on 19/05/2017.
 */

public class MostrarRuta {
    private static final String DIRECTION_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyATPWlQtOpCVIhpB9us_dXbXahVMYa-Pk4";

    private MostrarRutaListener listener;
    private LatLng origin;
    private LatLng destination;

    private String origenAux;
    private String destinoAux;

    private List<Route> rutaEscogida = new ArrayList<Route>();



    public MostrarRuta(MostrarRutaListener listener, LatLng origen, LatLng destino) {
        this.listener = listener;
        this.origin = origen;
        this.destination = destino;
    }

    public MostrarRuta() {

    }

    public void execute() throws UnsupportedEncodingException {
        //listener.onMostrarRutaStart();
        new DetectarRuta().execute();
    }

    private String createUrl() throws UnsupportedEncodingException {
        origenAux = String.valueOf(origin.latitude) + "," + String.valueOf(origin.longitude);
        destinoAux = String.valueOf(destination.latitude) + "," + String.valueOf(destination.longitude);

        String urlOrigin = URLEncoder.encode(origenAux, "utf-8");
        String urlDestination = URLEncoder.encode(destinoAux, "utf-8");

        return DIRECTION_URL_API + "origin=" + urlOrigin + "&destination=" + urlDestination + "&key=" + GOOGLE_API_KEY;
    }

    private class DetectarRuta extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                rutaEscogida = decifrarJSon(DescargarRutaJson(createUrl()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            publishProgress(100);
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //listener.onMostrarRutaProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            listener.onRutaLista(rutaEscogida);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public String DescargarRutaJson(String link){
        try {
            URL url = new URL(link);
            InputStream is = url.openConnection().getInputStream();
            StringBuffer buffer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            return buffer.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Route> decifrarJSon(String res){
        try {
            return parseJSon2(res);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  null;
    }

    private List<Route> parseJSon2(String data) throws JSONException {
        if (data == null)
            return null;

        List<Route> routes = new ArrayList<Route>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
            Route route = new Route();

            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            JSONObject jsonLeg = jsonLegs.getJSONObject(0);
            JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
            JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
            JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
            JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");

            route.distance = new Distance(jsonDistance.getString("text"), jsonDistance.getInt("value"));
            route.duration = new Duration(jsonDuration.getString("text"), jsonDuration.getInt("value"));
            route.endAddress = jsonLeg.getString("end_address");
            route.startAddress = jsonLeg.getString("start_address");
            route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"));
            route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"));
            route.points = decodePolyLine2(overview_polylineJson.getString("points"));

            routes.add(route);
        }

        return routes;
    }

    public List<LatLng> decodePolyLine2(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
    }


}
