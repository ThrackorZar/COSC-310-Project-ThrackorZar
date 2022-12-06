package com.example.nfcscanner;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.nfcscanner.databinding.ActivityMapsBinding;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ArrayList<LatLng> locationStoreLatLng = new ArrayList<LatLng>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationStoreLatLng = (ArrayList<LatLng>) getIntent().getSerializableExtra("locationStoreLatLng");

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        int i = locationStoreLatLng.size();
        LatLng location5 = new LatLng((locationStoreLatLng.get(i-1).latitude), (locationStoreLatLng.get(i-1).longitude));
        LatLng location4 = new LatLng((locationStoreLatLng.get(i-2).latitude), (locationStoreLatLng.get(i-2).longitude));
        LatLng location3 = new LatLng((locationStoreLatLng.get(i-3).latitude), (locationStoreLatLng.get(i-3).longitude));
        LatLng location2 = new LatLng((locationStoreLatLng.get(i-4).latitude), (locationStoreLatLng.get(i-4).longitude));
        LatLng location1 = new LatLng((locationStoreLatLng.get(i-5).latitude), (locationStoreLatLng.get(i-5).longitude));


        mMap.addMarker(new MarkerOptions().position(location5).title("Location 5"));
        mMap.addMarker(new MarkerOptions().position(location4).title("Location 4"));
        mMap.addMarker(new MarkerOptions().position(location3).title("Location 3"));
        mMap.addMarker(new MarkerOptions().position(location2).title("Location 2"));
        mMap.addMarker(new MarkerOptions().position(location1).title("Location 1"));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location5));
    }
}