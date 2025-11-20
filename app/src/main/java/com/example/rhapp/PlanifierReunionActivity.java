package com.example.rhapp;

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

import com.example.rhapp.model.Reunion;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PlanifierReunionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_planifier_reunion);

        EditText titreReunion = findViewById(R.id.titrereunion);
        EditText dateReunion = findViewById(R.id.dateReunion);
        EditText heureReunion = findViewById(R.id.heureReunion);
        Spinner departementReunion = findViewById(R.id.departementReunion);
        EditText lieuReunion = findViewById(R.id.lieuReunion);
        EditText descriptionReunion = findViewById(R.id.descriptionReunion);
        Button btnCreerReunion = findViewById(R.id.btnCreerReunion);
        TextView close= findViewById(R.id.close);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
        //***************************** quitter la planification  *****************



        //***************************** planifier un reunion *****************
        btnCreerReunion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titre = titreReunion.getText().toString().trim();
                String date = dateReunion.getText().toString().trim();
                String heure = heureReunion.getText().toString().trim();
                String lieu =lieuReunion.getText().toString().trim();
                String departement= departementReunion.getSelectedItem().toString();
                String description = descriptionReunion.getText().toString().trim();

                //  Créer un objet de type Reunion (avec les champs saisies)
                Reunion reunion = new Reunion(titre, date, heure,lieu , departement, description);

                //  Envoyer directement cet objet à Firebase
                db.collection("Reunions")
                        .add(reunion)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(PlanifierReunionActivity.this, "Réunion ajoutée avec succès !", Toast.LENGTH_SHORT).show();

                            // Réinitialiser les champs
                            titreReunion.setText("");
                            dateReunion.setText("");
                            heureReunion.setText("");
                            lieuReunion.setText("");
                            departementReunion.setSelection(0);
                            descriptionReunion.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(PlanifierReunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }



        });


    }


}