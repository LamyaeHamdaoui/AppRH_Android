package com.example.rhapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private Button btnCongeAttente, btnCongeApprouve, btnCongeRefuse;

    private static final String TAG = "CongesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conges);

        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Activité RH des congés démarrée");

        initializeViews();
        setupClickListeners();
        setupFirestoreListener();
    }

    private void initializeViews() {
        viewCongeAttente = findViewById(R.id.viewCongeAttente);
        viewCongeApprouve = findViewById(R.id.viewCongeApprouve);
        viewCongeRefuse = findViewById(R.id.viewCongeRefuse);

        btnCongeAttente = findViewById(R.id.btnCongeAttente);
        btnCongeApprouve = findViewById(R.id.btnCongeApprouve);
        btnCongeRefuse = findViewById(R.id.btnCongeRefuse);

        // Initialiser les statistiques
        updateStatistics();
    }

    private void setupClickListeners() {
        btnCongeAttente.setOnClickListener(v -> {
            Log.d(TAG, "Bouton En attente cliqué");
            showCongesByStatus("En attente");
        });
        btnCongeApprouve.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Approuvé cliqué");
            showCongesByStatus("Approuvé");
        });
        btnCongeRefuse.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Refusé cliqué");
            showCongesByStatus("Refusé");
        });
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
                        Conge conge = document.toObject(Conge.class);
                        if (conge != null) {
                            switch (conge.getStatut()) {
                                case "En attente": attente++; break;
                                case "Approuvé": approuve++; break;
                                case "Refusé": refuse++; break;
                            }
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
                    View carteView = createCongeCard(conge, statut, dateFormat);
                    container.addView(carteView);
                }
            }

            Log.d(TAG, "Affichage de " + documents.size() + " demandes " + statut);
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

    private View createCongeCard(Conge conge, String statut, SimpleDateFormat dateFormat) {
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

        // Pour les cartes en attente, ajouter les boutons d'action
        if (statut.equals("En attente")) {
            setupActionButtons(carteView, conge);
        }

        // Ajouter le click listener pour les détails
        carteView.setOnClickListener(v -> openDetailsConge(conge));

        return carteView;
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

        DetailsCongeFragment fragment = DetailsCongeFragment.newInstance(conge.getId());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment)
                .addToBackStack(null)
                .commit();
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