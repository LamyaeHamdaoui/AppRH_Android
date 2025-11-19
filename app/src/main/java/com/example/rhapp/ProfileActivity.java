package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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
        LinearLayout reunions_interface = findViewById(R.id.reunions_interface);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout conges_interface = findViewById(R.id.conges_interafce);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout employes_interface = findViewById(R.id.employes_interface);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout modifier_profil = findViewById(R.id.modifier_profil);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout security_interface = findViewById(R.id.security_interface);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        LinearLayout help_support = findViewById(R.id.help_support);
        @SuppressLint("WrongViewCast")
        Button sedeconnecter = findViewById(R.id.sedeconnecter);



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
        modifier_profil.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
        security_interface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, SecurityActivity.class);
                startActivity(intent);
            }
        });
        help_support.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, HelpSupportActivity.class);
                startActivity(intent);
            }
        });
         sedeconnecter.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}