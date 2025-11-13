package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rhapp.model.Reunion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        // ************************************* Afficher les cards view des reuinons a venir **************************
        afficherReunions();
        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        db = FirebaseFirestore.getInstance();


    }

    // *************************************fonction  Afficher  des reuinons a venir **************************
    private LinearLayout reunionPlanifieContainer;
    private FirebaseFirestore db;
    private void afficherReunions() {
        db.collection("Reunions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Aucune réunion trouvée", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Pour chaque réunion dans Firestore
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reunion reunion = doc.toObject(Reunion.class);

                        //  On "gonfle" la carte depuis le XML
                        View cardView = LayoutInflater.from(this)
                                .inflate(R.layout.item_card_reunion_rh,reunionPlanifieContainer, false);

                        // On remplit les données
                        TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                        TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                        TextView timeReunion = cardView.findViewById(R.id.timeReunion);
                        TextView departementReunion= cardView.findViewById(R.id.localReunion);
                        TextView descriptionReunion = cardView.findViewById(R.id.reunionDescription);

                        titreReunion.setText(reunion.getTitre());
                        dateReunion.setText(reunion.getDate());
                        timeReunion.setText(reunion.getHeure());
                        departementReunion.setText(reunion.getDepartement());
                        descriptionReunion.setText(reunion.getDescription());

                        //  On ajoute la carte dans le conteneur
                        reunionPlanifieContainer.addView(cardView);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}