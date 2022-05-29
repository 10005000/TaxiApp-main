package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {


    private GoogleMap mMap;
    Location lastLocation;
    LocationRequest locationRequest;
    GoogleApiClient googleApiClient;
    SearchView searchView;
    private TextView myAddress;
    private Button saveAddressBtn;
    private DatabaseReference reference;
    private FirebaseAuth mAuth;
    private String CustomerId;
    private FirebaseDatabase database;
    private DatabaseReference RootRef;
    private String address;
    private boolean getAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

//        if(getIntent().getExtras().get("address") != null){
//            getAddress = true;
//        }
//        else {
//            address = getIntent().getExtras().get("address").toString();
//        };

        myAddress = findViewById(R.id.address);
        saveAddressBtn = findViewById(R.id.save_address_button);
        saveAddressBtn.setVisibility(View.GONE);
        myAddress.setVisibility(View.GONE);
        mAuth = FirebaseAuth.getInstance();
        CustomerId = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        searchView = findViewById(R.id.search_location);
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("Users").child("Customers");
        // Getting a reference to the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(getAddress == true) {
            address = getIntent().getExtras().get("address").toString();

            searchView.setQuery(address, false);
            //      searchView.clearFocus();

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    String location = searchView.getQuery().toString();
                    List<Address> addressList = null;

                    if (location != null || !location.equals("")) {
                        Geocoder geocoder = new Geocoder(MapActivity.this);
                        try {
                            addressList = geocoder.getFromLocationName(location, 5);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));


                    }


                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }else {

        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 5);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                }


                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().isMapToolbarEnabled();
        mMap.getUiSettings().isZoomControlsEnabled();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){

            @Override
            public void onMapClick(final LatLng latLng) {


                if (mMap != null) {
                    Marker hamburg = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0))
                            .title("Пассажир").snippet("Забрать тут")
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.close)));
                    hamburg.isVisible();
                    hamburg.setVisible(true);

                    CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 18);
                    mMap.animateCamera(update);

                    saveAddressBtn.setVisibility(View.VISIBLE);
                    myAddress.setVisibility(View.VISIBLE);
                    Locale locale = new Locale("ru_RU");

                    Geocoder geocoder = new Geocoder(MapActivity.this, locale);
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latLng.latitude,
                                latLng.longitude, 1);

                        if (addresses != null) {
                            Address returnedAddress = addresses.get(0);
                            final String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                            String city = addresses.get(0).getLocality();
                            String state = addresses.get(0).getAdminArea();
                            String country = addresses.get(0).getCountryName();
                            String postalCode = addresses.get(0).getPostalCode();
                            String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
                            StringBuilder strReturnedAddress = new StringBuilder(
                                    address);
                            for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                                strReturnedAddress
                                        .append(returnedAddress.getAddressLine(i)).append(
                                        "\n");
                            }

                            myAddress.setText(strReturnedAddress.toString());

                            saveAddressBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    Double lat = latLng.latitude;
                                    Double lon = latLng.longitude;

                                    HashMap<String, Object> productMap = new HashMap<>();
                                    productMap.put("pid", CustomerId);
                                    productMap.put("address1", address);
                                    productMap.put("lat", lat);
                                    productMap.put("lon", lon);


                                    RootRef.child("Customers").child("Orders").child(CustomerId).updateChildren(productMap)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Intent intent = new Intent(MapActivity.this, CustomersMapActivity.class);
                                                        startActivity(intent);

                                                    } else {
                                                        String message = task.getException().toString();
                                                        Toast.makeText(MapActivity.this, "Ошибка:  " + message, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                }

                            });

                        } else {
                            myAddress.setText("No Address returned!");
                        }
                    } catch (IOException e) {
                        // Автоматически сгенерированный блок catch
                        e.printStackTrace();
                        myAddress.setText("Cannot get Address!");
                    }

                }
            }
        });


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000000);
        locationRequest.setFastestInterval(100000000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));


    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this, R.style.AlertDialog);
        builder.setTitle("Отметьте точку на карте, откуда Вас забрать.");

        builder.setNegativeButton("Понятно, спасибо!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}