package com.example.mype;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap map;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    Location userLocation;
    private BottomSheetBehavior bottomSheetBehavior;
    private Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_main);

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.sheet));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    public void getCurrentLocation() {
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    userLocation = location;
                    supportMapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        new addMetro().execute(googleMap);
        map = googleMap;

        map.setOnMarkerClickListener(this);

        LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
        map.addMarker(new MarkerOptions().position(latLng).title("My position"));
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        TextView staitonName = findViewById(R.id.stationName);
        TextView distance = findViewById(R.id.distance);
        float fDistance[] = new float[3];

        staitonName.setText(marker.getTitle());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                marker.getPosition().latitude, marker.getPosition().longitude, fDistance);

        distance.setText(String.format("%.1f",  fDistance[0]/1000) + " km");
        //if(marker.getPosition() != new LatLng(userLocation.getLatitude(), userLocation.getLongitude()))
        //    drawRoute(marker.getPosition());

        return false;
    }

    private void drawRoute(LatLng destination){
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(new LatLng(userLocation.getLatitude(), userLocation.getLongitude())
                , destination);

        WebRequest webreq = new WebRequest();
        new MainActivity.ParserTask().execute(webreq.makeWebServiceCall(url, WebRequest.GET));

    }
    private String getDirectionsUrl(LatLng origin,LatLng dest){
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        String key = "key=AIzaSyDJrDoRp2ENbjtZXCLOjR1d_vLECBxhdCw";

        String parameters = str_origin+"&amp;"+str_dest+"&amp;"+key;

        return "https://maps.googleapis.com/maps/api/directions/json?"+parameters;
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            List<List<HashMap<String, String>>> routes = null;
            try{
                // Starts parsing data
                routes = DirectionsJSONParser.parse(new JSONObject(jsonData[0]));
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                if(polyline != null){
                    polyline.remove();
                }
                polyline = map.addPolyline(lineOptions);

            }else
                Toast.makeText(getApplicationContext(),"No route is found", Toast.LENGTH_LONG).show();
        }
    }

}
