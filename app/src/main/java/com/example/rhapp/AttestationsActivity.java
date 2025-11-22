package com.example.rhapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttestationsActivity extends AppCompatActivity {

    private Button btnAttente, btnApprouve, btnRefuse;
    private LinearLayout containerAttestations;

    // TextViews pour les statistiques - vous devrez les ajouter dans votre XML
    private TextView tvTotal, tvAttente, tvApprouve, tvRefuse;

    private FirebaseFirestore db;
    private String filtreActuel = "en_attente";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attestations);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupFirebase();
        loadStats();
        chargerAttestations(filtreActuel);
        setupClickListeners();
    }

    private void initViews() {
        btnAttente = findViewById(R.id.btnCongeAttente);
        btnApprouve = findViewById(R.id.btnCongeApprouve);
        btnRefuse = findViewById(R.id.btnCongeRefuse);

        // Initialiser le conteneur - vous devrez l'ajouter dans votre XML
        containerAttestations = findViewById(R.id.containerAttestations);

        // Si le conteneur n'existe pas, utilisez le layout principal
        if (containerAttestations == null) {
            LinearLayout mainLayout = findViewById(R.id.main);
            if (mainLayout != null) {
                // Créer un nouveau conteneur dynamiquement
                containerAttestations = new LinearLayout(this);
                containerAttestations.setOrientation(LinearLayout.VERTICAL);
                mainLayout.addView(containerAttestations);
            }
        }

        // Initialiser les TextViews des statistiques
        // Vous devrez ajouter ces IDs dans votre XML activity_attestations.xml
        tvTotal = findViewById(R.id.tvTotal);
        tvAttente = findViewById(R.id.tvAttente);
        tvApprouve = findViewById(R.id.tvApprouve);
        tvRefuse = findViewById(R.id.tvRefuse);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void loadStats() {
        db.collection("Attestations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = 0, enAttente = 0, approuvees = 0, refusees = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        total++;
                        Attestation attestation = document.toObject(Attestation.class);

                        switch (attestation.getStatut()) {
                            case "en_attente":
                                enAttente++;
                                break;
                            case "approuvee":
                                approuvees++;
                                break;
                            case "refusee":
                                refusees++;
                                break;
                        }
                    }

                    updateStatsUI(total, enAttente, approuvees, refusees);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatsUI(int total, int enAttente, int approuvees, int refusees) {
        if (tvTotal != null) tvTotal.setText(String.valueOf(total));
        if (tvAttente != null) tvAttente.setText(String.valueOf(enAttente));
        if (tvApprouve != null) tvApprouve.setText(String.valueOf(approuvees));
        if (tvRefuse != null) tvRefuse.setText(String.valueOf(refusees));
    }

    private void chargerAttestations(String statut) {
        db.collection("Attestations")
                .whereEqualTo("statut", statut)
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attestation> attestations = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attestation attestation = document.toObject(Attestation.class);
                        attestation.setId(document.getId());
                        attestations.add(attestation);
                    }

                    afficherAttestations(attestations);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void afficherAttestations(List<Attestation> attestations) {
        if (containerAttestations == null) return;

        containerAttestations.removeAllViews();

        if (attestations.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune attestation " + getStatutText(filtreActuel));
            emptyText.setTextSize(16);
            emptyText.setPadding(50, 50, 50, 50);
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            containerAttestations.addView(emptyText);
            return;
        }

        for (Attestation attestation : attestations) {
            View itemView = getLayoutInflater().inflate(
                    R.layout.item_attestations_card,
                    containerAttestations,
                    false
            );

            configureAdminItemView(itemView, attestation);
            containerAttestations.addView(itemView);
        }
    }

    private String getStatutText(String statut) {
        switch (statut) {
            case "en_attente": return "en attente";
            case "approuvee": return "approuvée";
            case "refusee": return "refusée";
            default: return "";
        }
    }

    private void configureAdminItemView(View itemView, Attestation attestation) {
        // Formatter la date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        String dateDemande = dateFormat.format(attestation.getDateDemande());

        // Configurer les TextViews
        TextView nomComplet = itemView.findViewById(R.id.nomComplet);
        TextView departement = itemView.findViewById(R.id.departement);
        TextView typeAttestation = itemView.findViewById(R.id.TypeConge);
        TextView dateDemandee = itemView.findViewById(R.id.DateDemandee);
        TextView motif = itemView.findViewById(R.id.MotifAttestation);
        TextView statut = itemView.findViewById(R.id.StatutAttestation);

        // Configurer les boutons
        Button btnDetails = itemView.findViewById(R.id.btnDetails);
        Button btnValidate = itemView.findViewById(R.id.btnValidate);
        Button btnReject = itemView.findViewById(R.id.btnReject);

        // Remplir les données
        if (nomComplet != null) nomComplet.setText(attestation.getEmployeNom());
        if (departement != null) departement.setText(attestation.getEmployeDepartement());
        if (typeAttestation != null) typeAttestation.setText(attestation.getTypeAttestation());
        if (dateDemandee != null) dateDemandee.setText("Demandée le " + dateDemande);
        if (motif != null && attestation.getMotif() != null) {
            motif.setText("Motif : " + attestation.getMotif());
        }
        if (statut != null) statut.setText(getStatutDisplayText(attestation.getStatut()));

        // Configurer les boutons
        if (btnValidate != null) {
            btnValidate.setOnClickListener(v -> validerAttestation(attestation));
        }

        if (btnReject != null) {
            btnReject.setOnClickListener(v -> refuserAttestation(attestation));
        }

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> voirDetails(attestation));
        }
    }

    private String getStatutDisplayText(String statut) {
        switch (statut) {
            case "en_attente": return "En attente";
            case "approuvee": return "Approuvée";
            case "refusee": return "Refusée";
            default: return statut;
        }
    }

    private void validerAttestation(Attestation attestation) {
        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "approuvee",
                        "dateTraitement", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attestation approuvée", Toast.LENGTH_SHORT).show();
                    rechargerDonnees();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void refuserAttestation(Attestation attestation) {
        // Pour l'instant, refus sans motif. Vous pouvez ajouter un dialog pour saisir le motif
        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "refusee",
                        "dateTraitement", new java.util.Date(),
                        "motifRefus", "Raison non spécifiée"
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attestation refusée", Toast.LENGTH_SHORT).show();
                    rechargerDonnees();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void voirDetails(Attestation attestation) {
        // Ouvrir un dialog avec les détails complets
        Toast.makeText(this, "Détails: " + attestation.getTypeAttestation(), Toast.LENGTH_SHORT).show();
    }

    private void setupClickListeners() {
        if (btnAttente != null) {
            btnAttente.setOnClickListener(v -> {
                filtreActuel = "en_attente";
                chargerAttestations(filtreActuel);
                updateButtonStyles();
            });
        }

        if (btnApprouve != null) {
            btnApprouve.setOnClickListener(v -> {
                filtreActuel = "approuvee";
                chargerAttestations(filtreActuel);
                updateButtonStyles();
            });
        }

        if (btnRefuse != null) {
            btnRefuse.setOnClickListener(v -> {
                filtreActuel = "refusee";
                chargerAttestations(filtreActuel);
                updateButtonStyles();
            });
        }
    }

    private void updateButtonStyles() {
        // Réinitialiser tous les boutons
        resetButtonStyle(btnAttente);
        resetButtonStyle(btnApprouve);
        resetButtonStyle(btnRefuse);

        // Mettre en surbrillance le bouton actif
        switch (filtreActuel) {
            case "en_attente":
                setActiveButtonStyle(btnAttente);
                break;
            case "approuvee":
                setActiveButtonStyle(btnApprouve);
                break;
            case "refusee":
                setActiveButtonStyle(btnRefuse);
                break;
        }
    }

    private void resetButtonStyle(Button button) {
        if (button != null) {
            button.setBackgroundResource(R.drawable.border_gris);
            button.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void setActiveButtonStyle(Button button) {
        if (button != null) {
            button.setBackgroundResource(R.drawable.border_button);
            button.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void rechargerDonnees() {
        loadStats();
        chargerAttestations(filtreActuel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rechargerDonnees();
    }
}