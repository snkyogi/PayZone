package com.example.spraycode;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.maps.model.LatLng;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class EmployeeViewContract extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String contractId;
    private TextView contractorEmailTextView;
    private TextView publiserEmailTextView;
    private TextView payRateTextView;
    private TextView payoutIntervalTextView;
    private TextView contractPeriodTextView;
    private TextView shiftWindowTextView;
    private TextView statusTextView;
    private SupportMapFragment mapFragment;

    private Button acceptButton;
    private Button rejectButton;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_employee_view_contract);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.employee_view_contract_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("contract_id")) {
            contractId = intent.getStringExtra("contract_id").replace("Contract_", "");
        }
        fetchContracts();

        // Initialize views
        publiserEmailTextView = findViewById(R.id.publisher_email);
        contractorEmailTextView = findViewById(R.id.contractor_email);
        payRateTextView = findViewById(R.id.pay_rate);
        payoutIntervalTextView = findViewById(R.id.payout_interval);
        contractPeriodTextView = findViewById(R.id.contract_period);
        shiftWindowTextView = findViewById(R.id.shift_window);
        statusTextView = findViewById(R.id.current_status);

        acceptButton = findViewById(R.id.accept_button);
        rejectButton = findViewById(R.id.reject_button);

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accept_contract(contractId);
            }
        });

        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reject_contract(contractId);
            }
        });



        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.employer_view_contract_map);
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

    }

    private void reject_contract(String contractId) {

        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("contract_id", contractId)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/reject_contract_id")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeViewContract.this, "Failed to Reject contract", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeeViewContract.this, "Rejected contract", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmployeeViewContract.this, InboundContracts.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EmployeeViewContract.this, "Failed to Reject contract", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

    }

    private void accept_contract(String contractId) {

        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("contract_id", contractId)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/accept_contract_id")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeViewContract.this, "Failed to accept contract", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeeViewContract.this, "Accepted contract", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EmployeeViewContract.this, InboundContracts.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EmployeeViewContract.this, "Failed to accept contract", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
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

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_loc)).draggable(false).title("You are here"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));

                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void fetchContracts() {
        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("contract_id", contractId)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/contract_info")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeViewContract.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String jsonResponse = responseBody.string();
                            Log.d("MY_LOG",jsonResponse);
                            Type contractListType = new TypeToken<ArrayList<List>>(){}.getType();
                            List<List> data  = new Gson().fromJson(jsonResponse, contractListType);

                            List<Contract> contracts = new ArrayList<>();
                            for (List<Object> item : data) {

                                int id = ((Double) item.get(0)).intValue();
                                String publisherEmail = (String) item.get(1);
                                String contractorEmail = (String) item.get(2);
//                                String timestamp = (String) item.get(3);
                                String status = (String) item.get(4);
//                                String description = (String) item.get(5);
                                double amount = ((Double) item.get(6));
                                String startDate = (String) item.get(7);
                                String endDate = (String) item.get(8);
                                String startTime = (String) item.get(9);
                                String endTime = (String) item.get(10);
                                String interval = (String) item.get(11);
                                contracts.add(new Contract("Contract_"+id, contractorEmail, amount+"("+interval+")", endDate, status, "publisher"));

                                runOnUiThread(() -> {
                                    publiserEmailTextView.setText(publiserEmailTextView.getText()+" "+publisherEmail);
                                    contractorEmailTextView.setText(contractorEmailTextView.getText()+" "+contractorEmail);
                                    payRateTextView.setText(payRateTextView.getText()+" "+amount+" £");
                                    contractPeriodTextView.setText(contractPeriodTextView.getText()+" "+startDate+" to "+endDate);
                                    payoutIntervalTextView.setText(payoutIntervalTextView.getText()+" "+interval);
                                    shiftWindowTextView.setText(shiftWindowTextView.getText()+" "+startTime+" to "+endTime);
                                    statusTextView.setText(statusTextView.getText()+" "+status);
                                });
                            }
                            fetchZones();
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EmployeeViewContract.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchZones() {
        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("contract_id", contractId)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/zone_info_id")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(EmployeeViewContract.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
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

                                int CONTRACT_ID = ((Double) item.get(1)).intValue();
                                double LATITUDE = ((Double) item.get(2));
                                double LONGITUDE = ((Double) item.get(3));
                                int MARKER_IDX = ((Double) item.get(4)).intValue();
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
                            });
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(EmployeeViewContract.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
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
}