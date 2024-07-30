package com.example.spraycode;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccountAdapter extends ArrayAdapter<Account> {
    private final Context context;
    private final List<Account> accounts;
    private final SharedPreferences preferences;
    private final int GREEN_COLOR = Color.rgb(29, 120, 24);
    private String defaultAccountNumber = "";

    public AccountAdapter(Context context, List<Account> accounts) {
        super(context, R.layout.list_item_account, accounts);
        this.context = context;
        this.accounts = accounts;
        this.preferences = context.getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        this.defaultAccountNumber = preferences.getString("default_account_number", "");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_account, parent, false);
        }

        Account account = accounts.get(position);

        TextView accountNameTextView = convertView.findViewById(R.id.account_name);
        TextView accountNumberTextView = convertView.findViewById(R.id.account_num);
        TextView accountTypeTextView = convertView.findViewById(R.id.account_type);
        TextView accountSubTypeTextView = convertView.findViewById(R.id.account_sub_type);

        accountNameTextView.setText(account.getName());
        accountNumberTextView.setText(account.getAccountNum());
        accountTypeTextView.setText(account.getAccountType());
        accountSubTypeTextView.setText(account.getAccountSubType());

        // Set background color based on default status
        if (account.getAccountNum().equals(defaultAccountNumber)) {
            convertView.setBackgroundColor(GREEN_COLOR);
            register_account(account);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        convertView.setOnClickListener(v -> {
            // Handle item click to toggle default status
            if (account.getAccountNum().equals(defaultAccountNumber)) {
                // If the clicked item is already the default, clear the default
                clearDefaultAccount();
            } else {
                // Otherwise, set this item as the new default
                setDefaultAccount(account);
            }
            // Notify the adapter that the data has changed
            notifyDataSetChanged();
        });

        return convertView;
    }

    private void register_account(Account account) {

        OkHttpClient client = new OkHttpClient();

        SharedPreferences sharedPreferences = context.getSharedPreferences("LoginPreferences", MODE_PRIVATE);
        String issuer_email = sharedPreferences.getString("email", null);
        String bank_uid = sharedPreferences.getString("bank_uid", null);

        FormBody formBody = new FormBody.Builder()
                .add("email", issuer_email)
                .add("user_identification_number", bank_uid)
                .add("account_id", account.getAccountId())
                .add("account_name", account.getName())
                .add("account_number", account.getAccountNum())
                .add("account_type", account.getAccountType())
                .add("account_sub_type", account.getAccountSubType())
                .build();
        Request request = new Request.Builder()
                .url(context.getString(R.string.url_base) + "/api/set_primary_account")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("MY_LOG","Failed Account Update");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("MY_LOG","Failed");
                } else {
                    Log.d("MY_LOG","Failed");
                }
            }
        });
    }

    private void setDefaultAccount(Account account) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("default_account_id", account.getAccountId());
        editor.putString("default_account_name", account.getName());
        editor.putString("default_account_type", account.getAccountType());
        editor.putString("default_account_sub_type", account.getAccountSubType());
        editor.putString("default_account_number", account.getAccountNum());
        editor.apply();
        defaultAccountNumber = account.getAccountNum(); // Update local reference
    }

    private void clearDefaultAccount() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("default_account_id");
        editor.remove("default_account_name");
        editor.remove("default_account_type");
        editor.remove("default_account_sub_type");
        editor.remove("default_account_number");
        editor.apply();
        defaultAccountNumber = ""; // Clear local reference
    }
}
