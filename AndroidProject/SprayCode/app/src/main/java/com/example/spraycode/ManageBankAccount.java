package com.example.spraycode;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.lang.reflect.Type;
import java.util.List;

public class ManageBankAccount extends AppCompatActivity {

    private String API_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_bank_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.banking_main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        API_URL = getString(R.string.url_base) +"/api/account_info";

        ListView listView = findViewById(R.id.accounts_list);
        new FetchAccountsTask(listView).execute(API_URL);
    }

    private class FetchAccountsTask extends AsyncTask<String, Void, List<Account>> {
        private final ListView listView;
        private final OkHttpClient client = new OkHttpClient();

        FetchAccountsTask(ListView listView) {
            this.listView = listView;
        }

        @Override
        protected List<Account> doInBackground(String... urls) {
            String urlString = urls[0];
            try {
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE);
                String bank_uid = sharedPreferences.getString("bank_uid", null);
                FormBody formBody = new FormBody.Builder()
                        .add("user_identification_number", bank_uid)
                        .build();
                Request request = new Request.Builder()
                        .url(urlString)
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        Log.d("MY_TAG", "Response JSON: " + json);
                        Gson gson = new Gson();
                        Type accountListType = new TypeToken<List<Account>>() {}.getType();
                        return gson.fromJson(json, accountListType);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Account> accounts) {
            if (accounts != null) {
                AccountAdapter adapter = new AccountAdapter(ManageBankAccount.this, accounts);
                listView.setAdapter(adapter);
            }
        }
    }
}