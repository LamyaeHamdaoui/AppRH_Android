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
import com.google.firebase.auth.FirebaseAuth;

public class PlanifierReunionActivity extends AppCompatActivity {
    private EditText titreReunion, dateReunion, heureReunion, lieuReunion, descriptionReunion;
    private Spinner departementReunion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_planifier_reunion);

         titreReunion = findViewById(R.id.titrereunion);
         dateReunion = findViewById(R.id.dateReunion);
         heureReunion = findViewById(R.id.heureReunion);
         departementReunion = findViewById(R.id.departementReunion);
         lieuReunion = findViewById(R.id.lieuReunion);
         descriptionReunion = findViewById(R.id.descriptionReunion);
        Button btnCreerReunion = findViewById(R.id.btnCreerReunion);
        TextView close = findViewById(R.id.close);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();


        //***************************** planifier un reunion *****************
        btnCreerReunion.setOnClickListener(v -> {
            String titre = titreReunion.getText().toString().trim();
            String date = dateReunion.getText().toString().trim();
            String heure = heureReunion.getText().toString().trim();
            String lieu = lieuReunion.getText().toString().trim();
            String departement = departementReunion.getSelectedItem().toString();
            String description = descriptionReunion.getText().toString().trim();

            // Vérifier que tous les champs sont remplis
            if (titre.isEmpty() || date.isEmpty() || heure.isEmpty() || lieu.isEmpty() || description.isEmpty() || departement.equals("Sélectionnez un département")) {
                Toast.makeText(this, "Tous les champs sont obligatoires !", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier le format de la date : par exemple "dd/MM/yyyy"
            if (!date.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                Toast.makeText(this, "Date invalide ! Format attendu : jj/mm/aaaa", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier le format de l'heure : par exemple "HH:mm"
            if (!heure.matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
                Toast.makeText(this, "Heure invalide ! Format attendu : HH:mm", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si tout est correct, créer la réunion
            creerReunionAvecLeader(titre, date, heure, lieu, departement, description);
        });





//        btnCreerReunion.setOnClickListener(v -> {
//            String titre = titreReunion.getText().toString().trim();
//            String date = dateReunion.getText().toString().trim();
//            String heure = heureReunion.getText().toString().trim();
//            String lieu = lieuReunion.getText().toString().trim();
//            String departement = departementReunion.getSelectedItem().toString();
//            String description = descriptionReunion.getText().toString().trim();
//
//            //  Récupérer le nom ou ID du RH connecté
//            String currentRhId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//            //  Créer l’objet Reunion avec planifiePar
//            Reunion reunion = new Reunion(titre, date, heure, lieu, departement, description, currentRhId);
//
//            //  Envoyer à Firestore
//            FirebaseFirestore.getInstance()
//                    .collection("Reunions")
//                    .add(reunion)
//                    .addOnSuccessListener(documentReference -> {
//                        Toast.makeText(PlanifierReunionActivity.this, "Réunion ajoutée !", Toast.LENGTH_SHORT).show();
//                        // Réinitialiser les champs
//                        titreReunion.setText("");
//                        dateReunion.setText("");
//                        heureReunion.setText("");
//                        lieuReunion.setText("");
//                        departementReunion.setSelection(0);
//                        descriptionReunion.setText("");
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(PlanifierReunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//        });

    }




    private void creerReunionAvecLeader(
            String titre,
            String date,
            String heure,
            String lieu,
            String departement,
            String description
    ) {
        String currentRhId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentRhId)
                .get()
                .addOnSuccessListener(rhSnap -> {
                    if (rhSnap.exists()) {

                        String nom = rhSnap.getString("nom");
                        String prenom = rhSnap.getString("prenom");

                        // Capitaliser nom et prénom
                        nom = capitalizeWords(nom);
                        prenom = capitalizeWords(prenom);

                        String fullName = nom + " " + prenom;

                        // Créer la réunion
                        Reunion reunion = new Reunion(
                                titre, date, heure, lieu,
                                departement, description, currentRhId
                        );
                        reunion.setLeaderNomComplet(fullName);

                        FirebaseFirestore.getInstance()
                                .collection("Reunions")
                                .add(reunion)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(
                                            PlanifierReunionActivity.this,
                                            "Réunion ajoutée avec succès",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // 🔥 RÉINITIALISER LES CHAMPS ICI
                                    titreReunion.setText("");
                                    dateReunion.setText("");
                                    heureReunion.setText("");
                                    lieuReunion.setText("");
                                    departementReunion.setSelection(0);
                                    descriptionReunion.setText("");

                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                PlanifierReunionActivity.this,
                                                "Erreur : " + e.getMessage(),
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                    }
                });
    }




    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();

        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }




}