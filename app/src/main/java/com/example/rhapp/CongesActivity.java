package com.example.rhapp;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.model.Conge;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CongesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration congesListener;
    private LinearLayout viewCongeAttente, viewCongeApprouve, viewCongeRefuse;
    private TextView btnCongeAttente, btnCongeApprouve, btnCongeRefuse;

    // Variables pour les éléments du footer
    private ImageView accueilIcon, employesIcon, congesIcon, reunionsIcon, profilIcon;
    private TextView accueilText, employesText, congesText, reunionsText, profilText;

    private static final String TAG = "CongesActivity";
    private static final int ACTIVE_COLOR = 0xFF4669EB; // Couleur #4669EB
    private static final int INACTIVE_COLOR = 0xFF808080; // Couleur grise

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conges);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Activité RH des congés démarrée");

        initializeViews();
        setupClickListeners();
        setupFirestoreListener();
        setupFooterNavigation();
        setActiveFooterItem(R.id.congesLayout); // Marquer Congés comme actif

        // Initialiser avec le bouton "En attente" sélectionné
        setActiveButton(btnCongeAttente);
    }

    private void initializeViews() {
        viewCongeAttente = findViewById(R.id.viewCongeAttente);
        viewCongeApprouve = findViewById(R.id.viewCongeApprouve);
        viewCongeRefuse = findViewById(R.id.viewCongeRefuse);

        btnCongeAttente = findViewById(R.id.btnCongeAttente);
        btnCongeApprouve = findViewById(R.id.btnCongeApprouve);
        btnCongeRefuse = findViewById(R.id.btnCongeRefuse);

        // Initialiser les éléments du footer
        accueilIcon = findViewById(R.id.accueilIcon);
        employesIcon = findViewById(R.id.employesIcon);
        congesIcon = findViewById(R.id.congesIcon);
        reunionsIcon = findViewById(R.id.reunionsIcon);
        profilIcon = findViewById(R.id.profilIcon);

        accueilText = findViewById(R.id.accueilText);
        employesText = findViewById(R.id.employesText);
        congesText = findViewById(R.id.congesText);
        reunionsText = findViewById(R.id.reunionsText);
        profilText = findViewById(R.id.profilText);

        // Initialiser les statistiques
        updateStatistics();
    }

    private void setupClickListeners() {
        btnCongeAttente.setOnClickListener(v -> {
            Log.d(TAG, "Bouton En attente cliqué");
            setActiveButton(btnCongeAttente);
            showCongesByStatus("En attente");
        });
        btnCongeApprouve.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Approuvé cliqué");
            setActiveButton(btnCongeApprouve);
            showCongesByStatus("Approuvé");
        });
        btnCongeRefuse.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Refusé cliqué");
            setActiveButton(btnCongeRefuse);
            showCongesByStatus("Refusé");
        });
    }

    // MÉTHODE CORRIGÉE: Gérer l'état actif des boutons
    private void setActiveButton(TextView activeButton) {
        // Réinitialiser tous les boutons
        btnCongeAttente.setBackgroundResource(R.drawable.button_tab_unselected);
        btnCongeApprouve.setBackgroundResource(R.drawable.button_tab_unselected);
        btnCongeRefuse.setBackgroundResource(R.drawable.button_tab_unselected);

        // Définir le bouton actif
        activeButton.setBackgroundResource(R.drawable.button_tab_selected);

        // Forcer le rafraîchissement de l'affichage
        activeButton.invalidate();
    }

    // Méthode pour gérer l'état actif des éléments du footer - CORRIGÉE
    private void setActiveFooterItem(int activeItemId) {
        // Réinitialiser toutes les couleurs
        resetFooterColors();

        // CORRECTION: Utiliser if/else au lieu de switch pour éviter l'erreur "Constant expression required"
        if (activeItemId == R.id.accueilLayout) {
            if (accueilIcon != null) accueilIcon.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
            if (accueilText != null) accueilText.setTextColor(ACTIVE_COLOR);
        } else if (activeItemId == R.id.employesLayout) {
            if (employesIcon != null) employesIcon.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
            if (employesText != null) employesText.setTextColor(ACTIVE_COLOR);
        } else if (activeItemId == R.id.congesLayout) {
            if (congesIcon != null) congesIcon.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
            if (congesText != null) congesText.setTextColor(ACTIVE_COLOR);
        } else if (activeItemId == R.id.reunionsLayout) {
            if (reunionsIcon != null) reunionsIcon.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
            if (reunionsText != null) reunionsText.setTextColor(ACTIVE_COLOR);
        } else if (activeItemId == R.id.profilLayout) {
            if (profilIcon != null) profilIcon.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
            if (profilText != null) profilText.setTextColor(ACTIVE_COLOR);
        }
    }

    // Réinitialiser toutes les couleurs du footer
    private void resetFooterColors() {
        if (accueilIcon != null) accueilIcon.setColorFilter(INACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
        if (employesIcon != null) employesIcon.setColorFilter(INACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
        if (congesIcon != null) congesIcon.setColorFilter(INACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
        if (reunionsIcon != null) reunionsIcon.setColorFilter(INACTIVE_COLOR, PorterDuff.Mode.SRC_IN);
        if (profilIcon != null) profilIcon.setColorFilter(INACTIVE_COLOR, PorterDuff.Mode.SRC_IN);

        if (accueilText != null) accueilText.setTextColor(INACTIVE_COLOR);
        if (employesText != null) employesText.setTextColor(INACTIVE_COLOR);
        if (congesText != null) congesText.setTextColor(INACTIVE_COLOR);
        if (reunionsText != null) reunionsText.setTextColor(INACTIVE_COLOR);
        if (profilText != null) profilText.setTextColor(INACTIVE_COLOR);
    }

    private void setupFirestoreListener() {
        Log.d(TAG, "Configuration du listener Firestore");

        congesListener = db.collection("conges")
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur Firestore: " + error.getMessage());
                        Toast.makeText(this, "Erreur de connexion à la base de données", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Données reçues: " + value.size() + " documents");
                        updateStatistics();
                        // Recharger la vue actuelle
                        String currentStatus = getCurrentVisibleStatus();
                        if (currentStatus != null) {
                            showCongesByStatus(currentStatus);
                        } else {
                            showCongesByStatus("En attente");
                        }
                    } else {
                        Log.d(TAG, "Aucune donnée trouvée dans Firestore");
                        // Afficher un message quand il n'y a pas de données
                        showNoDataMessage();
                    }
                });
    }

    // NAVIGATION DU FOOTER
    private void setupFooterNavigation() {
        // Accueil
        LinearLayout accueilLayout = findViewById(R.id.accueilLayout);
        if (accueilLayout != null) {
            accueilLayout.setOnClickListener(v -> {
                setActiveFooterItem(R.id.accueilLayout);
                Intent intent = new Intent(CongesActivity.this, AcceuilRhActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.navigation.slide_in_right, R.navigation.slide_out_left);
            });
        }

        // Employés
        LinearLayout employesLayout = findViewById(R.id.employesLayout);
        if (employesLayout != null) {
            employesLayout.setOnClickListener(v -> {
                setActiveFooterItem(R.id.employesLayout);
                Intent intent = new Intent(CongesActivity.this, EmployeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.navigation.slide_in_right, R.navigation.slide_out_left);
            });
        }

        // Congés (actuel - désactivé ou highlighté)
        LinearLayout congesLayout = findViewById(R.id.congesLayout);
        if (congesLayout != null) {
            congesLayout.setOnClickListener(v -> {
                setActiveFooterItem(R.id.congesLayout);
                // Déjà sur la page congés, on peut soit ne rien faire soit afficher un message
                Toast.makeText(CongesActivity.this, "Vous êtes déjà sur la page Congés", Toast.LENGTH_SHORT).show();
            });
        }

        // Réunions
        LinearLayout reunionsLayout = findViewById(R.id.reunionsLayout);
        if (reunionsLayout != null) {
            reunionsLayout.setOnClickListener(v -> {
                setActiveFooterItem(R.id.reunionsLayout);
                Intent intent = new Intent(CongesActivity.this, reunionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.navigation.slide_in_right, R.navigation.slide_out_left);
            });
        }

        // Profil
        LinearLayout profilLayout = findViewById(R.id.profilLayout);
        if (profilLayout != null) {
            profilLayout.setOnClickListener(v -> {
                setActiveFooterItem(R.id.profilLayout);
                Intent intent = new Intent(CongesActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(R.navigation.slide_in_right, R.navigation.slide_out_left);
            });
        }
    }

    private void showNoDataMessage() {
        runOnUiThread(() -> {
            viewCongeAttente.removeAllViews();
            viewCongeApprouve.removeAllViews();
            viewCongeRefuse.removeAllViews();

            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune demande de congé trouvée");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(0, 50, 0, 0);
            viewCongeAttente.addView(emptyText);
            viewCongeAttente.setVisibility(View.VISIBLE);
        });
    }

    private String getCurrentVisibleStatus() {
        if (viewCongeAttente.getVisibility() == View.VISIBLE) return "En attente";
        if (viewCongeApprouve.getVisibility() == View.VISIBLE) return "Approuvé";
        if (viewCongeRefuse.getVisibility() == View.VISIBLE) return "Refusé";
        return null;
    }

    private void updateStatistics() {
        Log.d(TAG, "Mise à jour des statistiques");

        db.collection("conges").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                    Log.d(TAG, "Statistiques: " + documents.size() + " documents trouvés");

                    int total = documents.size();
                    int attente = 0, approuve = 0, refuse = 0;

                    for (DocumentSnapshot document : documents) {
                        // CORRECTION: Vérifier si le document existe et a des données
                        if (document.exists() && document.getData() != null) {
                            Conge conge = document.toObject(Conge.class);

                            if (conge != null && conge.getStatut() != null) {
                                String statut = conge.getStatut();

                                // CORRECTION: Utiliser if/else au lieu de switch pour éviter l'erreur de null
                                if (statut.equals("En attente")) {
                                    attente++;
                                } else if (statut.equals("Approuvé") || statut.equals("Approuvée")) {
                                    approuve++;
                                } else if (statut.equals("Refusé") || statut.equals("Refusée")) {
                                    refuse++;
                                } else {
                                    Log.d(TAG, "Statut inconnu: " + statut);
                                }
                            } else {
                                Log.d(TAG, "Document " + document.getId() + ": Congé null ou statut null");
                            }
                        } else {
                            Log.d(TAG, "Document " + document.getId() + " n'existe pas ou n'a pas de données");
                        }
                    }

                    updateStatisticsUI(total, attente, approuve, refuse);
                } else {
                    Log.d(TAG, "QuerySnapshot est null");
                    updateStatisticsUI(0, 0, 0, 0);
                }
            } else {
                Log.e(TAG, "Erreur statistiques: " + task.getException());
                Toast.makeText(this, "Erreur lors du chargement des statistiques", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateStatisticsUI(int total, int attente, int approuve, int refuse) {
        runOnUiThread(() -> {
            TextView nbreTotal = findViewById(R.id.nbreTotalConge);
            TextView nbreAttente = findViewById(R.id.nbreAttenteConge);
            TextView nbreApprouve = findViewById(R.id.nbreApprouveConge);
            TextView nbreRefuse = findViewById(R.id.nbreRefuseConge);

            if (nbreTotal != null) nbreTotal.setText(String.valueOf(total));
            if (nbreAttente != null) nbreAttente.setText(String.valueOf(attente));
            if (nbreApprouve != null) nbreApprouve.setText(String.valueOf(approuve));
            if (nbreRefuse != null) nbreRefuse.setText(String.valueOf(refuse));

            Log.d(TAG, "Statistiques mises à jour - Total: " + total + ", Attente: " + attente + ", Approuvé: " + approuve + ", Refusé: " + refuse);
        });
    }

    private void showCongesByStatus(String statut) {
        Log.d(TAG, "Chargement des congés avec statut: " + statut);

        // Masquer toutes les vues
        viewCongeAttente.setVisibility(View.GONE);
        viewCongeApprouve.setVisibility(View.GONE);
        viewCongeRefuse.setVisibility(View.GONE);

        // Afficher un indicateur de chargement
        showLoadingIndicator(statut);

        // Charger les congés selon le statut
        db.collection("conges")
                .whereEqualTo("statut", statut)
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                            Log.d(TAG, statut + " - " + documents.size() + " documents trouvés");
                            displayConges(documents, statut);
                        } else {
                            Log.d(TAG, "QuerySnapshot null pour statut: " + statut);
                            displayEmptyState(statut);
                        }
                    } else {
                        Log.e(TAG, "Erreur chargement " + statut + ": " + task.getException());
                        displayErrorState(statut, task.getException().getMessage());
                    }
                });
    }

    private void showLoadingIndicator(String statut) {
        runOnUiThread(() -> {
            LinearLayout container = getContainerByStatus(statut);
            if (container != null) {
                container.removeAllViews();

                TextView loadingText = new TextView(this);
                loadingText.setText("Chargement des demandes " + statut.toLowerCase() + "...");
                loadingText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                loadingText.setPadding(0, 50, 0, 0);
                container.addView(loadingText);
                container.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayConges(List<DocumentSnapshot> documents, String statut) {
        runOnUiThread(() -> {
            LinearLayout container = getContainerByStatus(statut);
            if (container == null) return;

            container.removeAllViews();
            container.setVisibility(View.VISIBLE);

            if (documents.isEmpty()) {
                TextView emptyText = new TextView(this);
                emptyText.setText("Aucune demande " + statut.toLowerCase());
                emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                emptyText.setPadding(0, 50, 0, 0);
                container.addView(emptyText);
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH);

            for (DocumentSnapshot document : documents) {
                Conge conge = document.toObject(Conge.class);
                if (conge != null) {
                    // Définir l'ID du congé
                    conge.setId(document.getId());

                    // Calculer le solde actuel en temps réel
                    calculerEtAfficherSoldeActuel(conge, container, statut, dateFormat);
                }
            }

            Log.d(TAG, "Affichage de " + documents.size() + " demandes " + statut);
        });
    }

    private void calculerEtAfficherSoldeActuel(Conge conge, LinearLayout container, String statut, SimpleDateFormat dateFormat) {
        // Récupérer le solde initial de l'employé
        db.collection("employees")
                .document(conge.getUserId())
                .get()
                .addOnCompleteListener(employeeTask -> {
                    if (employeeTask.isSuccessful() && employeeTask.getResult() != null) {
                        DocumentSnapshot employeeDoc = employeeTask.getResult();
                        Integer soldeInitial = employeeDoc.getLong("soldeConge") != null ?
                                employeeDoc.getLong("soldeConge").intValue() : 30;

                        // Calculer le solde actuel en temps réel
                        calculerSoldeActuelReel(conge.getUserId(), soldeInitial, conge, container, statut, dateFormat);
                    } else {
                        // En cas d'erreur, utiliser le solde stocké dans le congé
                        afficherCarteConge(conge, container, statut, dateFormat, conge.getSoldeActuel());
                    }
                })
                .addOnFailureListener(e -> {
                    // En cas d'erreur, utiliser le solde stocké dans le congé
                    afficherCarteConge(conge, container, statut, dateFormat, conge.getSoldeActuel());
                });
    }

    private void calculerSoldeActuelReel(String userId, int soldeInitial, Conge conge, LinearLayout container, String statut, SimpleDateFormat dateFormat) {
        // Récupérer tous les congés approuvés de cet employé
        db.collection("conges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("statut", "Approuvé")
                .get()
                .addOnCompleteListener(congesTask -> {
                    int totalJoursPris = 0;

                    if (congesTask.isSuccessful() && congesTask.getResult() != null) {
                        for (DocumentSnapshot doc : congesTask.getResult().getDocuments()) {
                            // Ne pas compter le congé actuel s'il est en attente
                            if (!doc.getId().equals(conge.getId()) || !conge.getStatut().equals("En attente")) {
                                Integer duree = doc.getLong("duree") != null ? doc.getLong("duree").intValue() : 0;
                                totalJoursPris += duree;
                            }
                        }
                    }

                    int soldeActuelReel = soldeInitial - totalJoursPris;

                    // Mettre à jour le solde dans l'objet Conge
                    conge.setSoldeActuel(soldeActuelReel);

                    // Afficher la carte avec le solde actualisé
                    afficherCarteConge(conge, container, statut, dateFormat, soldeActuelReel);
                })
                .addOnFailureListener(e -> {
                    // En cas d'erreur, utiliser le solde stocké
                    afficherCarteConge(conge, container, statut, dateFormat, conge.getSoldeActuel());
                });
    }

    private void afficherCarteConge(Conge conge, LinearLayout container, String statut, SimpleDateFormat dateFormat, int soldeActuel) {
        runOnUiThread(() -> {
            int layoutRes;
            switch (statut) {
                case "En attente": layoutRes = R.layout.item_card_congeenttente; break;
                case "Approuvé": layoutRes = R.layout.item_card_congeapprouve; break;
                case "Refusé": layoutRes = R.layout.item_card_congerefuse; break;
                default: layoutRes = R.layout.item_card_congeenttente;
            }

            View carteView = getLayoutInflater().inflate(layoutRes, null);

            // Remplir les données communes
            TextView nomConge = carteView.findViewById(R.id.nomConge);
            TextView departement = carteView.findViewById(R.id.DepartementConge);
            TextView typeCongeView = carteView.findViewById(R.id.typeConge);
            TextView datesConge = carteView.findViewById(R.id.datesConge);
            TextView dureeConge = carteView.findViewById(R.id.DureeConge);
            TextView motifConge = carteView.findViewById(R.id.MotifConge);
            TextView soldeActuelText = carteView.findViewById(R.id.soldeActuelText);

            // Utiliser les données réelles du congé, jamais de données d'exemple
            if (nomConge != null) nomConge.setText(conge.getUserName() != null ? conge.getUserName() : "Nom non disponible");
            if (departement != null) departement.setText(conge.getUserDepartment() != null ? conge.getUserDepartment() : "Département non disponible");
            if (typeCongeView != null) typeCongeView.setText(conge.getTypeConge() != null ? conge.getTypeConge() : "Type non disponible");
            if (datesConge != null) {
                String dateDebut = conge.getDateDebut() != null ? dateFormat.format(conge.getDateDebut()) : "Date non disponible";
                String dateFin = conge.getDateFin() != null ? dateFormat.format(conge.getDateFin()) : "Date non disponible";
                datesConge.setText(dateDebut + " - " + dateFin);
            }
            if (dureeConge != null) {
                dureeConge.setText(conge.getDuree() + " jours");
            }
            if (motifConge != null) {
                motifConge.setText(conge.getMotif() != null ? conge.getMotif() : "Motif non spécifié");
            }
            if (soldeActuelText != null) {
                soldeActuelText.setText(soldeActuel + " jours");
            }

            // Pour les cartes en attente, ajouter les boutons d'action
            if (statut.equals("En attente")) {
                setupActionButtons(carteView, conge);
            }

            // Ajouter le click listener pour les détails
            carteView.setOnClickListener(v -> openDetailsConge(conge));

            // Ajouter un espacement entre les cartes
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 16); // Espacement de 16dp en bas
            carteView.setLayoutParams(layoutParams);

            container.addView(carteView);
        });
    }

    private void displayEmptyState(String statut) {
        runOnUiThread(() -> {
            LinearLayout container = getContainerByStatus(statut);
            if (container != null) {
                container.removeAllViews();

                TextView emptyText = new TextView(this);
                emptyText.setText("Aucune demande " + statut.toLowerCase());
                emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                emptyText.setPadding(0, 50, 0, 0);
                container.addView(emptyText);
                container.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayErrorState(String statut, String errorMessage) {
        runOnUiThread(() -> {
            LinearLayout container = getContainerByStatus(statut);
            if (container != null) {
                container.removeAllViews();

                TextView errorText = new TextView(this);
                errorText.setText("Erreur de chargement: " + errorMessage);
                errorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                errorText.setPadding(0, 50, 0, 0);
                errorText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                container.addView(errorText);
                container.setVisibility(View.VISIBLE);
            }

            Toast.makeText(this, "Erreur lors du chargement des demandes " + statut.toLowerCase(), Toast.LENGTH_SHORT).show();
        });
    }

    private LinearLayout getContainerByStatus(String statut) {
        switch (statut) {
            case "En attente": return viewCongeAttente;
            case "Approuvé": return viewCongeApprouve;
            case "Refusé": return viewCongeRefuse;
            default: return null;
        }
    }

    private void setupActionButtons(View carteView, Conge conge) {
        View btnDetails = carteView.findViewById(R.id.btnDetaileConge);
        View btnAccept = carteView.findViewById(R.id.acceptConge);
        View btnRefuse = carteView.findViewById(R.id.refuseConge);

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> openDetailsConge(conge));
        }

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> updateCongeStatus(conge, "Approuvé", ""));
        }

        if (btnRefuse != null) {
            btnRefuse.setOnClickListener(v -> {
                updateCongeStatus(conge, "Refusé", "Raison non spécifiée");
            });
        }
    }

    private void updateCongeStatus(Conge conge, String nouveauStatut, String raison) {
        if (conge.getId() == null) {
            Toast.makeText(this, "Erreur: ID du congé non défini", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Mise à jour du congé " + conge.getId() + " vers statut: " + nouveauStatut);

        DocumentReference docRef = db.collection("conges").document(conge.getId());

        docRef.update("statut", nouveauStatut)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String message = "Demande " + nouveauStatut.toLowerCase() + " avec succès";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Statut mis à jour avec succès");

                        // Mettre à jour l'affichage
                        updateStatistics();
                        showCongesByStatus("En attente");
                    } else {
                        String errorMsg = "Erreur lors de la mise à jour: " + task.getException().getMessage();
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, errorMsg);
                    }
                });
    }

    private void openDetailsConge(Conge conge) {
        if (conge.getId() == null) {
            Toast.makeText(this, "Erreur: Impossible d'ouvrir les détails - ID manquant", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Ouverture des détails pour le congé: " + conge.getId());

        try {
            DetailsCongeFragment fragment = DetailsCongeFragment.newInstance(conge.getId());

            // Utiliser add() au lieu de replace() pour superposer le fragment
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main, fragment)
                    .addToBackStack("details_conge")
                    .commit();

            Log.d(TAG, "Fragment de détails ajouté avec succès");
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture des détails: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'ouverture des détails", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (congesListener != null) {
            congesListener.remove();
            Log.d(TAG, "Listener Firestore supprimé");
        }
    }
}