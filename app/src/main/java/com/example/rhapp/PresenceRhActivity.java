package com.example.rhapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

// REMARQUE: Vous aurez besoin d'importer les autres classes utilisées (e.g., History, User, etc.)
// et de déclarer vos chemins de collections (COLLECTION_USERS, COLLECTION_PRESENCE).

public class PresenceRhActivity extends AppCompatActivity {

    private static final String TAG = "PresenceRhActivity";

    // Collections Firestore (Assurez-vous que ces constantes correspondent à votre BDD)
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_PRESENCE = "PresenceHistory";

    // Références Firestore
    private FirebaseFirestore db;
    private CollectionReference usersCollection;

    // UI Elements (Dashboard)
    private TextView nbreTotalTextView;
    private TextView nbrePresentsTextView;
    private TextView nbreAbsentsTextView;
    private TextView nbreJustifiesTextView;
    private TextView nbreTauxTextView;
    private View progressFill;

    // UI Elements (Filtre et recherche)
    private TextView datePresenceTextView;
    private TextView btnTous, btnPresents, btnAbsents;
    private LinearLayout viewCongeAttenteContainer;

    // Date actuelle pour la recherche
    private String currentDateRaw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_rh); // Assurez-vous que le nom du fichier XML est correct

        // Initialisation Firestore
        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection(COLLECTION_USERS);

        // 1. Liaison des éléments UI
        linkUiElements();

        // 2. Initialisation de la date et chargement des stats
        initializeDateAndLoadStats();

        // 3. Configuration des listeners
        setupClickListeners();
    }

    private void linkUiElements() {
        // Dashboard Stats
        nbreTotalTextView = findViewById(R.id.nbreTotalConge); // Utilisé pour le total des employés
        nbrePresentsTextView = findViewById(R.id.nbreApprouveConge); // Utilisé pour les Présents
        nbreAbsentsTextView = findViewById(R.id.nbreRefuseConge); // Utilisé pour les Absents
        nbreJustifiesTextView = findViewById(R.id.nbreAttenteConge); // Utilisé pour les Justifiés

        // Taux de présence
        nbreTauxTextView = findViewById(R.id.nombreTaux);
        progressFill = findViewById(R.id.progressFill);

        // Filtre et recherche
        datePresenceTextView = findViewById(R.id.datePresence);
        btnTous = findViewById(R.id.btnCongeAttente);
        btnPresents = findViewById(R.id.btnCongeApprouve);
        btnAbsents = findViewById(R.id.btnCongeRefuse);
        viewCongeAttenteContainer = findViewById(R.id.viewCongeAttente);
    }

    private void initializeDateAndLoadStats() {
        // Définir la date par défaut à aujourd'hui
        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

        currentDateRaw = rawFormat.format(new Date());
        datePresenceTextView.setText(displayFormat.format(new Date()));

        // Charger toutes les statistiques au démarrage
        loadTotalEmployeeCount();
        loadPresenceStatsForDate(currentDateRaw);

        // Charger la liste par défaut (Tous)
        loadEmployeeListForStatus(currentDateRaw, "all");
    }

    private void setupClickListeners() {
        // Listener pour l'ouverture du DatePicker
        datePresenceTextView.setOnClickListener(v -> showDatePickerDialog());

        // Listeners pour les boutons de filtre (Tous, Présents, Absents)
        btnTous.setOnClickListener(v -> {
            updateTabSelection(btnTous);
            loadEmployeeListForStatus(currentDateRaw, "all");
        });
        btnPresents.setOnClickListener(v -> {
            updateTabSelection(btnPresents);
            loadEmployeeListForStatus(currentDateRaw, "present");
        });
        btnAbsents.setOnClickListener(v -> {
            updateTabSelection(btnAbsents);
            loadEmployeeListForStatus(currentDateRaw, "absent"); // Inclut non-justifié et justifié
        });

        // Ajoutez ici les listeners pour le footer (navigation)
    }

    private void updateTabSelection(TextView selectedButton) {
        // Réinitialiser tous les boutons (background unselected)
        btnTous.setBackgroundResource(R.drawable.button_tab_unselected);
        btnPresents.setBackgroundResource(R.drawable.button_tab_unselected);
        btnAbsents.setBackgroundResource(R.drawable.button_tab_unselected);

        // Mettre à jour le bouton sélectionné
        selectedButton.setBackgroundResource(R.drawable.button_tab_selected);
        // Note: Assurez-vous que button_tab_selected a un fond différent (blanc ou couleur primaire)
    }

    // -----------------------------------------------------
    // PARTIE 1: CALCUL DES STATISTIQUES GLOBALES
    // -----------------------------------------------------

    /**
     * Calcule le nombre total d'employés dans la collection Users.
     */
    private void loadTotalEmployeeCount() {
        usersCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = queryDocumentSnapshots.size();
            nbreTotalTextView.setText(String.valueOf(total));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erreur lors du comptage des employés: ", e);
            nbreTotalTextView.setText("?");
        });
    }

    /**
     * Calcule les statistiques de présence (Présents, Absents, Justifiés) pour une date donnée.
     * @param rawDate La date au format "yyyy-MM-dd".
     */
    private void loadPresenceStatsForDate(String rawDate) {
        db.collection(COLLECTION_PRESENCE)
                .whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int totalEmployees = parseTotalEmployees();
                    int presents = 0;
                    int absentsNonJustifies = 0;
                    int absentsJustifies = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");

                        if ("present".equals(status)) {
                            presents++;
                        } else if ("absent_justifie".equals(status) || "conge".equals(status)) {
                            absentsJustifies++;
                        } else {
                            // "absent" (non justifié)
                            absentsNonJustifies++;
                        }
                    }

                    // Calcul des Absents TOTAUX (ceux non trouvés ou non marqués)
                    // On suppose que tous les employés doivent être présents ou absents (marqués ou non)
                    int markedCount = presents + absentsJustifies + absentsNonJustifies;
                    int totalAbsents = totalEmployees - presents; // Total Absents (Justifiés + Non Justifiés + Non Enregistrés)

                    // Si on ne compte que ceux qui ont un statut 'absent' explicite
                    int absentsTotalAffichage = absentsNonJustifies + absentsJustifies;

                    // Mise à jour du Dashboard
                    nbrePresentsTextView.setText(String.valueOf(presents));
                    nbreAbsentsTextView.setText(String.valueOf(totalAbsents)); // Afficher Total Absents = Total - Présents
                    nbreJustifiesTextView.setText(String.valueOf(absentsJustifies));

                    // Mise à jour du Taux de Présence
                    updateTauxDePresence(presents, totalEmployees);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement des statistiques de présence: ", e);
                    Toast.makeText(this, "Erreur de chargement des stats.", Toast.LENGTH_SHORT).show();
                    // Réinitialiser l'affichage
                    nbrePresentsTextView.setText("0");
                    nbreAbsentsTextView.setText("0");
                    nbreJustifiesTextView.setText("0");
                    updateTauxDePresence(0, parseTotalEmployees());
                });
    }

    /**
     * Extrait le nombre total d'employés du TextView pour le calcul du taux.
     */
    private int parseTotalEmployees() {
        try {
            return Integer.parseInt(nbreTotalTextView.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // -----------------------------------------------------
    // PARTIE 2: BARRE DE PROGRESSION ET POURCENTAGE
    // -----------------------------------------------------

    /**
     * Calcule le taux de présence et met à jour l'UI (Texte et Barre).
     */
    private void updateTauxDePresence(int presents, int total) {
        if (total == 0) {
            nbreTauxTextView.setText("0.0%");
            updateProgressBar(0.0);
            return;
        }

        double taux = ((double) presents / total) * 100.0;

        nbreTauxTextView.setText(String.format(Locale.getDefault(), "%.1f%%", taux));
        updateProgressBar(taux);
    }

    /**
     * Met à jour la largeur du View progressFill pour refléter le taux.
     */
    private void updateProgressBar(double taux) {
        if (progressFill == null) return;

        // Assurez-vous que la vue parente est dessinée pour obtenir la largeur totale
        final double finalTaux = taux;
        ((View) progressFill.getParent()).post(() -> {
            View container = (View) progressFill.getParent();
            int containerWidth = container.getWidth();

            // Calculer la nouvelle largeur basée sur le taux
            int newWidth = (int) (containerWidth * (finalTaux / 100.0));

            // Appliquer la nouvelle largeur
            ViewGroup.LayoutParams layoutParams = progressFill.getLayoutParams();
            layoutParams.width = newWidth;
            progressFill.setLayoutParams(layoutParams);
        });
    }

    // -----------------------------------------------------
    // PARTIE 3: FILTRE PAR DATE (Recherche spécifique)
    // -----------------------------------------------------

    /**
     * Affiche un DatePickerDialog et gère la sélection de la date.
     */
    private void showDatePickerDialog() {
        // NOTE: Ceci est une implémentation simplifiée. Vous devriez utiliser un DatePickerDialog
        // pour une meilleure expérience utilisateur.

        Calendar calendar = Calendar.getInstance();

        // Simuler la sélection pour l'exemple
        // Pour une vraie implémentation, utilisez un DatePickerDialog
        // ... new DatePickerDialog(this, (view, year, month, dayOfMonth) -> { ... }

        Toast.makeText(this, "Fonctionnalité DatePicker à implémenter.", Toast.LENGTH_SHORT).show();

        // Simuler la mise à jour après la sélection (ex: l'utilisateur a sélectionné le 25/11/2025)
        // newDateRaw = "2025-11-25";
        // newDateDisplay = "25-Nov-2025";

        // Simuler la mise à jour si une date est sélectionnée
        // if (newDateRaw != null) {
        //     currentDateRaw = newDateRaw;
        //     datePresenceTextView.setText(newDateDisplay);
        //     loadPresenceStatsForDate(currentDateRaw);
        //     // Recharger la liste actuelle (par défaut: Tous)
        //     loadEmployeeListForStatus(currentDateRaw, getCurrentSelectedStatus());
        // }
    }

    private String getCurrentSelectedStatus() {
        if (btnPresents.getBackground().getConstantState().equals(getResources().getDrawable(R.drawable.button_tab_selected).getConstantState())) {
            return "present";
        } else if (btnAbsents.getBackground().getConstantState().equals(getResources().getDrawable(R.drawable.button_tab_selected).getConstantState())) {
            return "absent";
        } else {
            return "all";
        }
    }


    /**
     * Charge et affiche la liste des employés pour le statut et la date donnés.
     * Cette méthode doit afficher les résultats dans le conteneur 'viewCongeAttente'.
     */
    private void loadEmployeeListForStatus(String rawDate, String statusFilter) {
        viewCongeAttenteContainer.removeAllViews(); // Vider l'ancien contenu

        // 1. Définir la requête de base (toutes les présences/absences pour la date)
        CollectionReference presenceHistoryRef = db.collection(COLLECTION_PRESENCE);

        // 2. Filtrer par date
        presenceHistoryRef.whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // --- Logique pour afficher les employés filtrés (Nécessite RecyclerView/Adapter) ---

                    // Simplification: Afficher juste un message pour l'exemple
                    TextView tv = new TextView(this);
                    tv.setText(String.format("Liste des employés (%s) pour le %s chargée. (Total: %d)", statusFilter.toUpperCase(), rawDate, queryDocumentSnapshots.size()));
                    tv.setPadding(10, 10, 10, 10);
                    viewCongeAttenteContainer.addView(tv);

                    // Pour une vraie implémentation, vous devriez :
                    // 1. Récupérer les IDs des employés filtrés (present, absent, etc.)
                    // 2. Joindre les données des employés à partir de la collection 'Users'
                    // 3. Afficher ces données dans un RecyclerView.

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement de la liste filtrée: ", e);
                    Toast.makeText(this, "Impossible de charger la liste.", Toast.LENGTH_SHORT).show();
                });
    }

    // Ajoutez ici la logique de navigation du footer (navigateToActivity, etc.)
}