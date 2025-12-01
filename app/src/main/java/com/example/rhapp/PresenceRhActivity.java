package com.example.rhapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PresenceRhActivity extends AppCompatActivity {

    private static final String TAG = "PresenceRhActivity";

    // Collections Firestore
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_PRESENCE = "PresenceHistory";

    private FirebaseFirestore db;
    private CollectionReference usersCollection;

    // UI Dashboard
    private TextView nbreTotalTextView;
    private TextView nbrePresentsTextView;
    private TextView nbreAbsentsTextView;
    private TextView nbreJustifiesTextView;
    private TextView nbreTauxTextView;
    private View progressFill;

    // UI Filtre et recherche
    private TextView datePresenceTextView;
    private Spinner departementSpinner;
    private TextView btnTous, btnPresents, btnAbsents;
    private LinearLayout viewCongeAttenteContainer;

    // Variables de Filtre
    private String currentDateRaw;
    private String selectedDepartment = "Tous les départements";
    private String currentStatusFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_rh);

        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection(COLLECTION_USERS);

        // 1. Planifier la mise à jour quotidienne (IMPORTANT)
        scheduleDailyUpdate();

        // 2. Liaison des éléments UI
        linkUiElements();

        // 3. Initialisation des filtres
        initializeDate();

        // 4. Configuration des listeners
        setupClickListeners();
        setupSpinnerListener();

        // 5. Chargement initial des données
        loadTotalEmployeeCount();
    }

    private void linkUiElements() {
        // ... (Liaison des TextViews du dashboard)
        nbreTotalTextView = findViewById(R.id.nbreTotalConge);
        nbrePresentsTextView = findViewById(R.id.nbreApprouveConge);
        nbreAbsentsTextView = findViewById(R.id.nbreRefuseConge);
        nbreJustifiesTextView = findViewById(R.id.nbreAttenteConge);

        nbreTauxTextView = findViewById(R.id.nombreTaux);
        progressFill = findViewById(R.id.progressFill);

        // Filtre et recherche
        datePresenceTextView = findViewById(R.id.datePresence);
        departementSpinner = findViewById(R.id.departement);
        btnTous = findViewById(R.id.btnCongeAttente);
        btnPresents = findViewById(R.id.btnCongeApprouve);
        btnAbsents = findViewById(R.id.btnCongeRefuse);
        viewCongeAttenteContainer = findViewById(R.id.viewCongeAttente);
    }

    private void initializeDate() {
        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

        currentDateRaw = rawFormat.format(new Date());
        datePresenceTextView.setText(displayFormat.format(new Date()));

        loadPresenceStatsForDate(currentDateRaw);
        loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
    }

    private void setupClickListeners() {
        datePresenceTextView.setOnClickListener(v -> showDatePickerDialog());

        // Listeners pour les boutons de filtre
        btnTous.setOnClickListener(v -> {
            updateTabSelection(btnTous);
            currentStatusFilter = "all";
            loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
        });
        btnPresents.setOnClickListener(v -> {
            updateTabSelection(btnPresents);
            currentStatusFilter = "present";
            loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
        });
        btnAbsents.setOnClickListener(v -> {
            updateTabSelection(btnAbsents);
            currentStatusFilter = "absent";
            loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
        });

        updateTabSelection(btnTous);
    }

    private void setupSpinnerListener() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.departements_recherche, // Assurez-vous que cet array existe
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departementSpinner.setAdapter(adapter);

        departementSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = parent.getItemAtPosition(position).toString();

                loadPresenceStatsForDate(currentDateRaw);
                loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Rien à faire
            }
        });
    }

    private void updateTabSelection(TextView selectedButton) {
        btnTous.setBackgroundResource(R.drawable.button_tab_unselected);
        btnPresents.setBackgroundResource(R.drawable.button_tab_unselected);
        btnAbsents.setBackgroundResource(R.drawable.button_tab_unselected);
        selectedButton.setBackgroundResource(R.drawable.button_tab_selected);
    }

    // -----------------------------------------------------
    // LOGIQUE DE WORKMANAGER POUR LA MISE À JOUR QUOTIDIENNE À MINUIT
    // -----------------------------------------------------

    private void scheduleDailyUpdate() {
        // 1. Calculer le délai avant la prochaine minuit (00:05:00)
        Calendar midnight = Calendar.getInstance();
        midnight.setTimeInMillis(System.currentTimeMillis());

        // Si l'heure actuelle est déjà après 00:05, on planifie pour minuit le jour suivant
        if (midnight.get(Calendar.HOUR_OF_DAY) >= 0 && midnight.get(Calendar.MINUTE) >= 5) {
            midnight.add(Calendar.DAY_OF_YEAR, 1);
        }

        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 5); // 00h05
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        long delay = midnight.getTimeInMillis() - System.currentTimeMillis();

        // 2. Créer une requête UNIQUE pour assurer qu'il n'y ait qu'une seule instance active
        PeriodicWorkRequest repeatedWorkRequest =
                new PeriodicWorkRequest.Builder(
                        DailyPresenceUpdateWorker.class,
                        24, // Répéter toutes les 24 heures
                        TimeUnit.HOURS)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS) // Début à 00h05
                        .addTag("DailyPresenceRecurring")
                        .build();

        // Planifie le travail périodique (conserve l'existant s'il y en a un)
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        "PresenceDailyUpdate",
                        ExistingPeriodicWorkPolicy.KEEP,
                        repeatedWorkRequest);

        Log.i(TAG, "Tâche de mise à jour quotidienne planifiée pour la première exécution à 00h05.");
    }

    // -----------------------------------------------------
    // PARTIE 4: FILTRAGE ET AFFICHAGE DES EMPLOYÉS
    // -----------------------------------------------------

    private void loadEmployeeListForStatus(String rawDate, String statusFilter, String departmentFilter) {
        viewCongeAttenteContainer.removeAllViews();

        db.collection(COLLECTION_PRESENCE)
                .whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(presenceSnapshots -> {

                    Map<String, String> userIdToStatus = new HashMap<>();
                    for (QueryDocumentSnapshot doc : presenceSnapshots) {
                        userIdToStatus.put(doc.getString("userId"), doc.getString("status"));
                    }

                    Query usersQuery = db.collection(COLLECTION_USERS);

                    if (!"Tous les départements".equals(departmentFilter)) {
                        usersQuery = usersQuery.whereEqualTo("department", departmentFilter);
                    }

                    usersQuery.get().addOnSuccessListener(userSnapshots -> {

                        int displayCount = 0;

                        for (QueryDocumentSnapshot userDoc : userSnapshots) {
                            String userId = userDoc.getId();
                            String employeeName = userDoc.getString("name");
                            String employeeDepartment = userDoc.getString("department");

                            String presenceStatus = userIdToStatus.getOrDefault(userId, "unmarked");

                            boolean shouldDisplay = false;

                            switch (statusFilter) {
                                case "all":
                                    shouldDisplay = true;
                                    break;
                                case "present":
                                    if ("present".equals(presenceStatus)) {
                                        shouldDisplay = true;
                                    }
                                    break;
                                case "absent":
                                    if (!"present".equals(presenceStatus)) {
                                        shouldDisplay = true;
                                    }
                                    break;
                            }

                            if (shouldDisplay) {
                                displayCount++;
                                addEmployeeView(employeeName, employeeDepartment, presenceStatus);
                            }
                        }

                        if (displayCount == 0) {
                            displayNoResultsMessage();
                        }

                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors du chargement des employés: ", e);
                        Toast.makeText(PresenceRhActivity.this, "Erreur de chargement des employés.", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement de l'historique de présence: ", e);
                    Toast.makeText(PresenceRhActivity.this, "Erreur de chargement des présences.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addEmployeeView(String name, String department, String status) {
        TextView tv = new TextView(this);
        String displayStatus = formatStatus(status);
        tv.setText(String.format(Locale.getDefault(), "%s (%s) - %s", name, department, displayStatus));
        tv.setPadding(15, 15, 15, 15);
        tv.setTextSize(16f);

        tv.setBackgroundResource(R.drawable.border_gris);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        tv.setLayoutParams(params);

        viewCongeAttenteContainer.addView(tv);
    }

    private void displayNoResultsMessage() {
        TextView tv = new TextView(this);
        tv.setText("Aucun résultat trouvé pour cette date et ce filtre.");
        tv.setPadding(15, 30, 15, 30);
        tv.setTextSize(16f);
        tv.setGravity(Gravity.CENTER);
        viewCongeAttenteContainer.addView(tv);
    }

    private String formatStatus(String status) {
        switch (status) {
            case "present":
                return "Présent ✅";
            case "absent_justifie":
                return "Justifié 🟡";
            case "absent":
                return "Absent 🔴";
            case "unmarked":
            default:
                return "Non marqué ⚪";
        }
    }

    // -----------------------------------------------------
    // PARTIE 3: FILTRE PAR DATE (DatePickerDialog)
    // -----------------------------------------------------

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            Date d = displayFormat.parse(datePresenceTextView.getText().toString());
            if (d != null) c.setTime(d);
        } catch (Exception e) {
            // Utiliser la date actuelle
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

                    currentDateRaw = rawFormat.format(selectedCal.getTime());
                    datePresenceTextView.setText(displayFormat.format(selectedCal.getTime()));

                    loadPresenceStatsForDate(currentDateRaw);
                    loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
                },
                year, month, day);
        datePickerDialog.show();
    }

    // -----------------------------------------------------
    // PARTIE 1 & 2: STATISTIQUES ET BARRE DE PROGRESSION
    // -----------------------------------------------------

    private void loadTotalEmployeeCount() {
        usersCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = queryDocumentSnapshots.size();
            nbreTotalTextView.setText(String.valueOf(total));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erreur lors du comptage des employés: ", e);
            nbreTotalTextView.setText("?");
        });
    }

    private void loadPresenceStatsForDate(String rawDate) {
        // ... (Logique identique à la précédente pour le dashboard)
        db.collection(COLLECTION_PRESENCE)
                .whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int totalEmployees = parseTotalEmployees();
                    int presents = 0;
                    int absentsJustifies = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");

                        if ("present".equals(status)) {
                            presents++;
                        } else if ("absent_justifie".equals(status) || "conge".equals(status)) {
                            absentsJustifies++;
                        }
                    }

                    int totalAbsents = totalEmployees - presents;

                    nbrePresentsTextView.setText(String.valueOf(presents));
                    nbreAbsentsTextView.setText(String.valueOf(totalAbsents));
                    nbreJustifiesTextView.setText(String.valueOf(absentsJustifies));

                    updateTauxDePresence(presents, totalEmployees);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement des statistiques de présence: ", e);
                });
    }

    private int parseTotalEmployees() {
        try {
            return Integer.parseInt(nbreTotalTextView.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

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

    private void updateProgressBar(double taux) {
        if (progressFill == null) return;
        final double finalTaux = taux;
        ((View) progressFill.getParent()).post(() -> {
            View container = (View) progressFill.getParent();
            int containerWidth = container.getWidth();
            int newWidth = (int) (containerWidth * (finalTaux / 100.0));
            ViewGroup.LayoutParams layoutParams = progressFill.getLayoutParams();
            layoutParams.width = newWidth;
            progressFill.setLayoutParams(layoutParams);
        });
    }
}