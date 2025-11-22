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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttestationEmployeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvAttente, tvApprouve, tvTotal;
    private LinearLayout historiqueContainer;
    private Button btnNouvelleDemande;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attestation_employe);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupFirebase();
        loadAttestations();
        setupClickListeners();
    }

    private void initViews() {
        tvAttente = findViewById(R.id.congeAttente);
        tvApprouve = findViewById(R.id.congeApprouve);
        tvTotal = findViewById(R.id.congePris);
        historiqueContainer = findViewById(R.id.historiqueContainer);
        btnNouvelleDemande = findViewById(R.id.btnNouvelleDemandeConge);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void loadAttestations() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Attestations")
                .whereEqualTo("employeId", currentUser.getUid())
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attestation> attestations = new ArrayList<>();
                    int enAttente = 0, approuvees = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attestation attestation = document.toObject(Attestation.class);
                        attestation.setId(document.getId());
                        attestations.add(attestation);

                        // Compter les statuts
                        if ("en_attente".equals(attestation.getStatut())) {
                            enAttente++;
                        } else if ("approuvee".equals(attestation.getStatut())) {
                            approuvees++;
                        }
                    }

                    // Mettre à jour les statistiques
                    updateStats(enAttente, approuvees, attestations.size());

                    // Afficher l'historique
                    afficherHistorique(attestations);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStats(int enAttente, int approuvees, int total) {
        if (tvAttente != null) tvAttente.setText(String.valueOf(enAttente));
        if (tvApprouve != null) tvApprouve.setText(String.valueOf(approuvees));
        if (tvTotal != null) tvTotal.setText(String.valueOf(total));
    }

    private void afficherHistorique(List<Attestation> attestations) {
        if (historiqueContainer == null) return;

        historiqueContainer.removeAllViews();

        if (attestations.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune demande d'attestation");
            emptyText.setTextSize(16);
            emptyText.setPadding(50, 50, 50, 50);
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            historiqueContainer.addView(emptyText);
            return;
        }

        for (Attestation attestation : attestations) {
            View itemView = createItemView(attestation);
            if (itemView != null) {
                historiqueContainer.addView(itemView);
            }
        }
    }

    private View createItemView(Attestation attestation) {
        int layoutRes = getLayoutForStatut(attestation.getStatut());
        View itemView = getLayoutInflater().inflate(layoutRes, historiqueContainer, false);

        configureItemView(itemView, attestation);

        return itemView;
    }

    private int getLayoutForStatut(String statut) {
        switch (statut) {
            case "approuvee":
                return R.layout.item_card_attestation_approuvee;
            case "refusee":
                return R.layout.item_attestation_refuse;
            default: // en_attente
                return R.layout.item_card_attestation_enattente;
        }
    }

    private void configureItemView(View itemView, Attestation attestation) {
        // Formatter la date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        String dateDemande = dateFormat.format(attestation.getDateDemande());

        // Configurer les vues communes
        TextView typeView = itemView.findViewById(R.id.TypeAttestation);
        TextView statutView = itemView.findViewById(R.id.StatutConge);
        TextView dateView = itemView.findViewById(R.id.DatesDemandee);
        TextView motifView = itemView.findViewById(R.id.MotifAttestation);

        if (typeView != null) typeView.setText(attestation.getTypeAttestation());
        if (dateView != null) dateView.setText("Demandée le " + dateDemande);
        if (motifView != null && attestation.getMotif() != null) {
            motifView.setText(attestation.getMotif());
        }

        // Pour les attestations approuvées
// Version corrigée - sans référence à dateApprobation
        if ("approuvee".equals(attestation.getStatut())) {
            Button btnTelecharger = itemView.findViewById(R.id.btnTelecharger);
            if (btnTelecharger != null) {
                btnTelecharger.setOnClickListener(v -> {
                    telechargerAttestation(attestation);
                });
            }

            // Pour les attestations approuvées, on peut mettre à jour le texte de date
            if (attestation.getDateTraitement() != null && dateView != null) {
                String dateApprob = dateFormat.format(attestation.getDateTraitement());
                dateView.setText("Approuvée le " + dateApprob);
            }
        }
        // Pour les attestations refusées
        if ("refusee".equals(attestation.getStatut())) {
            TextView dateRefus = itemView.findViewById(R.id.DateDemandee);
            if (dateRefus != null && attestation.getMotifRefus() != null) {
                dateRefus.setText("Refusée - " + attestation.getMotifRefus());
            }
        }
    }

    private void telechargerAttestation(Attestation attestation) {
        if (attestation.getPdfUrl() != null && !attestation.getPdfUrl().isEmpty()) {
            // Implémenter le téléchargement du PDF
            Toast.makeText(this, "Téléchargement de l'attestation...", Toast.LENGTH_SHORT).show();
            // Ici vous pouvez utiliser Intent pour ouvrir le PDF ou le télécharger
        } else {
            Toast.makeText(this, "PDF non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        if (btnNouvelleDemande != null) {
            btnNouvelleDemande.setOnClickListener(v -> {
                // Ouvrir le fragment d'ajout
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, new AjouterAttestationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttestations();
    }
}