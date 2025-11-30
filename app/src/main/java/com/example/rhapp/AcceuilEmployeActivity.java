package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AcceuilEmployeActivity extends AppCompatActivity {
    private LinearLayout presencefooter, congesfooter, reunionsfooter, profilefooter, attestation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_employe);

        presencefooter = findViewById(R.id.presencefooter);
        congesfooter = findViewById(R.id.congesfooter);
        reunionsfooter = findViewById(R.id.reunionsfooter);
        profilefooter = findViewById(R.id.profilefooter);
        attestation = findViewById(R.id.attestation);

        // Navigation vers Presence
        if (presencefooter != null) {
            presencefooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, PresenceActivity.class));
            });
        }

        // Navigation vers Congés
        if (congesfooter != null) {
            congesfooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, CongesEmploye.class));
            });
        }

        // Navigation vers Réunions
        if (reunionsfooter != null) {
            reunionsfooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ReunionEmployeActivity.class));
            });
        }

        // Navigation vers Profil
        if (profilefooter != null) {
            profilefooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ProfileEmployeActivity.class));
            });
        }

        // Navigation vers Attestations
        if (attestation != null) {
            attestation.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, AttestationEmployeActivity.class));
            });
        }
    }
}