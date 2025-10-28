package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button idrh=findViewById(R.id. id);
        idrh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Créer un intent explicite pour démarrer SecondActivity
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
            }
        });
    }
}