package com.example.spraycode;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CashFlow extends AppCompatActivity {

    private TextView accountDetailsTextView;
    private ListView transactionsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cash_flow);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cashFlowLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        accountDetailsTextView = findViewById(R.id.accountDetailsTextView);
        transactionsListView = findViewById(R.id.transactionsListView);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);

        String userEmail = sharedPreferences.getString("email", null);


        fetchUserDetails(userEmail);
    }

    private void fetchUserDetails(String email) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("email", email)
                .build();

        Request request = new Request.Builder()
                .url(getString(R.string.url_base) + "/api/user_details")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle failure
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("CashFlow", "Response: " + responseData);
                    runOnUiThread(() -> {
                        // Parse JSON data
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            JSONArray accountDetailsArray = jsonResponse.getJSONArray("account_details");
                            JSONArray transactionsArray = jsonResponse.getJSONArray("transactions");
                            String my_account_number = "";
                            // Update account details
                            JSONArray accountDetails = accountDetailsArray.getJSONArray(0);
                            my_account_number = accountDetails.getString(3);

                            double transactionsTotal = 0.0;

                            // Update transactions list view
                            List<String> transactionsList = new ArrayList<>();
                            for (int i = 0; i < transactionsArray.length(); i++) {
                                JSONArray transaction = transactionsArray.getJSONArray(i);
                                String transactionInfo = "Amount: " + String.format("%.2f", transaction.getDouble(3)) + " £\n";

                                if (my_account_number.equals(transaction.getString(1))) {
                                    transactionInfo = transactionInfo+"Paid to: "+transaction.getString(2);
                                    transactionsTotal = transactionsTotal - transaction.getDouble(3);
                                } else {
                                    transactionInfo = transactionInfo+"Received from: "+transaction.getString(1);
                                    transactionsTotal = transactionsTotal + transaction.getDouble(3);
                                }
                                transactionInfo = transactionInfo+ "\n" //+"Transaction ID: " + transaction.getString(0) + "\n"
                                        + "Contract ID: " + transaction.getString(4) + "\n"
                                        + "Transaction Time: " + transaction.getString(5).substring(0, 19);

                                transactionsList.add(transactionInfo);
                            }


                            String accountInfo = "Account Number: " + accountDetails.getString(3) + "\n"
                                    + "Type: " + accountDetails.getString(4)+" - "+accountDetails.getString(5) + "\n"
                                    + "Account Balance: " + String.format("%.2f", (accountDetails.getDouble(6)+transactionsTotal))+" £";
                            accountDetailsTextView.setText(accountInfo);

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(CashFlow.this, android.R.layout.simple_list_item_1, transactionsList);
                            transactionsListView.setAdapter(adapter);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // Handle JSON parsing error
                        }
                    });
                }
            }
        });
    }
}