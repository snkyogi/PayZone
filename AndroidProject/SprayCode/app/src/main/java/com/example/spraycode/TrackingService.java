package com.example.spraycode;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
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

public class TrackingService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private static final long INTERVAL = 60*1000; // 1 minute
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("TrackingService", "Checking user status");
                checkStatus();
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(runnable);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    // Handle location updates here
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("latitude", String.valueOf(latitude));
                    editor.putString("longitude", String.valueOf(longitude));
                    editor.apply();
                }


            }
        };

        startLocationUpdates();
    }

    private void checkStatus() {
        Log.d("MY_SERVICE","SERVICE RUNNING IN BG!!!");
        getActiveZones();
    }

    private void getActiveZones() {
        OkHttpClient client = new OkHttpClient();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);
        String latitude = sharedPreferences.getString("latitude", null);
        String longitude = sharedPreferences.getString("longitude", null);

        if (issuer_email == null || latitude == null || longitude == null) {
            Log.d("MY_LOG","Failed NULL in preferences");
            return;
        }

        FormBody formBody = new FormBody.Builder()
                .add("email", issuer_email)
                .add("latitude", latitude)
                .add("longitude", longitude)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/zone_info_email_contractor_gps")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("MY_LOG","Failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String contract_ids = responseBody.string();
                            Log.d("MY_LOGGY_SERVICE",contract_ids);
                            update_work_log(contract_ids);
//                            Type contractListType = new TypeToken<ArrayList<List>>(){}.getType();
//                            List<List> data  = new Gson().fromJson(jsonResponse, contractListType);
//                            Log.d("MY_LOGGY_SERVICE","Came here");
                        }
                    }
                } else {
                    Log.d("MY_LOG","Failed");
                }
            }
        });
    }

    private void update_work_log(String contract_ids) {
        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("contract_ids", contract_ids)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/insert_work_minute_logs")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("MY_LOG","Failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String jsonResponse = responseBody.string();
                            Log.d("MY_LOGGY_SERVICE_WORK_LOG",jsonResponse);

                        }
                    }
                } else {
                    Log.d("MY_LOG","Failed");
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}