package com.example.mytaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.sql.Driver;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;


    private Button customerLogoutButton, settingsButton;
    private Button callTaxiButton;
    private String customerID, driverFoundID;
    private LatLng CustomerPosition;
    private int radius = 1;
    private Boolean driverFound = false, requestType= false;

    private FirebaseUser currentUser;
    private DatabaseReference CustomerDatabaseRef;
    private DatabaseReference DriversAvailableRef;
    private DatabaseReference DriversRef;
    private DatabaseReference DriversLocationRef;

    private ValueEventListener DriverLocationRefListener;
    private ImageView callDriver;
    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView driverPhoto;
    private RelativeLayout relativeLayout;

    private TextView map1, map2;
    private TextView txtDistance;
    private EditText inputAddress1, inputAddress2;

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String CustomerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        customerLogoutButton = (Button) findViewById(R.id.cuslomer_logout_button);
        settingsButton = (Button)findViewById(R.id.cuslomer_settings_button);
        callTaxiButton = (Button)findViewById(R.id.cuslomer_order_button);
        callDriver = findViewById(R.id.call_to_driver);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Requests");
        DriversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
        DriversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        txtName = (TextView)findViewById(R.id.driver_name);
        txtPhone = (TextView)findViewById(R.id.driver_phone_number);
        txtCarName = (TextView)findViewById(R.id.driver_car);
        driverPhoto = (CircleImageView)findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.rel1);
        txtDistance = (TextView) findViewById(R.id.distance_details);
        relativeLayout.setVisibility(View.INVISIBLE);

        map1 = findViewById(R.id.add_address1_with_map);
        map1.setVisibility(View.VISIBLE);
        map2 = findViewById(R.id.add_address2_with_map);
        map2.setVisibility(View.VISIBLE);

        inputAddress1 = findViewById(R.id.first_address_name);
        inputAddress2 = findViewById(R.id.second_address_name);
        RootRef = FirebaseDatabase.getInstance().getReference();

        map1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(CustomersMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(CustomersMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        Intent welcomeIntent = new Intent(CustomersMapActivity.this, MapActivity.class);
                        if (!TextUtils.isEmpty(inputAddress1.getText().toString())){
                            welcomeIntent.putExtra("address", inputAddress1.getText().toString());
                        }
                        else {
                            welcomeIntent.putExtra("address", " ");
                        }
                        startActivity(welcomeIntent);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                                10);

                    }
                }
            }
        });

        map2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent welcomeIntent = new Intent(CustomersMapActivity.this, SecondMapActivity.class);
                if (!TextUtils.isEmpty(inputAddress2.getText().toString())){
                    welcomeIntent.putExtra("address2", inputAddress2.getText().toString());
                }
                else {
                    welcomeIntent.putExtra("address2", " ");
                }
                startActivity(welcomeIntent);
            }
        });

        RootRef.child("Customers").child("Orders").child(customerID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount()>0) {
                    if (snapshot.hasChild("address1")) {
                        map1.setVisibility(View.GONE);

                        String address = snapshot.child("address1").getValue().toString();
                        inputAddress1.setText(address);

                    }
                    if (snapshot.hasChild("address2")) {
                        map2.setVisibility(View.GONE);

                        String address2 = snapshot.child("address2").getValue().toString();
                        inputAddress2.setText(address2);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        customerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mAuth.signOut();
                    LogoutCustomer();
                }catch(Exception e){

            }}
        });

        callTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (requestType)
                {
                    requestType = false;
                    GeoFire geofire = new GeoFire(CustomerDatabaseRef);
                    geofire.removeLocation(customerID);

                    if(PickUpMarker !=null)
                    {
                        PickUpMarker.remove();
                    }
                    if(driverMarker !=null)
                    {
                        driverMarker.remove();
                    }

                    callTaxiButton.setText("Вызвать такси");
                    if (driverFound!=null)
                    {
                        DriversRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");

                        DriversRef.removeValue();

                        driverFoundID = null;
                    }

                    driverFound = false;
                    radius = 1;


                }
                else {
                    requestType = true;

                    GeoFire geofire = new GeoFire(CustomerDatabaseRef);
                    geofire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    CustomerPosition = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiButton.setText("Поиск водителя...");
                    getNearbyDrivers();
                }


            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference().child("Customers").child("Orders");
        final MarkerOptions[] mop = new MarkerOptions[1];
        productsRef.child(customerID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("lat2")) {

                    Double latitu = (Double) dataSnapshot.child("lat").getValue();
                    Double longitu = (Double) dataSnapshot.child("lon").getValue();
                    Double latitu2 = (Double) dataSnapshot.child("lat2").getValue();
                    Double longitu2 = (Double) dataSnapshot.child("lon2").getValue();

//                    fullMapOpen.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            String map = "kakaomap://route?sp=" + latitu + " ," + longitu + "&ep=" + latitu2 + " ," + longitu2 + "&by= CAR";
//                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(map));
//                            startActivity(intent);
//                        }
//                    });
                    LatLng trainLocation = new LatLng(latitu, longitu);

                    mop[0] = new MarkerOptions();
                    mop[0].position(trainLocation);
                    mop[0].title("Забрать тут");
                    mMap.addMarker(mop[0]);

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(trainLocation));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(trainLocation, 15f));

                    double distance;
                    Location locationA = new Location("");
                    locationA.setLatitude(latitu);
                    locationA.setLongitude(longitu);
                    Location locationB = new Location("");
                    locationB.setLatitude(latitu2);
                    locationB.setLongitude(longitu2);
                    distance = locationA.distanceTo(locationB) / 1000;
                    int km = (int)Math.floor(distance);
                    int price = km*17;
                    txtDistance.setVisibility(View.VISIBLE);
                    txtDistance.setText(("Стоимость поездки " + price + " р."));

                }
            }



            @Override
            public void onCancelled (DatabaseError databaseError){
            }

        });


    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
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
    public void onLocationChanged(Location location)
    {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }
    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void LogoutCustomer()
    {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(DriversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPosition.latitude, CustomerPosition.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType)
                {
                    driverFound = true;
                    final String driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);

                    DriverLocationRefListener = DriversLocationRef.child(driverFoundID).child("l").
                            addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists() && requestType)
                                    {
                                        List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                                        double locationLat = 0;
                                        double locationLng = 0;

                                        callTaxiButton.setText("Водитель найден");

                                        relativeLayout.setVisibility(View.VISIBLE);
                                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                                                .child("Users").child("Drivers").child(driverFoundID);

                                        reference.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                                                {
                                                    String name = dataSnapshot.child("name").getValue().toString();
                                                    final String phone  = dataSnapshot.child("phone").getValue().toString();
                                                    String carname = dataSnapshot.child("carname").getValue().toString();



                                                    txtName.setText(name);
                                                    txtPhone.setText(phone);
                                                    txtCarName.setText(carname);

                                                    callDriver.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            int permissionCheck = ContextCompat.checkSelfPermission(CustomersMapActivity.this, Manifest.permission.CALL_PHONE);

                                                            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                                                                ActivityCompat.requestPermissions(
                                                                        CustomersMapActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 123
                                                                );
                                                            }
                                                            else{
                                                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + phone));
                                                                startActivity(intent);
                                                            }
                                                        }
                                                    });

                                                    if (dataSnapshot.hasChild("image")) {
                                                        String image = dataSnapshot.child("image").getValue().toString();
                                                        Picasso.get().load(image).into(driverPhoto);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        if (driverLocationMap.get(0) != null)
                                        {
                                            locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                                        }
                                        if (driverLocationMap.get(1) != null)
                                        {
                                            locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                                        }
                                        LatLng DriverLatLng = new LatLng(locationLat, locationLng);

                                        if(driverMarker !=null)
                                        {
                                            driverMarker.remove();
                                        }

                                        Location location1 = new Location("");
                                        location1.setLatitude(CustomerPosition.latitude);
                                        location1.setLongitude(CustomerPosition.longitude);

                                        Location location2 = new Location("");
                                        location2.setLatitude(DriverLatLng.latitude);
                                        location2.setLongitude(DriverLatLng.longitude);

                                        float Distance = location1.distanceTo(location2);
                                        if(Distance>2000)
                                        {
                                            callTaxiButton.setText("Ваше такси подъезжает");
                                        }
                                        else {
                                            callTaxiButton.setText("Расстояние до такси " + String.valueOf(Distance));
                                        }

                                        driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                                .title("Ваше такси тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius = radius + 1;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


}