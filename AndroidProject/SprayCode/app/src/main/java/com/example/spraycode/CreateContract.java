package com.example.spraycode;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateContract extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private int markerIndexCounter = 1;
    private GoogleMap mMap;
    private List<Marker> markers = new ArrayList<>();
    private Polygon polygon;
    private FusedLocationProviderClient fusedLocationClient;

    private EditText fromDateInput;
    private EditText toDateInput;
    private EditText currentEditText;

    private EditText editTextStartTime;
    private EditText editTextEndTime;
    private EditText currentTimeEditText;
    MarkerOptions userLocationMarker;

    private EditText contractorEmailEditText;
    private EditText payRateEditText;
    private RadioGroup payoutIntervalGroup;
    private EditText fromDateEditText;
    private EditText toDateEditText;
    private EditText fromTimeEditText;
    private EditText toTimeEditText;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_contract);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_contract_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        contractorEmailEditText = findViewById(R.id.contractor_email);
        payRateEditText = findViewById(R.id.pay_rate);
        payoutIntervalGroup = findViewById(R.id.payout_interval_group);
        fromDateEditText = findViewById(R.id.from_date_input);
        toDateEditText = findViewById(R.id.to_date_input);
        fromTimeEditText = findViewById(R.id.from_time_input);
        toTimeEditText = findViewById(R.id.to_time_input);

        fromDateInput = findViewById(R.id.from_date_input);
        toDateInput = findViewById(R.id.to_date_input);

        editTextStartTime = findViewById(R.id.from_time_input);
        editTextEndTime = findViewById(R.id.to_time_input);

        client = new OkHttpClient();

        fromDateInput.setOnClickListener(v -> {
            currentEditText = fromDateInput;
            showDatePickerDialog();
        });
        toDateInput.setOnClickListener(v -> {
            currentEditText = toDateInput;
            showDatePickerDialog();
        });

        editTextStartTime.setOnClickListener(v -> {
            currentTimeEditText = editTextStartTime;
            showTimePickerDialog();
        });

        editTextEndTime.setOnClickListener(v -> {
            currentTimeEditText = editTextEndTime;
            showTimePickerDialog();
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.workzone_selection_map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted
            getCurrentLocation();
        }

        Button submitContractButton = findViewById(R.id.submit_button);
        submitContractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start ContractsListActivity
//                Intent intent = new Intent(CreateContract.this, OutboundContracts.class);
//                startActivity(intent);
                registerContract();
            }
        });



    }

    private void registerContract() {
        // Extract values from the form
        String contractorEmail = contractorEmailEditText.getText().toString();
        String payRate = payRateEditText.getText().toString();

        // Get selected payout interval
        int selectedId = payoutIntervalGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);
        String payoutInterval = selectedRadioButton != null ? selectedRadioButton.getText().toString() : "";

        String fromDate = fromDateEditText.getText().toString();
        String toDate = toDateEditText.getText().toString();
        String fromTime = fromTimeEditText.getText().toString();
        String toTime = toTimeEditText.getText().toString();

        // Get marker positions from the map
        JSONArray markersArray = new JSONArray();
        for (Marker marker : markers) {
            JSONObject markerObject = new JSONObject();
            try {
                markerObject.put("latitude", marker.getPosition().latitude);
                markerObject.put("longitude", marker.getPosition().longitude);
                markerObject.put("marker_idx", marker.getTag()); // Retrieve marker index from tag
                markersArray.put(markerObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);

        // Create JSON object
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("contractor_email", contractorEmail);
            jsonObject.put("issuer_email", issuer_email);
            jsonObject.put("pay_rate", payRate);
            jsonObject.put("payout_interval", payoutInterval);
            jsonObject.put("from_date", fromDate);
            jsonObject.put("to_date", toDate);
            jsonObject.put("from_time", fromTime);
            jsonObject.put("to_time", toTime);
            jsonObject.put("markers", markersArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Convert JSON object to string
        String jsonString = jsonObject.toString();

        // Create OkHttp request
        RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(getString(R.string.url_base)+"/api/add_contract")
                .post(requestBody)
                .build();

        // Execute request
//        client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace(); // Handle error
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    final String responseData = response.body().string();
                    runOnUiThread(() -> {
                        Toast.makeText(CreateContract.this, "Contract Published", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CreateContract.this, OutboundContracts.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateContract.this, "Failed to publish contract", Toast.LENGTH_SHORT).show();
                    });
                    // Handle unsuccessful response
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Add a default marker and move the camera
        LatLng defaultLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
        mMap.setOnMapClickListener(this::addMarker);
        mMap.setOnMarkerClickListener(this::onMarkerClick);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // Optionally handle marker drag start
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Optionally handle marker drag event
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // Update the polygon when marker is dragged
                updatePolygon();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Log.d("MYLocation", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    MarkerOptions user_loc_marker = new MarkerOptions().position(currentLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_loc)).draggable(false).title("You are here");
                    userLocationMarker = user_loc_marker;
                    mMap.addMarker(user_loc_marker);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));

                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showDatePickerDialog() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        // Format the selected date and set it to the current EditText
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        if (currentEditText != null) {
                            currentEditText.setText(selectedDate);
                        }
                    }
                }, year, month, day);

        // Show the DatePickerDialog
        datePickerDialog.show();
    }
    private void showTimePickerDialog() {
        // Get current time
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Format the selected time and set it to the current EditText
                        String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                        currentTimeEditText.setText(selectedTime);
                    }
                }, hour, minute, true); // true for 24-hour format

        // Show the TimePickerDialog
        timePickerDialog.show();
    }

    private void addMarker(LatLng latLng) {
        // Add a new marker
        Marker marker = mMap.addMarker(new MarkerOptions().draggable(true).position(latLng));
        if (marker != null) {
            marker.setTag(markerIndexCounter); // Use tag to store index
            markers.add(marker);
            markerIndexCounter++; // Increment counter
        }
        updatePolygon();
    }
    private boolean onMarkerClick(Marker marker) {
        if(marker.getPosition().toString().equals(userLocationMarker.getPosition().toString())){
            Log.d("Matching!!!","Matching!!!!!!!");
            return false;
        }
        // Remove the marker on long press
        marker.remove();
        markers.remove(marker);
        updatePolygon();
        return true; // Return true to indicate that the click event was handled
    }

    private void updatePolygon() {
        // Remove the existing polygon if it exists
        if (polygon != null) {
            polygon.remove();
        }

        List<LatLng> polygonPoints = new ArrayList<>();
        for (Marker marker : markers) {
            polygonPoints.add(marker.getPosition());
        }
        // Draw a new polygon with the updated points
        if (polygonPoints.size() > 2) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(polygonPoints)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(50, 255, 0, 0)); // Semi-transparent red fill
            polygon = mMap.addPolygon(polygonOptions);
        }
    }
}