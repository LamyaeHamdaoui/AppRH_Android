package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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



        Intent intent = getIntent();
        String reunionId = intent.getStringExtra("reunionId");
        String titre = intent.getStringExtra("titre");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        String lieu = intent.getStringExtra("lieu");
        String departement= intent.getStringExtra("departement");
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

                //  Créer un objet de type Reunion (avec les champs saisies)
                // Reunion reunion = new Reunion(titre, date, heure, lieu, departement, description);

                //  Envoyer directement cet objet à Firebase
                db.collection("Reunions")
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
                            Toast.makeText(ModifierReunionActivity.this, " Modification réussie !", Toast.LENGTH_SHORT).show();
                            finish(); // retourne à l’activité précédente

                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ModifierReunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

        });
    }

}



