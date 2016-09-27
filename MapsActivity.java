package com.example.administrator.mapfrag;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory; //some bs imports
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap;
    private Marker DRONE;

    private double uav_lat = -37.00081, uav_long = 174.87;
    private LatLng UAV_starting;

    private Polyline line;
    private Polyline uavLine;

    private PolylineOptions flightPath = new PolylineOptions(); //this is what draw the flight path
    private PolylineOptions uav_travelPath = new PolylineOptions(); //this is to draw where the uav has been, press to flight para button to see

    private List<Marker> flightPoints = new ArrayList<>();  //where all the flight points are stored, you're going to send whats in here to sherwin
    private List<Marker> Paste_flightPoints = new ArrayList<>();    //i cant remember what this is for in all honestly

    private boolean copying = false;

    public ArrayList<Double> singlePoint = new ArrayList<Double>();    //*Heemesh*
    public ArrayList<Double> multiplePoint = new ArrayList<Double>();
    public ArrayList<Double> bearingPoint = new ArrayList<Double>();

    public ArrayList<Double> sendingPoints = new ArrayList<Double>();

    private static final int REQUEST_ENABLE_BT = 1;         //*Heemesh*
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "00:1A:7D:DA:71:13";

    private static final String TAG = "LogCat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //don't worry about this
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() { //dont worry about this
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Get the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we fucked it up.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


     //this is where shit gets done bruh
     //most important method in the whole entire class
     //basically does everything
    //very important
    // dont worry about it you don't need to know it though
    private void setUpMap() {

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(-37.00081, 174.87) , 17.0f) );

        mMap.setOnMapClickListener(new OnMapClickListener() {
            public void onMapClick(LatLng destination) {

                if(copying==true){
                }
                else{

                Marker newPoint =  mMap.addMarker(new MarkerOptions()
                        .position(destination)
                        .title(String.valueOf(destination.latitude) + ", " + String.valueOf(destination.longitude)));
               // mMap.addMarker(new MarkerOptions().position(destination)
               //         .title(String.valueOf(destination.latitude)
               //                 + ", " + String.valueOf(destination.longitude)));
                flightPath.add(destination);

                if(flightPoints.size() >= 2)
                    line.remove();

                line = mMap.addPolyline(flightPath
                    .width(3)
                    .color(Color.RED));
                flightPoints.add(newPoint); }
            }
        });

       /* mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker arg0) {
                arg0.remove();
                return true;
            }
        }); */

       // UAV_starting = new LatLng(uav_lat,uav_long );
        DRONE = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(uav_lat,uav_long ))
                .title("UAV")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        uav_travelPath.add(new LatLng(uav_lat,uav_long));
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.uav_icon)));
    }

    public void clearScreen(View view){
        flightPath = new PolylineOptions();
        uav_travelPath = new PolylineOptions();
        line.remove();

        for(Marker marker: flightPoints){
            marker.remove();
        }
        flightPoints.clear();
    }

    public void undoMarker(View view){

        if(!flightPoints.isEmpty()) {
            Marker removeMark = flightPoints.get((flightPoints.size()) - 1);
            removeMark.remove();
            flightPoints.remove((flightPoints.size()) - 1);
            flightPath = new PolylineOptions();
            line.remove();
            for(Marker marker: flightPoints){
                flightPath.add(marker.getPosition());
            }
            line = mMap.addPolyline(flightPath
                    .width(3)
                    .color(Color.RED));

        }

    }

    public void copyPattern(View view){
        copying = !copying;
    }

    public void saveFP(View view){

    }
    //this is what you need to look at !

    /**
     *
     * Look here mofxcker
     *
     * you can delete anything you want here, its the button that will be used
     * to send data anyways
     *
     */
    public void sendData(View view) {

        sendingPoints.clear();
       /* if (flightPoints.size() < 2) {  //checks if only one marks is on the map, if so it prints the lat/lng in a toast
                String position = "" + (flightPoints.get(0)).getPosition(); //this is about you get the lat/lng, it's separated by commas
                                                                            //i made it a string so hopefully that helps
                                                                            //as per before flightpoints is where the flight path the user enters are stored
                                                                            //the '0' is where in array so to speak, just try in through a for loop, ill help you
                                                                            // out if you get stuck
            Toast.makeText(getApplicationContext(), position, Toast.LENGTH_SHORT).show();
        } else {    //you can keep this if you like, its just a method to tell the user data has been sent, if you want to keep it you'll
                    //have to change to if-else statements though
            line.setColor(Color.GREEN);
            for (Marker marker : flightPoints) {
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                Toast.makeText(getApplicationContext(), "Data Successfully Sent", Toast.LENGTH_SHORT).show();
            }
        }*/
        line.setColor(Color.GREEN);
        for (Marker marker : flightPoints) {
           //sendingPoints.add(position);
          // String stupidtest = "" + test;
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            double lat = marker.getPosition().latitude;
            double lng = marker.getPosition().longitude;
            sendingPoints.add(lat);
            sendingPoints.add(lng);
        }
        Toast.makeText(getApplicationContext(), "Data Successfully Sent", Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(), sendingPoints.get(sendingPoints.size()-1), Toast.LENGTH_SHORT).show();
        ArrayList<Double> testArray = new ArrayList<Double>();

        Send(sendingPoints);
    }

    public void getStats(View view){

        //DRONE.
        DRONE.remove();
        uav_long =  uav_long + 0.00001;
        uav_lat = uav_lat + 0.00001;
        LatLng newP = new LatLng(uav_lat, uav_long);
        uav_travelPath.add(newP);
        uavLine = mMap.addPolyline(uav_travelPath.width(3).color(Color.MAGENTA));
        DRONE = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(uav_lat,uav_long ))
                .title("UAV")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    }

    private void waiting() {    //*Heemesh*
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Task implements Runnable {    //*Heemesh*
        @Override
        public void run() {
            try {
                InputStream inStream = btSocket.getInputStream();
                BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
                for (int i = 0; i < 1; i++) {		//while(true)
                    String receivedString = bReader.readLine();
                    //Log.v(TAG, receivedString);

                    if((receivedString.charAt(0)) == 'B')
                    {
                        String test7 = receivedString.substring(1);
                        List<String> bearingSplit = Arrays.asList(test7.split(","));
                        for (int j = 0; j < bearingSplit.size(); j++) {
                            double value = Double.parseDouble(bearingSplit.get(j));
                            bearingPoint.add(value);
                            //Log.v(TAG, "CONVERTED VALUE BP: " + value);
                        }
                    }
                    else if(receivedString.charAt(0) == 'M')
                    {
                        String test7 = receivedString.substring(1);
                        List<String> multipleSplit = Arrays.asList(test7.split(","));
                        for (int j = 0; j < multipleSplit.size(); j++) {
                            double value = Double.parseDouble(multipleSplit.get(j));
                            multiplePoint.add(value);
                            //Log.v(TAG, "CONVERTED VALUE MP: " + value);
                        }
                    }
                    else if(receivedString.charAt(0) == 'S')
                    {
                        String test7 = receivedString.substring(1);
                        List<String> singleSplit = Arrays.asList(test7.split(","));
                        for (int j = 0; j < singleSplit.size(); j++) {
                            double value = Double.parseDouble(singleSplit.get(j));
                            singlePoint.add(value);
                            //Log.v(TAG, "CONVERTED VALUE SP: " + value);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void Send(ArrayList<Double> wayPoints) {    //*Heemesh*
        // Set up a pointer to the remote node using it's address.
        Log.d(TAG, "test");
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        Log.d(TAG, "**********************************");
        //BluetoothDevice device = null;
        // Two things are needed to make a connection:
        // A MAC address, which we got above.
        // A Service ID or UUID. In this case we are using the
        // UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

//            Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {

            AlertBox("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
/*
        // Discovery is resource intensive. Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection. This will block until it connects.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                AlertBox("Fatal Error","In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            AlertBox("Fatal Error","In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }


        ArrayList<String> xWayPoints = new ArrayList<String>();
        ArrayList<String> yWayPoints = new ArrayList<String>();
//	  xWayPoints.add("43.645");
//	  xWayPoints.add("1212.2");
//	  yWayPoints.add("93.2");
//	  yWayPoints.add("43996.55");

//	  String cStr1 = Double.toString(wayPoints.get(0));
//	  xWayPoints.add(cStr1);

        for(int i = 0; i<wayPoints.size(); i++)
        {
            if(i%2==0){
                String cStr1 = Double.toString(wayPoints.get(i));
                xWayPoints.add(cStr1);
            }
            else{
                String cStr2 = Double.toString(wayPoints.get(i));
                yWayPoints.add(cStr2);
            }
        }

//	  String hi = xWayPoints.get(0);
//	  Toast toast = Toast.makeText(getApplicationContext(), hi , Toast.LENGTH_SHORT);
//	  toast.show();


//	  ArrayList<String> originalAL = new ArrayList<String>();
//	  originalAL.add(s1);
//	  originalAL.add(s2);

        String xString = "";
        String yString = "";

        for(String j : xWayPoints)
        {
            xString += j + ", ";
        }
        for(String s : yWayPoints)
        {
            yString += s + ", ";
        }



        String finalString = xString + ":" + yString + "\n";

//	  Toast toast = Toast.makeText(getApplicationContext(), finalString , Toast.LENGTH_SHORT);
//	  toast.show();

        byte[] msgBuffer = finalString.getBytes();

        try {
            outStream.write(msgBuffer);

            // Toast toast = Toast.makeText(getApplicationContext(), "Sending...", Toast.LENGTH_SHORT);
            // toast.show();

        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00")){
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
            }
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
            AlertBox("Error", msg);
        }*/
    }

    public void AlertBox( String title, String message ){       //*Heemesh*
        new AlertDialog.Builder(this)
                .setTitle( title )
                .setMessage( message + " Press OK to exit." )
                .setPositiveButton("OK", new OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                }).show();
    }

    private void CheckBTState() {       //*Heemesh*
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
//        out.append("\n...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

}
