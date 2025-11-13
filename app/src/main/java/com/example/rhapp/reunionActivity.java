package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class reunionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reunion);

        // ******************************* Passer d'une fenetre a l'autre ************************************

        LinearLayout accueil = findViewById(R.id.accueil);
        LinearLayout employes = findViewById(R.id.employes);
        LinearLayout conge = findViewById(R.id.conge);
        LinearLayout profil = findViewById(R.id.profil);

        accueil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, AcceuilRhActivity.class));

            }
        });
        employes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, employeActivity.class));

            }
        });
        conge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, CongesActivity.class));

            }
        });
        profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, ProfileActivity.class));

            }
        });

        // ********************************** Planifier un reunion ********************

        Button btnPlanifierReunion = findViewById(R.id.btnPlanifierReunion);

        btnPlanifierReunion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, PlanifierReunionActivity.class));
            }
        });

        // ********************************** Notifications ********************

        RelativeLayout notifications = findViewById(R.id.notifications);

        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, NotificationsRhActivity.class));
            }
        });

        // ***************************************************************



    }
}