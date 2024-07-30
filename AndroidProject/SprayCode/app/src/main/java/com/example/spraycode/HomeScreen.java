package com.example.spraycode;

import android.Manifest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeScreen extends AppCompatActivity implements OnMapReadyCallback  {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_screen_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            getCurrentLocation();
        }

        Button outboundContractsButton = findViewById(R.id.outbound_contracts_button);
        outboundContractsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start ContractsListActivity
                Intent intent = new Intent(HomeScreen.this, OutboundContracts.class);
                startActivity(intent);
            }
        });

        Button inboundContractsButton = findViewById(R.id.inbound_contracts_button);
        inboundContractsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start ContractsListActivity
                Intent intent = new Intent(HomeScreen.this, InboundContracts.class);
                startActivity(intent);
            }
        });

        Button manage_bank_button = findViewById(R.id.manage_bank_button);
        manage_bank_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start ContractsListActivity
                Intent intent = new Intent(HomeScreen.this, ManageBankAccount.class);
                startActivity(intent);
            }
        });

        Button cash_flow_button = findViewById(R.id.cash_flow_button);
        cash_flow_button.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    // Create an Intent to start ContractsListActivity
                                                    Intent intent = new Intent(HomeScreen.this, CashFlow.class);
                                                    startActivity(intent);
                                                }
                                            });


//        Intent serviceIntent = new Intent(this, TrackingService.class);
//        stopService(serviceIntent);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_loc)).draggable(false).title("You are here"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));
                    fetchContractorZones();

                    Intent serviceIntent = new Intent(this, TrackingService.class);
                    startService(serviceIntent);

                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getCurrentLocation();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void fetchContractorZones() {
        OkHttpClient client = new OkHttpClient();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);

        FormBody formBody = new FormBody.Builder()
                .add("email", issuer_email)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/zone_info_email_contractor")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeScreen.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String jsonResponse = responseBody.string();
                            Log.d("MY_LOGGY",jsonResponse);
                            Type contractListType = new TypeToken<ArrayList<List>>(){}.getType();
                            List<List> data  = new Gson().fromJson(jsonResponse, contractListType);
                            Map<Integer, List<LatLng>> polygonPointsMap = new HashMap<>();

                            for (List<Object> item : data) {

                                int CONTRACT_ID = ((Double) item.get(0)).intValue();
                                double LATITUDE = ((Double) item.get(1));
                                double LONGITUDE = ((Double) item.get(2));
                                int MARKER_IDX = ((Double) item.get(3)).intValue();
                                Log.d("MY_LOG",CONTRACT_ID+" %%%% "+LATITUDE+" "+LONGITUDE+" %%%% "+MARKER_IDX);

                                LatLng point = new LatLng(LATITUDE, LONGITUDE);

                                // If the list of points for this polygon ID doesn't exist, create it
                                if (!polygonPointsMap.containsKey(CONTRACT_ID)) {
                                    polygonPointsMap.put(CONTRACT_ID, new ArrayList<>());
                                }
                                polygonPointsMap.get(CONTRACT_ID).add(point);

                            }
                            runOnUiThread(() -> {
                                for (Map.Entry<Integer, List<LatLng>> entry : polygonPointsMap.entrySet()) {
                                    List<LatLng> points = entry.getValue();
                                    if (points.size() > 2) { // A polygon needs at least 3 points
                                        mMap.addPolygon(new PolygonOptions()
                                                .addAll(points)
                                                .strokeColor(Color.GREEN) // Polygon border color
                                                .fillColor(0x3000ff00) // Polygon fill color (semi-transparent red)
                                                .strokeWidth(5f)); // Border width
                                        LatLng center = getCenterPoint(points);
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 18f));

                                    }
                                }
                                fetchIssuerZones();
                            });
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeScreen.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    public LatLng getCenterPoint(List<LatLng> points) {
        if (points == null || points.isEmpty()) {
            return null; // Return null or handle empty list case as needed
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;

        for (LatLng point : points) {
            double lat = point.latitude;
            double lng = point.longitude;

            if (lat < minLat) {
                minLat = lat;
            }
            if (lat > maxLat) {
                maxLat = lat;
            }
            if (lng < minLng) {
                minLng = lng;
            }
            if (lng > maxLng) {
                maxLng = lng;
            }
        }

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLng = (minLng + maxLng) / 2.0;

        return new LatLng(centerLat, centerLng);
    }

    private void fetchIssuerZones() {
        OkHttpClient client = new OkHttpClient();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);

        FormBody formBody = new FormBody.Builder()
                .add("email", issuer_email)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/zone_info_email_issuer")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeScreen.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String jsonResponse = responseBody.string();
                            Log.d("MY_LOGGY",jsonResponse);
                            Type contractListType = new TypeToken<ArrayList<List>>(){}.getType();
                            List<List> data  = new Gson().fromJson(jsonResponse, contractListType);
                            Map<Integer, List<LatLng>> polygonPointsMap = new HashMap<>();

                            for (List<Object> item : data) {

                                int CONTRACT_ID = ((Double) item.get(0)).intValue();
                                double LATITUDE = ((Double) item.get(1));
                                double LONGITUDE = ((Double) item.get(2));
                                int MARKER_IDX = ((Double) item.get(3)).intValue();
                                Log.d("MY_LOG",CONTRACT_ID+" %%%% "+LATITUDE+" "+LONGITUDE+" %%%% "+MARKER_IDX);

                                LatLng point = new LatLng(LATITUDE, LONGITUDE);

                                // If the list of points for this polygon ID doesn't exist, create it
                                if (!polygonPointsMap.containsKey(CONTRACT_ID)) {
                                    polygonPointsMap.put(CONTRACT_ID, new ArrayList<>());
                                }
                                polygonPointsMap.get(CONTRACT_ID).add(point);

                            }
                            runOnUiThread(() -> {
                                for (Map.Entry<Integer, List<LatLng>> entry : polygonPointsMap.entrySet()) {
                                    List<LatLng> points = entry.getValue();
                                    if (points.size() > 2) { // A polygon needs at least 3 points
                                        mMap.addPolygon(new PolygonOptions()
                                                .addAll(points)
                                                .strokeColor(Color.GREEN) // Polygon border color
                                                .fillColor(0x30ff0000) // Polygon fill color (semi-transparent red)
                                                .strokeWidth(5f)); // Border width
                                        LatLng center = getCenterPoint(points);
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 18f));

                                    }
                                }
                            });
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeScreen.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

}