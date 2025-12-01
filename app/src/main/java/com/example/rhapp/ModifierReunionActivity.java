package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.firebase.firestore.FirebaseFirestore;

public class ModifierReunionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_modifier_reunion);


        EditText titreReunion = findViewById(R.id.titrereunion);
        EditText dateReunion = findViewById(R.id.dateReunion);
        EditText heureReunion = findViewById(R.id.heureReunion);
        Spinner departementReunion = findViewById(R.id.departementReunion);
        EditText lieuReunion = findViewById(R.id.lieuReunion);
        EditText descriptionReunion = findViewById(R.id.descriptionReunion);
        Button btnCreerReunion = findViewById(R.id.btnCreerReunion);
        TextView close = findViewById(R.id.close);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });


        Intent intent = getIntent();
        String reunionId = intent.getStringExtra("reunionId");
        String titre = intent.getStringExtra("titre");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String lieu = intent.getStringExtra("lieu");
        String departement = intent.getStringExtra("departement");
        String description = intent.getStringExtra("description");


        titreReunion.setText(titre);
        dateReunion.setText(date);
        heureReunion.setText(time);
        lieuReunion.setText(lieu);
        descriptionReunion.setText(description);
        //pour recuperer l item de spinner departement
        android.widget.ArrayAdapter adapter = (android.widget.ArrayAdapter) departementReunion.getAdapter();
        int position = adapter.getPosition(departement);
        if (position >= 0) {
            departementReunion.setSelection(position);
        }


        //***************************** Enregistrer le reunion *****************
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        btnCreerReunion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titre = titreReunion.getText().toString().trim();
                String date = dateReunion.getText().toString().trim();
                String heure = heureReunion.getText().toString().trim();
                String lieu = lieuReunion.getText().toString().trim();
                String departement = departementReunion.getSelectedItem().toString();
                String description = descriptionReunion.getText().toString().trim();

                // Vérification des champs obligatoires
                if (titre.isEmpty() || date.isEmpty() || heure.isEmpty() || lieu.isEmpty() || description.isEmpty() || departement.equals("Sélectionnez un département")) {
                    Toast.makeText(ModifierReunionActivity.this, "Tous les champs sont obligatoires !", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vérification format date (jj/mm/aaaa)
                if (!date.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    Toast.makeText(ModifierReunionActivity.this, "Date invalide ! Format attendu : jj/mm/aaaa", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vérification format heure (HH:mm)
                if (!heure.matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
                    Toast.makeText(ModifierReunionActivity.this, "Heure invalide ! Format attendu : HH:mm", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Mise à jour de la réunion dans Firestore
                FirebaseFirestore.getInstance()
                        .collection("Reunions")
                        .document(reunionId)
                        .update(
                                "titre", titre,
                                "date", date,
                                "heure", heure,
                                "lieu", lieu,
                                "departement", departement,
                                "description", description
                        )
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(ModifierReunionActivity.this, "Modification réussie !", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ModifierReunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    }



