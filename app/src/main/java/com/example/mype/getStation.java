package com.example.mype;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class addMetro extends AsyncTask<GoogleMap, Void, MarkerOptions[]> {

    GoogleMap map;
    private static String url = "https://my-json-server.typicode.com/BeeWhy/metro/db";

    private static final String TAG_STATIONS = "stations";
    private static final String TAG_NAME = "name";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";

    @Override
    protected MarkerOptions[] doInBackground(GoogleMap... googleMaps) {
        WebRequest webreq = new WebRequest();

        Log.d("addMetro", "enter!!");
        String jsonStr = webreq.makeWebServiceCall(url, WebRequest.GET);
        map = googleMaps[0];
        return ParseJSON(jsonStr);
    }

    @Override
    protected void onPostExecute(MarkerOptions[] markerOptions) {
        super.onPostExecute(markerOptions);
        for (int i = 0; i < markerOptions.length; i++) {
            map.addMarker(markerOptions[i]);
        }
    }

    private MarkerOptions[] ParseJSON(String json) {
        MarkerOptions[] markerOptions;
        if (json != null) {
            try {
                JSONObject jsonObj = new JSONObject(json);
                JSONArray stations = jsonObj.getJSONArray(TAG_STATIONS);

                markerOptions = new MarkerOptions[stations.length()];
                String name;
                double latitude, longitude;
                for (int i = 0; i < stations.length(); i++) {
                    JSONObject c = stations.getJSONObject(i);
                    name = c.getString(TAG_NAME);
                    latitude = Double.parseDouble(c.getString(TAG_LATITUDE));
                    longitude = Double.parseDouble(c.getString(TAG_LONGITUDE));

                    LatLng latLng = new LatLng(latitude, longitude);

                    markerOptions[i] = new MarkerOptions().position(latLng).title(name);
                    markerOptions[i].icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                }
                return markerOptions;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            Log.e("ServiceHandler", "Couldn't get any data from the url");
            return null;
        }
    }
}

