package com.example.easyrdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Criteria;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;



public class MapsActivity extends FragmentActivity implements LocationListener{

    GoogleMap map;
    ArrayList<LatLng> markerPoints;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        markerPoints = new ArrayList<LatLng>();
        SupportMapFragment fm = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        final Button btnDraw = (Button)findViewById(R.id.btn_draw);
        Button btnReport = (Button)findViewById(R.id.btn_report);
        final Button btnSub = (Button)findViewById(R.id.btn_submit);

        btnSub.setVisibility(View.INVISIBLE);
        map = fm.getMap();

        if(map!=null){

            map.setMyLocationEnabled(true);

            map.getUiSettings().setZoomControlsEnabled(true);

            //LatLng center = new LatLng(-1.281744, 36.815000);

            //map.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 13));
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            final Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);

            if(location!=null){
                onLocationChanged(location);
            }
            locationManager.requestLocationUpdates(provider, 20000, 0, this);

            final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

            markerPoints.add(myLocation);

            MarkerOptions options = new MarkerOptions();

            options.position(myLocation);

            if(markerPoints.size()==1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }

            map.addMarker(options);
            Toast mess2 = Toast.makeText(MapsActivity.this, "Tap on the Location you would like Route information on", Toast.LENGTH_LONG);
            mess2.show();

            map.setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    if(markerPoints.size()>1){
                        markerPoints.clear();
                        map.clear();
                    }

                    markerPoints.add(point);

                    MarkerOptions options = new MarkerOptions();

                    options.position(point);

                    if(markerPoints.size()==1){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    }else if(markerPoints.size()==2){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    map.addMarker(options);
                    Toast mess3 = Toast.makeText(MapsActivity.this, "Tapping the screen again will remove the information", Toast.LENGTH_LONG);

                    mess3.show();

                }
            });

            btnDraw.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    Toast mess4 = Toast.makeText(MapsActivity.this, "Please wait a few moments while the information is retrieved", Toast.LENGTH_LONG);

                    mess4.show();

                    new RetrieveTask().execute();
                }
            });

            btnReport.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    map.clear();

                    final int once = 0;

                    btnDraw.setVisibility(View.INVISIBLE);

                    Toast mess = Toast.makeText(MapsActivity.this, "Tap the specific location on the map where you have witnessed free flowing traffic", Toast.LENGTH_LONG);

                    mess.show();

                    map.setOnMapClickListener(new OnMapClickListener() {

                        @Override
                        public void onMapClick(final LatLng point) {

                            map.clear();

                            markerPoints.add(point);

                            MarkerOptions options = new MarkerOptions();

                            options.position(point);

                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                            map.addMarker(options);

                            if(once == 0) {
                                btnSub.setVisibility(View.VISIBLE);
                            }else{
                                btnSub.setVisibility(View.INVISIBLE);
                            }

                            btnSub.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {

                                    addMarker(point);
                                    sendToServer(point);

                                    Toast mess1 = Toast.makeText(MapsActivity.this, "Your Report has been sent, Please Restart Apllication", Toast.LENGTH_LONG);

                                    mess1.show();

                                    btnSub.setVisibility(View.INVISIBLE);

                                    finish();
                                    startActivity(getIntent());
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Waypoints
        String waypoints = "";
        for(int i=2;i<markerPoints.size();i++){
            LatLng point  = (LatLng) markerPoints.get(i);
            if(i==2)
                waypoints = "waypoints=";
            waypoints += point.latitude + "," + point.longitude + "|";
        }

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+waypoints;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }
    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception downloading", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
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
            MarkerOptions markerOptions = new MarkerOptions();

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
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            map.addPolyline(lineOptions);
        }
    }


    //Start of getting current location
    @Override
    public void onLocationChanged(Location location) {

        TextView tvLocation = (TextView) findViewById(R.id.tv_location);

        // Getting latitude of the current location
        double latitude = location.getLatitude();

        // Getting longitude of the current location
        double longitude = location.getLongitude();

        // Creating a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);

        // Showing the current location in Google Map
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        map.animateCamera(CameraUpdateFactory.zoomTo(13));

        // Setting latitude and longitude in the TextView tv_location
        tvLocation.setText("Latitude:" +  latitude  + ", Longitude:"+ longitude );

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    //START OF SENDING AND GETTING ROUTE FROM C-PANEL

    // Adding marker on the GoogleMaps
    private void addMarker(LatLng latlng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.title(latlng.latitude + "," + latlng.longitude);
        map.addMarker(markerOptions);
    }

    // Invoking background thread to store the touched location in Remote MySQL server
    private void sendToServer(LatLng latlng) {
        new SaveTask().execute(latlng);
    }
    // Background thread to save the location in remove MySQL server
    private class SaveTask extends AsyncTask<LatLng, Void, Void> {
        @Override
        protected Void doInBackground(LatLng... params) {
            String lat = Double.toString(params[0].latitude);
            String lng = Double.toString(params[0].longitude);
            String strUrl = "http://www.rutzilla.byethost31.com/save.php";
            URL url = null;
            try {
                url = new URL(strUrl);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());

                outputStreamWriter.write("lat=" + lat + "&lng="+lng);
                outputStreamWriter.flush();
                outputStreamWriter.close();

                InputStream iStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";

                while( (line = reader.readLine()) != null){
                    sb.append(line);
                }

                reader.close();
                iStream.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    // Background task to retrieve locations from remote mysql server
    private class RetrieveTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            String strUrl = "http://www.rutzilla.byethost31.com/retrieve.php";
            URL url = null;
            StringBuffer sb = new StringBuffer();
            try {
                url = new URL(strUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream iStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(iStream));
                String line = "";
                while( (line = reader.readLine()) != null){
                    sb.append(line);
                }

                reader.close();
                iStream.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask2().execute(result);
        }
    }

    // Background thread to parse the JSON data retrieved from MySQL server
    private class ParserTask2 extends AsyncTask<String, Void, List<HashMap<String, String>>>
    {
        @Override
        protected List<HashMap<String,String>> doInBackground(String... params) {
            MarkerJSONParser markerParser = new MarkerJSONParser();
            JSONObject json = null;
            try {
                json = new JSONObject(params[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            List<HashMap<String, String>> markersList = markerParser.parse(json);
            return markersList;
        }


        @Override
        protected void onPostExecute(List<HashMap<String, String>> result)
        {
            LatLng origin = markerPoints.get(0);
            LatLng dest = markerPoints.get(1);
            double temp, orglat = origin.latitude, orglng = origin.longitude, destlat = dest.latitude, destlng = dest.longitude;
            double latdiff = (orglat - destlat)/3, lngdiff = (orglng - destlng)/3;
            LatLng waypoint = new LatLng((origin.latitude + dest.latitude)/2, (origin.longitude + dest.longitude) / 2);
            int point = 0;

            MarkerOptions options = new MarkerOptions();

            if (markerPoints.size() > 1) {
                 options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
              }


            for(int j = 0; j < 3; j++) {

                int k;
                k = j;

                if(j == 0)
                {
                    destlat = origin.latitude - (latdiff);
                    destlng = origin.longitude - (lngdiff);

                }else if(j == 1)
                {
                    orglat = origin.latitude - (latdiff);
                    destlat = (origin.latitude - (latdiff))- (latdiff);
                    orglng = origin.longitude - (lngdiff);
                    destlng = (origin.longitude - (lngdiff)- (lngdiff));

                }else if(j == 2)
                {
                    orglat = (origin.latitude - (latdiff))- (latdiff);
                    destlat = ((origin.latitude - (latdiff))- (latdiff))- (latdiff);
                    orglng = (origin.longitude - (lngdiff))- (lngdiff);
                    destlng =((origin.longitude - (lngdiff))- (lngdiff))- (lngdiff);

                }

                for (int i = 0; i < result.size(); i++) {
                    HashMap<String, String> marker = result.get(i);
                    LatLng latlng = new LatLng(Double.parseDouble(marker.get("lat")), Double.parseDouble(marker.get("lng")));

                    if (orglat > destlat && orglng < destlng) {
                        if (((orglat > latlng.latitude) && (destlat < latlng.latitude)) && ((orglng < latlng.longitude) && (destlng > latlng.longitude))) {
                            if(point < 8 && k == j) {
                                markerPoints.add(latlng);
                                addMarker(latlng);
                                point++;
                                k++;
                            }
                        }
                    } else if (orglat < destlat && orglng < destlng) {
                        if (((orglat < latlng.latitude) && (destlat > latlng.latitude)) && ((orglng < latlng.longitude) && (destlng > latlng.longitude))) {
                            if(point < 8 && k == j) {
                                markerPoints.add(latlng);
                                addMarker(latlng);
                                point++;
                                k++;
                            }
                        }
                    } else if (orglat < destlat && orglng > destlng) {
                        if (((orglat < latlng.latitude) && (destlat > latlng.latitude)) && ((orglng > latlng.longitude) && (destlng < latlng.longitude))) {
                            if(point < 8 && k == j) {
                                markerPoints.add(latlng);
                                addMarker(latlng);
                                point++;
                                k++;
                            }
                        }
                    } else if (orglat > destlat && orglng > destlng) {
                        if (((orglat > latlng.latitude) && (destlat < latlng.latitude)) && ((orglng > latlng.longitude) && (destlng < latlng.longitude))) {
                            if(point < 8 && k == j) {
                                markerPoints.add(latlng);
                                addMarker(latlng);
                                point++;
                                k++;
                            }
                        }
                    }
                    if(i == (result.size() - 1) && k == j)
                    {
                        LatLng midway = new LatLng((orglat + destlat)/2, (orglng + destlng)/2);
                        markerPoints.add(midway);
                        addMarker(midway);
                        point++;
                        k++;
                    }
                }
            }

            String url = getDirectionsUrl(origin, dest);

            DownloadTask downloadTask = new DownloadTask();

            downloadTask.execute(url);

            System.out.println("Total number of points = "+ point);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}