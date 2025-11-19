package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);


        LinearLayout reunions_interface = findViewById(R.id.footerReunions);
        LinearLayout conges_interface = findViewById(R.id.footerConges);
        LinearLayout employes_interface = findViewById(R.id.footerEmployes);
        LinearLayout acceuil_interface = findViewById(R.id.footerAccueil);


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
                Intent intent = new Intent(ProfileActivity.this, EmployeActivity.class);
                startActivity(intent);
            }
        });
        acceuil_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, AcceuilRhActivity.class);
                startActivity(intent);
            }
        });


    }
    }


