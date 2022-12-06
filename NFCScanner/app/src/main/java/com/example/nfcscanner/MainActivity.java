package com.example.nfcscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //initialize required variables for location services
    private static final int PERMISSIONS_FINE_LOCATION = 0;
    Location currentLocation;
    double currentLocationLat, currentLocationLong;
    ArrayList<LatLng> locationStoreLatLng = new ArrayList<LatLng>();


    //initialize required NFC variables
    Tag detectedTag;
    NfcAdapter nfcAdapter;
    IntentFilter[] readTagFilters;
    PendingIntent pendingIntent;

    //initialize activity objects
    TextView textViewAccess;
    Spinner spinnerRoomAccess;

    //String and Int Variables to determine company role of NFC Card emulated and door requested to access
    String roomString, stringNFCContent, finalData, accessAttemptInfo;
    int intRoom, intNFCContent;
    String ipAddress = "";

    //allows for user location to be determined, absolutely essential to individual project
    FusedLocationProviderClient fusedLocationProviderClient;

    //location request, config file to determine settings required for FusedLocationProviderClient
    LocationRequest locationRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set all properties of LocationRequest (Yes I know it's deprecated)
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * 30);
        locationRequest.setFastestInterval(1000 * 5);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        //collects initial GPS location, and gives user prompt to use GPS location
        UpdateGPS();

        //pull ip address variable value from MainActivityIpAddress
        Intent intent = getIntent();
        ipAddress = intent.getStringExtra("ip_address");

        //textview "textViewAccess"
        textViewAccess = findViewById(R.id.textViewAccess);

        //ArrayAdapter to set spinners "SpinnerCompanyRolesAccess" and "spinnerEmulateNFCCard" to values from string.xml "companyroles"
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.rooms, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //Spinner "spinnerCompanyRolesAccess"
        spinnerRoomAccess = findViewById(R.id.spinnerRoomAccess);
        spinnerRoomAccess.setAdapter(adapter);

        //get device NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        readTagFilters = new IntentFilter[]{tagDetected, filter2};

        //initialize show map button that will send special condition to proccessing of 'MAP'
        Button buttonGenerateReport = findViewById(R.id.buttonShowMap);
        buttonGenerateReport.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(getApplicationContext(), "Map Opened!", Toast.LENGTH_SHORT);
                toast.show();
                collectData("MAP", intRoom, false);


                Intent intentMaps = new Intent(MainActivity.this, MapsActivity.class);
                intentMaps.putExtra("locationStoreLatLng", locationStoreLatLng);
                startActivity(intentMaps);
            }
        });

    }

    //from AndroidManifest.xml, when NDEF tag discovered in intent filter
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (getIntent().getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            readFromTag(getIntent());
        }
    }


    protected void onResume() {

        super.onResume();
        //enable NFC scanning in backround during app runtime
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, readTagFilters, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    UpdateGPS();
                } else {
                    Toast.makeText(this, "App requires location permission to run", Toast.LENGTH_SHORT).show();
                    finish();
            }
        }
    }

    private void UpdateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        //run check to determine if permission has been granted to determine users location using FusedLocationProviderClient
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //do nothing
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location tempLocation) {
                    //Got Permissions, now what
                    locationStoreLatLng.add(new LatLng(tempLocation.getLatitude(), tempLocation.getLongitude()));
                }
            });
        }
        else {
            //if not, request permission from user
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    //reads raw data from NDEF NFC tag, and translates it from byte data to String variable named 'stringNFCContent'
    public void readFromTag(Intent intent) {

        Ndef ndef = Ndef.get(detectedTag);

        try {
            ndef.connect();

            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (messages != null) {
                NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    ndefMessages[i] = (NdefMessage) messages[i];
                }
                NdefRecord record = ndefMessages[0].getRecords()[0];

                //converts record from above payload reader to byte array
                byte[] payload = record.getPayload();
                //converts byte array to string
                stringNFCContent = new String(payload);
                //appends off initial payload data
                stringNFCContent = stringNFCContent.substring(3);

                //stops NFC scanning
                ndef.close();

                //call onScan() method
                onScan();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Cannot Read From Tag.", Toast.LENGTH_LONG).show();
            System.out.println(e);
        }
    }

    private void onScan() {
        //spinner value chosen for room stored to string and then integer variable
        roomString = spinnerRoomAccess.getSelectedItem().toString();
        switch (roomString) {
            case "FRONT DOOR":
                intRoom = 0;
                break;
            case "CONFERENCE ROOM":
                intRoom = 1;
                break;
            case "BATHROOM 1":
                intRoom = 2;
                break;
            case "BATHROOM 2":
                intRoom = 3;
                break;
            case "CEO OFFICE":
                intRoom = 4;
                break;
            case "OFFICE 2":
                intRoom = 5;
                break;
            case "OFFICE 1":
                intRoom = 6;
                break;
            case "KITCHEN":
                intRoom = 7;
                break;
        }

        //NFC card content to string 'stringNFCContent' converted to int value
        switch (stringNFCContent) {
            case "CONFERENCE":
                intNFCContent = 0;
                break;
            case "GUEST":
                intNFCContent = 1;
                break;
            case "EMPLOYEE":
                intNFCContent = 2;
                break;
            case "CEO":
                intNFCContent = 3;
                break;
                //special case for if 'fire emergency' NFC card is scanned
            case "FIRE":
                intNFCContent = 4;
                break;
            //special case for if 'intruder emergency' NFC card is scanned
            case "INTRUDER":
                intNFCContent = 5;
                break;
            //default case for if NFC card data is not recognised as above cases
            default:
                intNFCContent = 10;
                break;
        }

        //if NFC card company role is allowed into selected room, goto accessGranted() and pas data to collectData()
        //otherwise if role is not allowed, goto accessDenied() and pass data to collectData()
        switch (intNFCContent) {
            case 0:
                if (intRoom == 1) {
                    accessGranted(textViewAccess);
                    collectData(stringNFCContent, intRoom, true);
                } else {
                    accessDenied(textViewAccess);
                    collectData(stringNFCContent, intRoom, false);
                }
                break;

            case 1:
                if (intRoom == 0 || intRoom == 2 || intRoom == 3) {
                    accessGranted(textViewAccess);
                    collectData(stringNFCContent, intRoom, true);
                } else {
                    accessDenied(textViewAccess);
                    collectData(stringNFCContent, intRoom, false);
                }
                break;

            case 2:
                if (intRoom == 0 || intRoom == 2 || intRoom == 3 || intRoom == 5 || intRoom == 6 || intRoom == 7) {
                    accessGranted(textViewAccess);
                    collectData(stringNFCContent, intRoom, true);
                } else {
                    accessDenied(textViewAccess);
                    collectData(stringNFCContent, intRoom, false);
                }
                break;

            case 3:
                if (intRoom == 0 || intRoom == 2 || intRoom == 3 || intRoom == 4 || intRoom == 7) {
                    accessGranted(textViewAccess);
                    collectData(stringNFCContent, intRoom, true);
                } else {
                    accessDenied(textViewAccess);
                    collectData(stringNFCContent, intRoom, false);
                }
                break;

            //special case if fire emergency NFC card is scanned, goto emergencyFire() and pass data to collectData()
            case 4:
                emergencyFire(textViewAccess);
                collectData("FIRE", intRoom, false);
                break;
            //special case if intruder emergency NFC card is scanned, goto emergencyIntruder() and pass data to collectData()
            case 5:
                emergencyIntruder(textViewAccess);
                collectData("INTRUDER", intRoom, false);
                break;
            //default case should NFC card content cannot be recognised, goto accessUnknown() and pass data to collectData()
            case 10:
                accessUnknown(textViewAccess);
                collectData("UNKNOWN", intRoom, false);
                break;

        }
    }

    private void accessGranted(TextView textViewAccess) {
        //on successful access attempt, change "textViewAccess" to access granted and play animation
        textViewAccess.setText("ACCESS GRANTED");

        //settings for animation
        Animation blink = new AlphaAnimation(0.f, 1.f);
        blink.setDuration(500);
        blink.setStartOffset(20);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(5);
        blink.setAnimationListener(new Animation.AnimationListener() {

            //play animation and with settings described below
            @Override
            public void onAnimationStart(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#0FFF00"));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFFFFF"));
                textViewAccess.setText("ACCESS PENDING");
            }
        });
        textViewAccess.startAnimation(blink);
    }

    private void accessDenied(TextView textViewAccess) {
        //on unsuccessful access attempt, change "textViewAccess" to access denied and play animation
        textViewAccess.setText("ACCESS DENIED");

        //settings for animation
        Animation blink = new AlphaAnimation(0.f, 1.f);
        blink.setDuration(500);
        blink.setStartOffset(20);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(5);
        blink.setAnimationListener(new Animation.AnimationListener() {

            //play animation and with settings described below
            @Override
            public void onAnimationStart(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FF0000"));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFFFFF"));
                textViewAccess.setText("ACCESS PENDING");
            }
        });
        textViewAccess.startAnimation(blink);
    }

    private void accessUnknown(TextView textViewAccess) {
        //on event NFC card data does not meet cases, set textViewAccess to "UNKNOWN CARD" and play animation
        textViewAccess.setText("UNKNOWN CARD");

        //settings for animation
        Animation blink = new AlphaAnimation(0.f, 1.f);
        blink.setDuration(500);
        blink.setStartOffset(20);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(5);
        blink.setAnimationListener(new Animation.AnimationListener() {

            //play animation and with settings described below
            @Override
            public void onAnimationStart(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFAA00"));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFFFFF"));
                textViewAccess.setText("ACCESS PENDING");
            }
        });
        textViewAccess.startAnimation(blink);
    }

    private void emergencyFire(TextView textViewAccess) {
        //on fire emergency, set textViewAccess to "FIRE ALERT" and play animation
        textViewAccess.setText("FIRE ALERT");

        //settings for animation
        Animation blink = new AlphaAnimation(0.f, 1.f);
        blink.setDuration(500);
        blink.setStartOffset(20);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(2000);
        blink.setAnimationListener(new Animation.AnimationListener() {

            //play animation and with settings described below
            @Override
            public void onAnimationStart(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FF4200"));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFFFFF"));
                textViewAccess.setText("ACCESS PENDING");
            }
        });
        textViewAccess.startAnimation(blink);
    }

    private void emergencyIntruder(TextView textViewAccess) {
        //on intruder emergency, set textViewAccess to "INTRUDER ALERT" and play animation
        textViewAccess.setText("INTRUDER ALERT");

        //settings for animation
        Animation blink = new AlphaAnimation(0.f, 1.f);
        blink.setDuration(500);
        blink.setStartOffset(20);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(2000);
        blink.setAnimationListener(new Animation.AnimationListener() {

            //play animation and with settings described below
            @Override
            public void onAnimationStart(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#00BDFF"));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textViewAccess.setTextColor(Color.parseColor("#FFFFFF"));
                textViewAccess.setText("ACCESS PENDING");
            }
        });
        textViewAccess.startAnimation(blink);
    }

    //collects individual data from access attempt (NFC Card data, room accessed, and if attempt was successful
    //stores data into single string finalData to be sent to sendData()
    private void collectData(String stringNFCContent, int intRoom, boolean access) {
        //collect current GPS long lat location
        UpdateGPS();
        //if emergency event, send only type of emergency and no door or access info
        if (stringNFCContent.equals("FIRE") || stringNFCContent.equals("INTRUDER")|| stringNFCContent.equals("MAP")) {
            finalData = stringNFCContent;
        }
        else {
            finalData = (stringNFCContent + "," + intRoom + "," + access + "," + locationStoreLatLng);
        }
        sendData();
    }

    //on seperate thread from GUI, run client side of java socket to send finalData to server
    private void sendData() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                DataOutputStream dataOutputStream = null;
                //open socket connection to user inputted ipAddress at port 4000
                try (Socket socket = new Socket(ipAddress, 4000)) {
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    while (true) {
                        //if new user data is different from saved previous attempt, send data over socket and save new user data as current
                        //otherwise, check newest input until different from saved input
                        if (accessAttemptInfo != finalData) {
                            dataOutputStream.writeUTF(finalData);
                        }
                        accessAttemptInfo = finalData;
                    }

                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        });
    }
}
