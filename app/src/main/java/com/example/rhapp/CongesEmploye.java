package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.rhapp.model.Conge;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CongesEmploye extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout historiqueContainer;
    private Button btnNouvelleDemandeConge;
    private ListenerRegistration congesListener;

    private TextView soldeCongeHeader, soldeConge, congeAttente, congeApprouve, congeRefuse;
    private String currentUserEmail;
    private String currentUserId;
    private int soldeInitial = 30; // Solde initial fixe

    private static final String TAG = "CongesEmploye";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conges_employe);

        Log.d(TAG, "onCreate: Démarrage de l'activité Congés");

        try {
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentUserEmail = currentUser.getEmail();
                Log.d(TAG, "Utilisateur connecté: " + currentUserEmail);
                getCurrentUserId();
            } else {
                Log.e(TAG, "Aucun utilisateur connecté");
                Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            initializeViews();
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur d'initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "Initialisation des vues");

        try {
            soldeCongeHeader = findViewById(R.id.soldeCongeHeader);
            soldeConge = findViewById(R.id.soldeConge);
            congeAttente = findViewById(R.id.congeAttente);
            congeApprouve = findViewById(R.id.congeApprouve);
            congeRefuse = findViewById(R.id.congeRefuse);
            historiqueContainer = findViewById(R.id.historiqueContainer);
            btnNouvelleDemandeConge = findViewById(R.id.btnNouvelleDemandeConge);

            // Initialisation par défaut
            updateUIWithDefaultValues();

            Log.d(TAG, "Vues initialisées avec succès");

        } catch (Exception e) {
            Log.e(TAG, "Erreur dans initializeViews: " + e.getMessage(), e);
            throw e;
        }
    }

    private void updateUIWithDefaultValues() {
        soldeCongeHeader.setText("Solde disponible : " + soldeInitial + " jours");
        soldeConge.setText(String.valueOf(soldeInitial));
        congeAttente.setText("0");
        congeApprouve.setText("0");
        congeRefuse.setText("0");
    }

    private void setupClickListeners() {
        Log.d(TAG, "Configuration des listeners");

        try {
            // Bouton nouvelle demande
            if (btnNouvelleDemandeConge != null) {
                btnNouvelleDemandeConge.setOnClickListener(v -> {
                    try {
                        openDemandeCongeFragment();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur ouverture fragment: " + e.getMessage(), e);
                        Toast.makeText(CongesEmploye.this, "Erreur ouverture formulaire", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Navigation
            setupNavigationListener(R.id.accueil, AcceuilEmployeActivity.class, "Accueil");
            setupNavigationListener(R.id.presence, PresenceActivity.class, "Présence");
            setupNavigationListener(R.id.reunions, ReunionEmployeActivity.class, "Réunions");
            setupNavigationListener(R.id.profil, ProfileEmployeActivity.class, "Profil");

            // Pour congés, on reste sur la même page
            View congesView = findViewById(R.id.conges);
            if (congesView != null) {
                congesView.setOnClickListener(v -> {
                    Toast.makeText(CongesEmploye.this, "Vous êtes déjà sur la page Congés", Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur dans setupClickListeners: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur configuration navigation", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNavigationListener(int viewId, Class<?> destination, String pageName) {
        try {
            View view = findViewById(viewId);
            if (view != null) {
                view.setOnClickListener(v -> {
                    try {
                        if (CongesEmploye.this.getClass() != destination) {
                            startActivity(new Intent(CongesEmploye.this, destination));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur navigation " + pageName + ": " + e.getMessage(), e);
                        Toast.makeText(CongesEmploye.this, "Erreur navigation " + pageName, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur setup listener " + pageName + ": " + e.getMessage(), e);
        }
    }

    private void getCurrentUserId() {
        if (currentUserEmail == null) {
            Log.e(TAG, "Email utilisateur null");
            return;
        }

        Log.d(TAG, "Récupération ID utilisateur pour: " + currentUserEmail);

        db.collection("employees")
                .whereEqualTo("email", currentUserEmail.toLowerCase().trim())
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot employeeDoc = task.getResult().getDocuments().get(0);
                        currentUserId = employeeDoc.getId();
                        Log.d(TAG, "ID utilisateur trouvé: " + currentUserId);

                        // Récupérer le solde réel de l'employé
                        Long soldeFromDB = employeeDoc.getLong("soldeConge");
                        if (soldeFromDB != null) {
                            soldeInitial = soldeFromDB.intValue();
                            Log.d(TAG, "Solde trouvé dans DB: " + soldeInitial + " jours");
                        } else {
                            Log.w(TAG, "Solde non trouvé, utilisation de la valeur par défaut: " + soldeInitial);
                        }

                        setupFirestoreListener();
                    } else {
                        Log.e(TAG, "Profil employé non trouvé");
                        setupFirestoreListener();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur récupération ID: " + e.getMessage(), e);
                    setupFirestoreListener();
                });
    }

    private void setupFirestoreListener() {
        Log.d(TAG, "Configuration listener Firestore avec index");

        try {
            if (currentUserId != null) {
                congesListener = db.collection("conges")
                        .whereEqualTo("userId", currentUserId)
                        .orderBy("dateDemande", Query.Direction.DESCENDING)
                        .addSnapshotListener((value, error) -> {
                            if (error != null) {
                                Log.e(TAG, "Erreur Firestore: " + error.getMessage(), error);
                                showEmptyState("Erreur de chargement des données");
                                return;
                            }

                            if (value != null && !value.isEmpty()) {
                                List<Conge> congesList = value.toObjects(Conge.class);
                                Log.d(TAG, "Congés chargés avec succès: " + congesList.size() + " éléments");
                                updateCongesStatistics(congesList);
                                displayHistoriqueConges(congesList);
                            } else {
                                Log.d(TAG, "Aucun congé trouvé pour cet utilisateur");
                                updateCongesStatistics(null);
                            }
                        });
            } else {
                showEmptyState("Profil utilisateur non chargé");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur configuration Firestore: " + e.getMessage(), e);
            showEmptyState("Erreur de configuration");
        }
    }

    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            if (historiqueContainer != null) {
                historiqueContainer.removeAllViews();
                TextView emptyText = new TextView(this);
                emptyText.setText(message);
                emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                emptyText.setPadding(0, 50, 0, 0);
                historiqueContainer.addView(emptyText);
            }
        });
    }

    private void updateCongesStatistics(List<Conge> congesList) {
        try {
            int attenteCount = 0;
            int approuveCount = 0;
            int refuseCount = 0;
            int prisCount = 0;

            if (congesList != null && !congesList.isEmpty()) {
                for (Conge conge : congesList) {
                    if (conge.getStatut() != null) {
                        switch (conge.getStatut()) {
                            case "En attente":
                                attenteCount++;
                                break;
                            case "Approuvé":
                                approuveCount++;
                                prisCount += conge.getDuree();
                                break;
                            case "Refusé":
                                refuseCount++;
                                break;
                        }
                    }
                }
            }

            int soldeRestant = soldeInitial - prisCount;

            int finalAttenteCount = attenteCount;
            int finalApprouveCount = approuveCount;
            int finalRefuseCount = refuseCount;
            runOnUiThread(() -> {
                if (congeAttente != null) congeAttente.setText(String.valueOf(finalAttenteCount));
                if (congeApprouve != null) congeApprouve.setText(String.valueOf(finalApprouveCount));
                if (congeRefuse != null) congeRefuse.setText(String.valueOf(finalRefuseCount));
                if (soldeConge != null) soldeConge.setText(String.valueOf(soldeRestant));
                if (soldeCongeHeader != null) soldeCongeHeader.setText("Solde disponible : " + soldeRestant + " jours");
            });

            Log.d(TAG, "Statistiques - Solde initial: " + soldeInitial +
                    ", Jours pris: " + prisCount +
                    ", Solde restant: " + soldeRestant +
                    ", Attente: " + attenteCount +
                    ", Approuvé: " + approuveCount +
                    ", Refusé: " + refuseCount);

        } catch (Exception e) {
            Log.e(TAG, "Erreur updateCongesStatistics: " + e.getMessage(), e);
        }
    }

    private void displayHistoriqueConges(List<Conge> congesList) {
        if (historiqueContainer == null) {
            Log.e(TAG, "historiqueContainer est null");
            return;
        }

        try {
            runOnUiThread(() -> {
                historiqueContainer.removeAllViews();

                if (congesList == null || congesList.isEmpty()) {
                    showEmptyState("Aucune demande de congé trouvée");
                    return;
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH);

                for (Conge conge : congesList) {
                    try {
                        View carteView = getLayoutInflater().inflate(R.layout.item_historique_conge_card, null);

                        TextView typeConge = carteView.findViewById(R.id.TypeConge);
                        TextView statutConge = carteView.findViewById(R.id.StatutConge);
                        TextView datesConge = carteView.findViewById(R.id.DatesConge);
                        TextView dureeConge = carteView.findViewById(R.id.DureeConge);
                        TextView motifConge = carteView.findViewById(R.id.MotifConge);

                        if (typeConge != null && conge.getTypeConge() != null)
                            typeConge.setText(conge.getTypeConge());
                        if (statutConge != null && conge.getStatut() != null) {
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

                        if (datesConge != null && conge.getDateDebut() != null && conge.getDateFin() != null) {
                            String dates = dateFormat.format(conge.getDateDebut()) + " - " + dateFormat.format(conge.getDateFin());
                            datesConge.setText(dates);
                        }

                        if (dureeConge != null) {
                            dureeConge.setText(conge.getDuree() + " jours");
                        }

                        if (motifConge != null && conge.getMotif() != null) {
                            motifConge.setText(conge.getMotif());
                        }

                        historiqueContainer.addView(carteView);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur création carte congé: " + e.getMessage(), e);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Erreur displayHistoriqueConges: " + e.getMessage(), e);
        }
    }

    private void openDemandeCongeFragment() {
        try {
            DemandeCongeFragment fragment = new DemandeCongeFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Fragment demande de congé ouvert");
        } catch (Exception e) {
            Log.e(TAG, "Erreur ouverture fragment: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur ouverture formulaire", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Rafraîchissement des données");
        // Rafraîchir les données quand on revient sur cette activité
        if (currentUserId != null) {
            setupFirestoreListener();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (congesListener != null) {
            congesListener.remove();
            Log.d(TAG, "Listener Firestore détruit");
        }
    }
}