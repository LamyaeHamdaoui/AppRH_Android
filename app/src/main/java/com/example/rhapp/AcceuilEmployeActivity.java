package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AcceuilEmployeActivity extends AppCompatActivity {
    private LinearLayout attestation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_employe);

        attestation = findViewById(R.id.attestation);
        if (attestation != null) {
            attestation.setOnClickListener(v -> {
                // Ouvrir l'activité des attestations employé
                startActivity(new Intent(AcceuilEmployeActivity.this, AttestationEmployeActivity.class));
            });
        }
    }
}