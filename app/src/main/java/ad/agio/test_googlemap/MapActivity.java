package ad.agio.test_googlemap;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.sothree.slidinguppanel.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final String TAG = MapActivity.class.getSimpleName();
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private SlidingUpPanelLayout panel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        panel = findViewById(R.id.slidingPanel);
        panel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) { }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });

    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(final GoogleMap map) {

        this.map = map;

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (panel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    panel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                } else {
                    MarkerOptions mOptions = new MarkerOptions();
                    Double latitude = latLng.latitude; // 위도
                    Double longitude = latLng.longitude; // 경도
                    mOptions.title(getAddress(latitude, longitude));
                    mOptions.snippet(latitude.toString() + ", " + longitude.toString());
                    mOptions.position(new LatLng(latitude, longitude));
                    map.addMarker(mOptions);
                }
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                final ListView listView = findViewById(R.id.listView);
                listView.setAdapter(new ArrayAdapter < String > (getApplicationContext(),
                        android.R.layout.simple_list_item_1,
                        new String[] {
                                marker.getTitle(),
                                marker.getPosition().latitude + "",
                                marker.getPosition().longitude + ""
                        }));

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView < ? > parent, View view, int position, long id) {
                        Toast.makeText(getApplicationContext(),
                                position + " 번째 값 : " + parent.getItemAtPosition(position),
                                Toast.LENGTH_SHORT).show();
                    }
                });

                SlidingUpPanelLayout panel = findViewById(R.id.slidingPanel);
                panel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                return false;
            }
        });

        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener < Location > () {
                    @Override
                    public void onComplete(@NonNull Task < Location > task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                Log.d(TAG, lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude());
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * getAdress from latitude and longitude
     * @param latitude
     * @param longitude
     * @return
     * @throws IOException
     */
    public String getAddress(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            List<Address> fromLocation = geocoder.getFromLocation(latitude, longitude, 1);
            Address address = fromLocation.get(0);
            StringBuilder stringBuilder = new StringBuilder(); // 대구시 수성구 사월동
            stringBuilder.append(address.getLocality()).append(" "); //시
            stringBuilder.append(address.getSubLocality()).append(" "); //구
            stringBuilder.append(address.getThoroughfare()); //동
            //return stringBuilder.toString();
            return address.getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
            return "no address";
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true); //내 위치로 돌아가는 버튼 활성화
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}