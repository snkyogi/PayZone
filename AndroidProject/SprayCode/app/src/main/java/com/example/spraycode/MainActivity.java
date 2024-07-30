package com.example.spraycode;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Use Handler to delay transition
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Create an intent to start the next activity
                Intent mainIntent = new Intent(MainActivity.this, LoginActivity.class);
                // Clear all previous activities from the stack
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
                // Finish the splash screen activity
                finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}