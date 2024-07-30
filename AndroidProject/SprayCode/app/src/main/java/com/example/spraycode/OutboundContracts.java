package com.example.spraycode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OutboundContracts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_outbound_contracts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView contractsRecyclerView = findViewById(R.id.contracts_recycler_view);
        // Sample data
//        List<Contract> contracts = new ArrayList<>();
//        contracts.add(new Contract("Contract 1", "sam","100.0", "01/01/2024", "Closed"));
//        contracts.add(new Contract("Contract 2", "murphy","200.0", "01/02/2024", "Declined"));
//        contracts.add(new Contract("Contract 3", "ben","300.0", "01/08/2024", "Active"));

        contractsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        contractsRecyclerView.setAdapter(new ContractsAdapter(contracts));
        fetchContracts(contractsRecyclerView);

        Button create_contract_button = findViewById(R.id.create_contract_button);
        create_contract_button.setOnClickListener(v -> {
            Intent intent = new Intent(OutboundContracts.this, CreateContract.class);
            startActivity(intent);
        });

    }
    private void fetchContracts(RecyclerView recyclerView) {
        OkHttpClient client = new OkHttpClient();
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);
        FormBody formBody = new FormBody.Builder()
                .add("email", issuer_email)
                .build();
        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/outbound_contracts_info")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(OutboundContracts.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
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
                                String contractorEmail = (String) item.get(2);
//                                String timestamp = (String) item.get(3);
                                String status = (String) item.get(4);
//                                String description = (String) item.get(5);
                                double amount = ((Double) item.get(6));
//                                String startDate = (String) item.get(7);
                                String endDate = (String) item.get(8);
//                                String startTime = (String) item.get(9);
//                                String endTime = (String) item.get(10);
                                String interval = (String) item.get(11);
                                contracts.add(new Contract("Contract_"+id, contractorEmail, amount+"("+interval+")", endDate, status, "publisher"));

                            runOnUiThread(() -> {
                                    recyclerView.setAdapter(new ContractsAdapter(contracts));
                                });
                            }
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(OutboundContracts.this, "Failed to fetch contracts", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, HomeScreen.class); // Replace TargetActivity with the activity you want to navigate to
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Optionally, you can finish the current activity
        super.onBackPressed();
    }
}