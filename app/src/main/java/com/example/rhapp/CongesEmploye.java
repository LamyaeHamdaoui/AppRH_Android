package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.rhapp.model.Conge;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CongesEmploye extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout historiqueContainer;
    private Button btnNouvelleDemandeConge;
    private ListenerRegistration congesListener;

    private TextView soldeCongeHeader, soldeConge, congeAttente, congeApprouve, congeRefuse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conges_employe);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupFirestoreListener();
    }

    private void initializeViews() {
        soldeCongeHeader = findViewById(R.id.soldeCongeHeader);
        soldeConge = findViewById(R.id.soldeConge);
        congeAttente = findViewById(R.id.congeAttente);
        congeApprouve = findViewById(R.id.congeApprouve);
        congeRefuse = findViewById(R.id.congeRefuse); // Changé de congePris à congeRefuse
        historiqueContainer = findViewById(R.id.historiqueContainer);
        btnNouvelleDemandeConge = findViewById(R.id.btnNouvelleDemandeConge);

        soldeCongeHeader.setText("Solde disponible : 45 jours");
        soldeConge.setText("45");
        congeAttente.setText("0");
        congeApprouve.setText("0");
        congeRefuse.setText("0"); // Initialisé à 0
    }

    private void setupClickListeners() {
        btnNouvelleDemandeConge.setOnClickListener(v -> openDemandeCongeFragment());

        // Navigation
        findViewById(R.id.accueil).setOnClickListener(v ->
                Toast.makeText(CongesEmploye.this, "Accueil", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.employes).setOnClickListener(v ->
                Toast.makeText(CongesEmploye.this, "Présence", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.conge).setOnClickListener(v ->
                Toast.makeText(CongesEmploye.this, "Congés", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.reunions).setOnClickListener(v ->
                Toast.makeText(CongesEmploye.this, "Réunions", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.profil).setOnClickListener(v ->
                Toast.makeText(CongesEmploye.this, "Profil", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupFirestoreListener() {
        congesListener = db.collection("conges")
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<Conge> congesList = value.toObjects(Conge.class);
                        updateCongesStatistics(congesList);
                        displayHistoriqueConges(congesList);
                    }
                });
    }

    private void updateCongesStatistics(List<Conge> congesList) {
        int attenteCount = 0;
        int approuveCount = 0;
        int refuseCount = 0; // Nouveau compteur pour les refusés
        int prisCount = 0;

        for (Conge conge : congesList) {
            switch (conge.getStatut()) {
                case "En attente":
                    attenteCount++;
                    break;
                case "Approuvé":
                    approuveCount++;
                    prisCount += conge.getDuree();
                    break;
                case "Refusé":
                    refuseCount++; // Compter les refusés
                    break;
            }
        }

        int soldeInitial = 45;
        int soldeRestant = soldeInitial - prisCount;

        congeAttente.setText(String.valueOf(attenteCount));
        congeApprouve.setText(String.valueOf(approuveCount));
        congeRefuse.setText(String.valueOf(refuseCount)); // Mettre à jour le compteur des refusés
        soldeConge.setText(String.valueOf(soldeRestant));
        soldeCongeHeader.setText("Solde disponible : " + soldeRestant + " jours");
    }

    private void displayHistoriqueConges(List<Conge> congesList) {
        historiqueContainer.removeAllViews();

        if (congesList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune demande de congé trouvée");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(0, 50, 0, 0);
            historiqueContainer.addView(emptyText);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH);

        for (Conge conge : congesList) {
            View carteView = getLayoutInflater().inflate(R.layout.item_historique_conge_card, null);

            TextView typeConge = carteView.findViewById(R.id.TypeConge);
            TextView statutConge = carteView.findViewById(R.id.StatutConge);
            TextView datesConge = carteView.findViewById(R.id.DatesConge);
            TextView dureeConge = carteView.findViewById(R.id.DureeConge);
            TextView motifConge = carteView.findViewById(R.id.MotifConge);

            if (typeConge != null) typeConge.setText(conge.getTypeConge());
            if (statutConge != null) {
                statutConge.setText(conge.getStatut());
                switch (conge.getStatut()) {
                    case "En attente":
                        statutConge.setBackgroundResource(R.drawable.border_orange);
                        break;
                    case "Approuvé":
                        statutConge.setBackgroundResource(R.drawable.border_green);
                        break;
                    case "Refusé":
                        statutConge.setBackgroundResource(R.drawable.border_icon_red);
                        break;
                }
            }

            if (datesConge != null) {
                String dates = dateFormat.format(conge.getDateDebut()) + " - " + dateFormat.format(conge.getDateFin());
                datesConge.setText(dates);
            }

            if (dureeConge != null) {
                dureeConge.setText(conge.getDuree() + " jours");
            }

            if (motifConge != null) {
                motifConge.setText(conge.getMotif());
            }

            historiqueContainer.addView(carteView);
        }
    }

    private void openDemandeCongeFragment() {
        DemandeCongeFragment fragment = new DemandeCongeFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (congesListener != null) {
            congesListener.remove();
        }
    }
}