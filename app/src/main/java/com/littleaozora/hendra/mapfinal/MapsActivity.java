package com.littleaozora.hendra.mapfinal;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.littleaozora.hendra.mapfinal.models.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MapsActivity";
    private static final int PLACE_PICKER_REQUEST = 1;
    private GoogleMap mMap;
    private PlaceAutocompleteAdapter mAdapter;
    protected GeoDataClient mGeoDataClient;
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    //Var get latitude longitude
    private FusedLocationProviderApi locationProviderApi = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient,mGoogleApiClient;
    private LocationRequest locationRequest;
    private Double myLatitude;
    private Double myLongitude;
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;
    private static final int MY_PERMISSION_REQUEST_COARSE_LOCATION = 102;
    private boolean permissionIsGranted = false;
    private boolean firstDisplay = true;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private PlaceInfo mPlace;
    private Marker mMarker;

    //widget
    private AutoCompleteTextView mSearchText;
    private ImageView mGps, mInfo, mPlacePicker;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mPlacePicker = (ImageView) findViewById(R.id.place_picker);

        //get latidute longitude
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5 * 1000);
        locationRequest.setFastestInterval(3 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();



    }


    private void init(){
        Log.d("MapsActivity", "Init: Inisializing");
        // Construct a GeoDataClient for the Google Places API for Android.
        /*
        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        */

        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mAdapter = new PlaceAutocompleteAdapter(this,
                Places.getGeoDataClient(this, null),
                LAT_LNG_BOUNDS,
                null);
        mSearchText.setAdapter(mAdapter);


        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == keyEvent.KEYCODE_ENTER){
                    geoLocate();
                }
                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onclick gps icon");
                LatLng sydney = new LatLng(myLatitude, myLongitude);
                moveCamera(sydney, 15.0f, "My Location");
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "OnClick : Clicked place info");
                try {
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        Log.d(TAG, "onClick : place info"+mPlace.toString());
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.e(TAG, "onClick "+e.getMessage());
                }
            }
        });

        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MapsActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e(TAG,"GooglePlayServicesRepairableException"+e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG,"GooglePlayServicesNotAvailableException"+e.getMessage());
                }
            }
        });



        hideSoftKeyboard();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(googleApiClient, place.getId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        }
    }

    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){
        Log.d(TAG, "move Camera: moving the camera to lat:"+latLng.latitude+", long:"+latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        mMap.clear();

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapsActivity.this));

        if(placeInfo != null){
            try {
                String snippet = "Address: "+placeInfo.getAddress()+"\n"+
                        "Phone Number: "+placeInfo.getPhoneNumber()+"\n"+
                        "Website: "+placeInfo.getWebsiteUri()+"\n"+
                        "Price Rating: "+placeInfo.getRating()+"\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);

                mMarker = mMap.addMarker(options);

            }catch(NullPointerException e){
                Log.e(TAG, "move Camera: "+e.getMessage());
            }
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }

        hideSoftKeyboard();
    }

    private void moveCamera(LatLng latLng, float zoom, String tittle){
        Log.d(TAG, "move Camera: moving the camera to lat:"+latLng.latitude+", long:"+latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        if(tittle != "My Location") {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(tittle);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();
    }

    private void geoLocate(){
        Log.d("MapsActivity", "geoLocate");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e("MapsActivity", "IoException : "+e.getMessage());
        }

        if(list.size() > 0){
            Address address = list.get(0);
            Log.d("MapsActivity", "geo found a location: "+address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), 15.0f, address.getAddressLine(0));
        }
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


        Log.d("MapsActivity:", "" + myLatitude + ", " + myLongitude);
        if (myLatitude == null && myLongitude == null) {
            myLatitude = 65.9667;
            myLongitude = -18.5333;
        }
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(myLatitude, myLongitude);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        init();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATION);
            }
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
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();

        Log.d("MapsActivity:", "Latitude=" + myLatitude + ", Longitude=" + myLongitude);
        // Add a marker in Sydney and move the camera

        if (myLatitude != null && myLongitude != null) {
            LatLng sydney = new LatLng(myLatitude, myLongitude);

            if (firstDisplay) {
                //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.0f));
                moveCamera(sydney, 15.0f, "My Location");
                firstDisplay = false;
                init();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(permissionIsGranted) {
            if (googleApiClient.isConnected()) {
                requestLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(permissionIsGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(permissionIsGranted) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case MY_PERMISSION_REQUEST_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    permissionIsGranted = true;
                }else{
                    permissionIsGranted = false;
                    Toast.makeText(getApplicationContext(), "This app request location permission to be granted", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSION_REQUEST_COARSE_LOCATION: break;
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
    ---------------------------------- google places API autocomplete suggestion -------------------------
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            /*
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .build();
            */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(googleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place query did not complete succesfully: "+places.getStatus().toString());
                places.release();
                return;
            }

            final Place place = places.get(0);

            try {
                mPlace = new PlaceInfo();
                if(place.getAttributions() != null) {
                    mPlace.setAttributions(place.getAttributions().toString());
                }
                if(place.getViewport() != null){
                    mPlace.setViewPort(place.getViewport().toString());
                }
                if(place.getPhoneNumber() != null){
                    mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                }
                if(place.getWebsiteUri() != null){
                    mPlace.setWebsiteUri(place.getWebsiteUri());
                }
                if(place.getId() != null){
                    mPlace.setId(place.getId().toString());
                }
                if(place.getAddress() != null){
                    mPlace.setAddress(place.getAddress().toString());
                }
                if(place.getLatLng() != null){
                    mPlace.setLatLng(place.getLatLng());
                }
                if(place.getLocale() != null){
                    mPlace.setLocal(place.getLocale().toString());
                }
                if(place.getName() != null){
                    mPlace.setName(place.getName().toString());
                }
                if(place.getPlaceTypes() != null){
                    mPlace.setPlaceType(place.getPlaceTypes().toString());
                }

                //mPlace.setPriceLevel(place.getPriceLevel());
                //mPlace.setRating(place.getRating());

                Log.d(TAG, "onResult Places:"+mPlace.toString());
            }catch (NullPointerException e){
                Log.e(TAG, "onResult : NullPointerException: "+e.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude), 15.0f, mPlace);
            /*
            Log.d(TAG, "onResult: place detail getAttributions: "+place.getAttributions());
            Log.d(TAG, "onResult: place detail getViewport: "+place.getViewport());
            Log.d(TAG, "onResult: place detail getPhoneNumber: "+place.getPhoneNumber());
            Log.d(TAG, "onResult: place detail getWebsiteUri: "+place.getWebsiteUri());
            Log.d(TAG, "onResult: place detail getId: "+place.getId());
            Log.d(TAG, "onResult: place detail getAddress: "+place.getAddress());
            Log.d(TAG, "onResult: place detail getLatLng: "+place.getLatLng());
            Log.d(TAG, "onResult: place detail getLocale: "+place.getLocale());
            Log.d(TAG, "onResult: place detail getName: "+place.getName());
            Log.d(TAG, "onResult: place detail getPlaceTypes: "+place.getPlaceTypes());
            //Log.d(TAG, "onResult: place detail getPriceLevel: "+place.getPriceLevel());
            Log.d(TAG, "onResult: place detail getRating: "+place.getRating());
            */


            places.release();
        }
    };
}
