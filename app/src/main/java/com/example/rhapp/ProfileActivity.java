package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.BreakIterator;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        LinearLayout reunions_interface = findViewById(R.id.reunions_interface);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout conges_interface = findViewById(R.id.conges_interafce);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout employes_interface = findViewById(R.id.employes_interface);


        reunions_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, reunionActivity.class);
                startActivity(intent);
            }
        });
        conges_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, CongesActivity.class);
                startActivity(intent);
            }
        });
        employes_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, employeActivity.class);
                startActivity(intent);
            }
        });

    }
}