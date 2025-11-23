package com.example.rhapp;
import com.google.firebase.Timestamp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttestationEmployeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvAttente, tvApprouve, tvRefuse, tvTotal;
    private LinearLayout historiqueContainer;
    private Button btnNouvelleDemande;

    // Cache pour éviter les rechargements inutiles
    private boolean isLoading = false;
    private long lastLoadTime = 0;
    private static final long MIN_RELOAD_INTERVAL = 2000; // 2 secondes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attestation_employe);

        // Éviter EdgeToEdge si ça cause des problèmes
        // EdgeToEdge.enable(this);

        initViews();
        setupFirebase();

        // Charger les données après un petit délai pour laisser l'UI se initialiser
        historiqueContainer.postDelayed(this::loadAttestations, 100);

        setupClickListeners();
    }

    private void initViews() {
        tvAttente = findViewById(R.id.AttestaionAttente);
        tvApprouve = findViewById(R.id.AttestationApprouve);
        tvRefuse = findViewById(R.id.attestationRefuse);
        tvTotal = findViewById(R.id.attestationTotal);
        historiqueContainer = findViewById(R.id.historiqueContainer);
        btnNouvelleDemande = findViewById(R.id.btnNouvelleDemandeConge);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void loadAttestations() {
        if (isLoading) {
            Log.d("ATTESTATION", "Chargement déjà en cours, ignoré");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLoadTime < MIN_RELOAD_INTERVAL) {
            Log.d("ATTESTATION", "Rechargement trop fréquent, ignoré");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("ATTESTATION", "Utilisateur non connecté");
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoading = true;
        String userUid = currentUser.getUid();
        String userEmail = currentUser.getEmail();

        Log.d("ATTESTATION", "=== DÉBUT CHARGEMENT ===");
        Log.d("ATTESTATION", "UID utilisateur: " + userUid);
        Log.d("ATTESTATION", "Email utilisateur: " + userEmail);

        // TEST: Vérifier d'abord si la collection existe et contient des données
        db.collection("Attestations")
                .limit(1)
                .get()
                .addOnSuccessListener(testSnapshot -> {
                    Log.d("ATTESTATION", "Collection Attestations accessible, nombre total de documents: " + testSnapshot.size());

                    // Maintenant chercher les attestations de l'utilisateur
                    db.collection("Attestations")
                            .whereEqualTo("employeeId", userUid)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                isLoading = false;
                                lastLoadTime = System.currentTimeMillis();

                                Log.d("ATTESTATION", "=== RÉSULTAT RECHERCHE ===");
                                Log.d("ATTESTATION", "Nombre de documents trouvés: " + queryDocumentSnapshots.size());
                                Log.d("ATTESTATION", "Requête: employeeId = " + userUid);

                                List<Attestation> attestations = new ArrayList<>();
                                int enAttente = 0, approuvees = 0, refusees = 0;

                                // Afficher tous les documents pour debug
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    Log.d("ATTESTATION", "Document trouvé - ID: " + document.getId());
                                    Log.d("ATTESTATION", "Données: " + document.getData());

                                    try {
                                        Attestation attestation = new Attestation();
                                        attestation.setId(document.getId());

                                        // Lire chaque champ individuellement pour debug
                                        String employeeId = document.getString("employeeId");
                                        String employeeNom = document.getString("employeeNom");
                                        String employeeDepartment = document.getString("employeeDepartment");
                                        String typeAttestation = document.getString("typeAttestation");
                                        String motif = document.getString("motif");
                                        String statut = document.getString("statut");

                                        Log.d("ATTESTATION", "Champs lus - employeeId: " + employeeId +
                                                ", employeeNom: " + employeeNom +
                                                ", statut: " + statut);

                                        attestation.setEmployeId(employeeId);
                                        attestation.setEmployeNom(employeeNom);
                                        attestation.setEmployeDepartement(employeeDepartment);
                                        attestation.setTypeAttestation(typeAttestation);
                                        attestation.setMotif(motif);
                                        attestation.setStatut(statut);

                                        // Gestion des dates
                                        Timestamp dateDemande = document.getTimestamp("dateDemande");
                                        if (dateDemande != null) {
                                            attestation.setDateDemande(dateDemande.toDate());
                                            Log.d("ATTESTATION", "Date demande: " + dateDemande.toDate());
                                        }

                                        Timestamp dateTraitement = document.getTimestamp("dateTraitement");
                                        if (dateTraitement != null) {
                                            attestation.setDateTraitement(dateTraitement.toDate());
                                        }

                                        attestation.setPdfUrl(document.getString("pdfUrl"));
                                        attestation.setMotifRefus(document.getString("motifRefus"));

                                        attestations.add(attestation);

                                        // Compter par statut
                                        if (statut == null || "en_attente".equals(statut)) {
                                            enAttente++;
                                        } else if ("approuvee".equals(statut)) {
                                            approuvees++;
                                        } else if ("refusee".equals(statut)) {
                                            refusees++;
                                        }

                                    } catch (Exception e) {
                                        Log.e("ATTESTATION", "Erreur conversion document " + document.getId() + ": " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }

                                Log.d("ATTESTATION", "=== STATISTIQUES ===");
                                Log.d("ATTESTATION", "En attente: " + enAttente + ", Approuvées: " + approuvees + ", Refusées: " + refusees + ", Total: " + attestations.size());

                                updateStats(enAttente, approuvees, refusees, attestations.size());
                                afficherHistorique(attestations);

                            })
                            .addOnFailureListener(e -> {
                                isLoading = false;
                                lastLoadTime = System.currentTimeMillis();
                                Log.e("ATTESTATION", "ERREUR Firestore: " + e.getMessage());
                                e.printStackTrace();
                                Toast.makeText(this, "Erreur de chargement: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    lastLoadTime = System.currentTimeMillis();
                    Log.e("ATTESTATION", "ERREUR Accès collection: " + e.getMessage());
                    Toast.makeText(this, "Erreur d'accès à la base de données", Toast.LENGTH_LONG).show();
                });
    }

    private void updateStats(int enAttente, int approuvees, int refusees, int total) {
        runOnUiThread(() -> {
            tvAttente.setText(String.valueOf(enAttente));
            tvApprouve.setText(String.valueOf(approuvees));
            tvRefuse.setText(String.valueOf(refusees));
            tvTotal.setText(String.valueOf(total));
        });
    }

    private void afficherHistorique(List<Attestation> attestations) {
        runOnUiThread(() -> {
            historiqueContainer.removeAllViews();

            if (attestations.isEmpty()) {
                TextView emptyText = new TextView(this);
                emptyText.setText("Aucune demande d'attestation");
                emptyText.setTextSize(16);
                emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                emptyText.setPadding(0, 50, 0, 50);
                historiqueContainer.addView(emptyText);
                return;
            }

            // Utiliser un layout simple pour éviter les problèmes de rendu
            for (Attestation attestation : attestations) {
                try {
                    View itemView = getLayoutInflater().inflate(
                            getLayoutForStatut(attestation.getStatut()),
                            historiqueContainer,
                            false
                    );
                    configureItemView(itemView, attestation);
                    historiqueContainer.addView(itemView);
                } catch (Exception e) {
                    Log.e("ATTESTATION", "Erreur création vue: " + e.getMessage());
                }
            }
        });
    }

    private int getLayoutForStatut(String statut) {
        if ("approuvee".equals(statut)) {
            return R.layout.item_card_attestation_approuvee;
        } else if ("refusee".equals(statut)) {
            return R.layout.item_card_attestation_refusee;
        } else {
            return R.layout.item_card_attestation_enattente;
        }
    }

    private void configureItemView(View itemView, Attestation attestation) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

        // Configurer les vues communes
        TextView typeView = itemView.findViewById(R.id.TypeAttestation);
        TextView dateDemandeView = itemView.findViewById(R.id.DatesDemandee);
        TextView motifView = itemView.findViewById(R.id.MotifAttestation);

        if (typeView != null) {
            typeView.setText(attestation.getTypeAttestation());
        }

        if (dateDemandeView != null && attestation.getDateDemande() != null) {
            String dateStr = dateFormat.format(attestation.getDateDemande());
            dateDemandeView.setText("Demandée le " + dateStr);
        }

        if (motifView != null) {
            String motif = attestation.getMotif();
            motifView.setText(motif != null && !motif.isEmpty() ?
                    "Motif: " + motif : "Aucun motif spécifié");
        }

        // Configurations spécifiques
        String statut = attestation.getStatut();
        if ("approuvee".equals(statut)) {
            Button btnTelecharger = itemView.findViewById(R.id.btnTelecharger);
            TextView dateApprobationView = itemView.findViewById(R.id.DateApprobation);

            if (btnTelecharger != null) {
                btnTelecharger.setOnClickListener(v -> {
                    telechargerAttestation(attestation);
                });
            }

            if (dateApprobationView != null && attestation.getDateTraitement() != null) {
                String dateApprob = dateFormat.format(attestation.getDateTraitement());
                dateApprobationView.setText("Approuvée le " + dateApprob);
            }
        }
    }

    private void telechargerAttestation(Attestation attestation) {
        if (attestation.getPdfUrl() != null && !attestation.getPdfUrl().isEmpty()) {
            Toast.makeText(this, "Téléchargement du PDF", Toast.LENGTH_SHORT).show();
            // Implémenter le téléchargement ici
        } else {
            Toast.makeText(this, "PDF non disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        btnNouvelleDemande.setOnClickListener(v -> {
            // Utiliser un délai pour éviter les conflits d'animation
            v.postDelayed(() -> {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                transaction.replace(R.id.main, new AjouterAttestationFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }, 100);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger avec un délai
        historiqueContainer.postDelayed(this::loadAttestations, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les références
        historiqueContainer = null;
    }
}