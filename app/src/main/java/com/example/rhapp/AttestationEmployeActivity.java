package com.example.rhapp;

import com.google.firebase.Timestamp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttestationEmployeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static final int WRITE_PERMISSION_REQUEST_CODE = 1002;
    private TextView tvAttente, tvApprouve, tvRefuse;
    private LinearLayout historiqueContainer;
    private Button btnNouvelleDemande;
    private ListenerRegistration attestationListener;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Thread management
    private ExecutorService executorService;
    private Handler mainHandler;

    // Cache pour éviter les rechargements inutiles
    private boolean isLoading = false;
    private long lastLoadTime = 0;
    private static final long MIN_RELOAD_INTERVAL = 2000; // 2 secondes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attestation_employe);

        // Initialisation des composants de threading
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupFirebase();
        setupClickListeners();
        setupSwipeRefresh();

        // Charger les données après un petit délai pour laisser l'UI s'initialiser
        historiqueContainer.postDelayed(this::setupRealTimeListener, 300);
    }

    private void initViews() {
        tvAttente = findViewById(R.id.AttestaionAttente);
        tvApprouve = findViewById(R.id.AttestationApprouve);
        tvRefuse = findViewById(R.id.attestationRefuse);
        historiqueContainer = findViewById(R.id.historiqueContainer);
        btnNouvelleDemande = findViewById(R.id.btnNouvelleDemandeConge);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) {
            Log.e("ATTESTATION", "SwipeRefreshLayout non trouvé dans le layout");
            return;
        }

        swipeRefreshLayout.setColorSchemeResources(
                R.color.blue,
                R.color.green,
                R.color.orange
        );

        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.white);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("ATTESTATION", "Swipe to refresh déclenché");
            reloadData();
        });
    }

    private void reloadData() {
        Log.d("ATTESTATION", "Forçage du rechargement des données");

        if (attestationListener != null) {
            attestationListener.remove();
            attestationListener = null;
        }

        lastLoadTime = 0;
        setupRealTimeListener();
    }

    private void setupRealTimeListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("ATTESTATION", "Utilisateur non connecté");
            stopRefreshAnimation();
            showToast("Utilisateur non connecté");
            return;
        }

        String userUid = currentUser.getUid();

        if (attestationListener != null) {
            attestationListener.remove();
        }

        Log.d("ATTESTATION", "Mise en place de l'écouteur en temps réel");

        // L'écouteur Firestore s'exécute déjà sur un thread séparé
        attestationListener = db.collection("Attestations")
                .whereEqualTo("employeeId", userUid)
                .addSnapshotListener(executorService, (queryDocumentSnapshots, error) -> {
                    stopRefreshAnimation();

                    if (error != null) {
                        Log.e("ATTESTATION", "Erreur écouteur temps réel: " + error.getMessage());
                        showToast("Erreur de synchronisation");
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Log.d("ATTESTATION", "Changement détecté - documents: " + queryDocumentSnapshots.size());
                        processAttestationsInBackground(queryDocumentSnapshots);
                    } else {
                        Log.d("ATTESTATION", "Aucune attestation trouvée");
                        updateStats(0, 0, 0);
                        afficherHistorique(new ArrayList<>());
                    }
                });
    }

    private void processAttestationsInBackground(QuerySnapshot queryDocumentSnapshots) {
        executorService.execute(() -> {
            try {
                List<Attestation> attestations = new ArrayList<>();
                int enAttente = 0, approuvees = 0, refusees = 0;

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Attestation attestation = new Attestation();
                        attestation.setId(document.getId());

                        String employeeId = document.getString("employeeId");
                        String employeeNom = document.getString("employeeNom");
                        String employeeDepartment = document.getString("employeeDepartment");
                        String typeAttestation = document.getString("typeAttestation");
                        String motif = document.getString("motif");
                        String statut = document.getString("statut");

                        attestation.setEmployeId(employeeId);
                        attestation.setEmployeNom(employeeNom);
                        attestation.setEmployeDepartement(employeeDepartment);
                        attestation.setTypeAttestation(typeAttestation);
                        attestation.setMotif(motif);
                        attestation.setStatut(statut);

                        // Gestion des dates dans le thread secondaire
                        Timestamp dateDemande = document.getTimestamp("dateDemande");
                        if (dateDemande != null) {
                            attestation.setDateDemande(dateDemande.toDate());
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
                        Log.e("ATTESTATION", "Erreur conversion document: " + e.getMessage());
                    }
                }

                Log.d("ATTESTATION", "Mise à jour - En attente: " + enAttente + ", Approuvées: " + approuvees + ", Refusées: " + refusees);

                // Créer des copies final des variables pour les utiliser dans le lambda
                final List<Attestation> finalAttestations = new ArrayList<>(attestations);
                final int finalEnAttente = enAttente;
                final int finalApprouvees = approuvees;
                final int finalRefusees = refusees;
                final boolean isRefreshing = swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing();

                // Retour au thread principal pour mettre à jour l'UI
                mainHandler.post(() -> {
                    updateStats(finalEnAttente, finalApprouvees, finalRefusees);
                    afficherHistorique(finalAttestations);

                    if (isRefreshing) {
                        showToast("Données rafraîchies");
                    }
                });

            } catch (Exception e) {
                Log.e("ATTESTATION", "Erreur lors du traitement des données: " + e.getMessage());
                mainHandler.post(() -> showToast("Erreur de traitement des données"));
            }
        });
    }

    private void stopRefreshAnimation() {
        mainHandler.post(() -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
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
            showToast("Utilisateur non connecté");
            return;
        }

        isLoading = true;
        String userUid = currentUser.getUid();

        Log.d("ATTESTATION", "=== DÉBUT CHARGEMENT DANS THREAD SECONDARY ===");

        executorService.execute(() -> {
            try {
                // TEST: Vérifier d'abord si la collection existe
                db.collection("Attestations")
                        .limit(1)
                        .get()
                        .addOnSuccessListener(testSnapshot -> {
                            // Maintenant chercher les attestations de l'utilisateur
                            db.collection("Attestations")
                                    .whereEqualTo("employeeId", userUid)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        // Traitement dans le thread secondaire
                                        processAttestationsInBackground(queryDocumentSnapshots);
                                    })
                                    .addOnFailureListener(e -> {
                                        handleLoadError(e, "ERREUR Firestore: ");
                                    });
                        })
                        .addOnFailureListener(e -> {
                            handleLoadError(e, "ERREUR Accès collection: ");
                        });

            } catch (Exception e) {
                handleLoadError(e, "Erreur générale: ");
            }
        });
    }

    private void handleLoadError(Exception e, String message) {
        isLoading = false;
        lastLoadTime = System.currentTimeMillis();
        stopRefreshAnimation();
        Log.e("ATTESTATION", message + e.getMessage());
        e.printStackTrace();
        mainHandler.post(() -> showToast("Erreur de chargement: " + e.getMessage()));
    }

    private void updateStats(int enAttente, int approuvees, int refusees) {
        runOnUiThread(() -> {
            tvAttente.setText(String.valueOf(enAttente));
            tvApprouve.setText(String.valueOf(approuvees));
            tvRefuse.setText(String.valueOf(refusees));
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

            if (dateApprobationView != null && attestation.getDateTraitement() != null) {
                String dateApprob = dateFormat.format(attestation.getDateTraitement());
                dateApprobationView.setText("Approuvée le " + dateApprob);
            }

            // Gestion du clic sur le bouton télécharger
            if (btnTelecharger != null) {
                btnTelecharger.setOnClickListener(v -> {
                    // Afficher directement "PDF non disponible"
                    showToast("PDF non disponible");
                });
            }
        }
    }

    private void setupClickListeners() {
        btnNouvelleDemande.setOnClickListener(v -> {
            v.postDelayed(() -> {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                transaction.replace(R.id.main, new AjouterAttestationFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            }, 100);
        });
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (attestationListener == null) {
            historiqueContainer.postDelayed(this::setupRealTimeListener, 100);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ne pas supprimer l'écouteur pour garder les mises à jour en temps réel
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les ressources
        if (attestationListener != null) {
            attestationListener.remove();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        historiqueContainer = null;
    }
}