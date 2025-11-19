package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AcceuilRhActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_rh);
        LinearLayout reunions_interface = findViewById(R.id.reunions_interface);
        LinearLayout conges_interface = findViewById(R.id.conges_interafce);
        LinearLayout employes_interface = findViewById(R.id.employes_interface);
         LinearLayout profile_interface = findViewById(R.id.profile_interface);


        reunions_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(AcceuilRhActivity.this, reunionActivity.class);
                startActivity(intent);
            }
        });
        conges_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(AcceuilRhActivity.this, CongesActivity.class);
                startActivity(intent);
            }
        });
        employes_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(AcceuilRhActivity.this, EmployeActivity.class);
                startActivity(intent);
            }
        });
        profile_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(AcceuilRhActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });


    }
}