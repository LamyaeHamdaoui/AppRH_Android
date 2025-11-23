package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class AcceuilEmployeActivity extends AppCompatActivity {
    private LinearLayout presencefooter, congesfooter, reunionsfooter, profilefooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_employe);

        presencefooter = findViewById(R.id.presencefooter);
        congesfooter = findViewById(R.id.congesfooter);
        reunionsfooter = findViewById(R.id.reunionsfooter);
        profilefooter = findViewById(R.id.profilefooter);
        if (presencefooter != null) {
            presencefooter.setOnClickListener(v -> {
                // Ouvrir l'activité des attestations employé
                startActivity(new Intent(AcceuilEmployeActivity.this, PresenceActivity.class));
            });
            if (congesfooter != null) {
                congesfooter.setOnClickListener(v -> {
                    // Remplacer ProfileActivity.class par la classe réelle
                    startActivity(new Intent(AcceuilEmployeActivity.this, CongesEmploye.class));
                });
                if (reunionsfooter != null) {
                    reunionsfooter.setOnClickListener(v -> {
                        // Ouvrir l'activité des attestations employé
                        startActivity(new Intent(AcceuilEmployeActivity.this, ReunionEmployeActivity.class));
                    });
                    if (profilefooter != null) {
                        profilefooter.setOnClickListener(v -> {
                            // Ouvrir l'activité des attestations employé
                            startActivity(new Intent(AcceuilEmployeActivity.this, ProfileActivity.class));
                        });

                    }
                }
            }
        }
    }
}