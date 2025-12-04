package com.example.rhapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PresenceRhActivity extends AppCompatActivity {
    private static final String TAG = "PresenceRhActivity";
    private static final String COLLECTION_USERS = "employees";
    private static final String COLLECTION_PRESENCE = "PresenceHistory";
    private FirebaseFirestore db;
    private CollectionReference usersCollection;
    private CollectionReference presenceCollection;
    private TextView nbreTotalTextView;
    private TextView nbrePresentsTextView;
    private TextView nbreAbsentsTextView;
    private TextView nbreJustifiesTextView;
    private TextView nbreTauxTextView;
    private View progressFill;
    private TextView datePresenceTextView;
    private Spinner departementSpinner;
    private TextView btnTous, btnPresents, btnAbsents;
    private LinearLayout viewCongeAttenteContainer, footerAccueil, footerEmployes, footerConges, footerReunions, footerProfile;

    // Variables de Filtre
    private String currentDateRaw;
    private String selectedDepartment = "Tous les départements";
    private String currentStatusFilter = "all";

    // Executor pour les opérations en arrière-plan
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_rh);

        footerAccueil = findViewById(R.id.footerAccueil);
        footerEmployes = findViewById(R.id.footerEmployes);
        footerConges = findViewById(R.id.footerConges);
        footerReunions = findViewById(R.id.footerReunions);
        footerProfile = findViewById(R.id.footerProfile);

        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection(COLLECTION_USERS);
        presenceCollection = db.collection(COLLECTION_PRESENCE);

        // Liaison des éléments UI
        linkUiElements();

        // Configuration des listeners
        setupClickListeners();
        setupSpinnerListener();

        // Chargement initial des données
        initializeDateAndDataLoad();
        setupFooterNavigation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer l'executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void linkUiElements() {
        nbreTotalTextView = findViewById(R.id.nbreTotalConge);
        nbrePresentsTextView = findViewById(R.id.nbreApprouveConge);
        nbreAbsentsTextView = findViewById(R.id.nbreRefuseConge);
        nbreJustifiesTextView = findViewById(R.id.nbreAttenteConge);
        nbreTauxTextView = findViewById(R.id.nombreTaux);
        progressFill = findViewById(R.id.progressFill);

        datePresenceTextView = findViewById(R.id.datePresence);
        departementSpinner = findViewById(R.id.departement);
        btnTous = findViewById(R.id.btnCongeAttente);
        btnPresents = findViewById(R.id.btnCongeApprouve);
        btnAbsents = findViewById(R.id.btnCongeRefuse);
        viewCongeAttenteContainer = findViewById(R.id.viewCongeAttente);
    }

    private void initializeDateAndDataLoad() {
        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

        currentDateRaw = rawFormat.format(new Date());
        datePresenceTextView.setText(displayFormat.format(new Date()));

        // Charger d'abord le nombre total d'employés
        loadTotalEmployeeCount();
    }

    private void loadTotalEmployeeCount() {
        executorService.execute(() -> {
            try {
                usersCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    Log.d(TAG, "Nombre total d'employés trouvés: " + total);

                    runOnUiThread(() -> {
                        nbreTotalTextView.setText(String.valueOf(total));
                    });

                    // Une fois le total chargé, charger les statistiques de présence
                    loadPresenceStatsForDate(currentDateRaw);
                    loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du comptage des employés: ", e);
                    runOnUiThread(() -> {
                        nbreTotalTextView.setText("0");
                        Toast.makeText(PresenceRhActivity.this, "Erreur de chargement des employés", Toast.LENGTH_SHORT).show();
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "Erreur dans loadTotalEmployeeCount: ", e);
            }
        });
    }

    private void loadPresenceStatsForDate(String rawDate) {
        executorService.execute(() -> {
            try {
                int totalEmployees = parseTotalEmployees();
                Log.d(TAG, "Chargement stats pour date: " + rawDate + ", Total employés: " + totalEmployees);

                if (totalEmployees == 0) {
                    Log.w(TAG, "Aucun employé trouvé, impossible de charger les stats");
                    return;
                }

                // Rechercher les présences pour la date spécifique
                presenceCollection
                        .whereEqualTo("date", rawDate)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {

                            Log.d(TAG, "Présences trouvées pour " + rawDate + ": " + queryDocumentSnapshots.size());

                            int presents = 0;
                            int absentsJustifies = 0;
                            int conges = 0;

                            // Map pour suivre quels utilisateurs ont enregistré leur présence
                            Map<String, String> userStatusMap = new HashMap<>();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                String userId = doc.getString("userId");
                                String status = doc.getString("status");

                                if (userId != null && status != null) {
                                    userStatusMap.put(userId, status);

                                    if ("present".equals(status)) {
                                        presents++;
                                    } else if ("absent_justifie".equals(status)) {
                                        absentsJustifies++;
                                    } else if ("conge".equals(status)) {
                                        conges++;
                                    }
                                }
                            }

                            // Calculer les absents non justifiés
                            int totalMarked = presents + absentsJustifies + conges;
                            int absentsNonJustifies = totalEmployees - totalMarked;

                            Log.d(TAG, "Résultats - Présents: " + presents +
                                    ", Absents justifiés: " + absentsJustifies +
                                    ", Congés: " + conges +
                                    ", Absents non justifiés: " + absentsNonJustifies);

                            // Mettre à jour l'UI
                            int finalPresents = presents;
                            int finalAbsentsJustifies = absentsJustifies;
                            int finalConges = conges;
                            runOnUiThread(() -> {
                                nbrePresentsTextView.setText(String.valueOf(finalPresents));
                                nbreAbsentsTextView.setText(String.valueOf(absentsNonJustifies));
                                nbreJustifiesTextView.setText(String.valueOf(finalAbsentsJustifies + finalConges)); // Total justifiés + congés
                                updateTauxDePresence(finalPresents, totalEmployees);
                            });

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur lors du chargement des statistiques de présence: ", e);
                            runOnUiThread(() -> {
                                // En cas d'erreur, afficher 0 pour tout
                                nbrePresentsTextView.setText("0");
                                nbreAbsentsTextView.setText("0");
                                nbreJustifiesTextView.setText("0");
                                nbreTauxTextView.setText("0.0%");
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Erreur dans loadPresenceStatsForDate: ", e);
            }
        });
    }

    private void loadEmployeeListForStatus(String rawDate, String statusFilter, String departmentFilter) {
        Log.d(TAG, "Chargement liste employés - Date: " + rawDate +
                ", Filtre: " + statusFilter +
                ", Département: " + departmentFilter);

        executorService.execute(() -> {
            try {
                runOnUiThread(() -> {
                    viewCongeAttenteContainer.removeAllViews();
                });

                // 1. Récupérer toutes les présences pour la date
                presenceCollection
                        .whereEqualTo("date", rawDate)
                        .get()
                        .addOnSuccessListener(presenceSnapshots -> {

                            Map<String, String> userIdToStatus = new HashMap<>();

                            for (QueryDocumentSnapshot doc : presenceSnapshots) {
                                String userId = doc.getString("userId");
                                String status = doc.getString("status");

                                if (userId != null && status != null) {
                                    userIdToStatus.put(userId, status);
                                    Log.d(TAG, "Présence trouvée - UserId: " + userId + ", Status: " + status);
                                }
                            }

                            Log.d(TAG, "Total présences trouvées pour " + rawDate + ": " + userIdToStatus.size());

                            // 2. Construire la requête pour les employés
                            Query usersQuery = usersCollection;

                            if (!"Tous les départements".equals(departmentFilter)) {
                                usersQuery = usersQuery.whereEqualTo("departement", departmentFilter);
                                Log.d(TAG, "Filtrage par département: " + departmentFilter);
                            }

                            usersQuery.get().addOnSuccessListener(userSnapshots -> {

                                runOnUiThread(() -> {
                                    viewCongeAttenteContainer.removeAllViews();

                                    if (userSnapshots.isEmpty()) {
                                        displayNoResultsMessage();
                                        return;
                                    }
                                });

                                final List<EmployeeData> employeesToDisplay = new ArrayList<>();

                                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                                    String userId = userDoc.getId(); // ID du document employé
                                    String userEmail = userDoc.getString("email");
                                    String employeeName = userDoc.getString("nomComplet");
                                    String employeeDepartment = userDoc.getString("departement");
                                    String employeePoste = userDoc.getString("poste");

                                    if (employeeName == null) employeeName = "Nom Inconnu";
                                    if (employeeDepartment == null) employeeDepartment = "N/A";
                                    if (employeePoste == null) employeePoste = "Poste Inconnu";

                                    Log.d(TAG, "Traitement employé: " + employeeName + " (Email: " + userEmail + ")");

                                    // Déterminer le statut de présence
                                    String presenceStatus = "unmarked";

                                    // Chercher d'abord par email
                                    if (userEmail != null) {
                                        String statusByEmail = userIdToStatus.get(userEmail);
                                        if (statusByEmail != null) {
                                            presenceStatus = statusByEmail;
                                            Log.d(TAG, "Statut trouvé par email: " + presenceStatus);
                                        }
                                    }

                                    // Si pas trouvé par email, chercher par ID de document
                                    if ("unmarked".equals(presenceStatus)) {
                                        String statusById = userIdToStatus.get(userId);
                                        if (statusById != null) {
                                            presenceStatus = statusById;
                                            Log.d(TAG, "Statut trouvé par ID: " + presenceStatus);
                                        }
                                    }

                                    Log.d(TAG, "Statut final pour " + employeeName + ": " + presenceStatus);

                                    // Vérifier si l'employé correspond au filtre
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
                                            // Afficher tous les absents (justifiés, non justifiés, congés)
                                            if ("absent".equals(presenceStatus) ||
                                                    "absent_justifie".equals(presenceStatus) ||
                                                    "conge".equals(presenceStatus) ||
                                                    "unmarked".equals(presenceStatus)) {
                                                shouldDisplay = true;
                                            }
                                            break;
                                    }

                                    if (shouldDisplay) {
                                        employeesToDisplay.add(new EmployeeData(
                                                employeeName,
                                                employeeDepartment,
                                                employeePoste,
                                                presenceStatus
                                        ));
                                    }
                                }

                                // Afficher toutes les cartes sur le thread principal
                                runOnUiThread(() -> {
                                    if (employeesToDisplay.isEmpty()) {
                                        displayNoResultsMessage();
                                    } else {
                                        for (EmployeeData employee : employeesToDisplay) {
                                            addEmployeeCard(
                                                    employee.name,
                                                    employee.department,
                                                    employee.poste,
                                                    employee.status
                                            );
                                        }
                                    }
                                    Log.d(TAG, "Employés affichés: " + employeesToDisplay.size() + "/" + userSnapshots.size());
                                });

                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "Erreur lors du chargement des employés: ", e);
                                runOnUiThread(() -> {
                                    viewCongeAttenteContainer.removeAllViews();
                                    TextView errorText = new TextView(PresenceRhActivity.this);
                                    errorText.setText("Erreur de chargement des employés");
                                    errorText.setGravity(Gravity.CENTER);
                                    errorText.setTextColor(ContextCompat.getColor(PresenceRhActivity.this, android.R.color.holo_red_dark));
                                    errorText.setPadding(0, 50, 0, 50);
                                    viewCongeAttenteContainer.addView(errorText);
                                });
                            });

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur lors du chargement des présences: ", e);
                            runOnUiThread(() -> {
                                viewCongeAttenteContainer.removeAllViews();
                                TextView errorText = new TextView(PresenceRhActivity.this);
                                errorText.setText("Erreur de chargement des présences");
                                errorText.setGravity(Gravity.CENTER);
                                errorText.setTextColor(ContextCompat.getColor(PresenceRhActivity.this, android.R.color.holo_red_dark));
                                errorText.setPadding(0, 50, 0, 50);
                                viewCongeAttenteContainer.addView(errorText);
                            });
                        });
            } catch (Exception e) {
                Log.e(TAG, "Erreur dans loadEmployeeListForStatus: ", e);
            }
        });
    }

    // Classe interne pour stocker les données des employés
    private static class EmployeeData {
        String name;
        String department;
        String poste;
        String status;

        EmployeeData(String name, String department, String poste, String status) {
            this.name = name;
            this.department = department;
            this.poste = poste;
            this.status = status;
        }
    }

    private void addEmployeeCard(String name, String department, String poste, String status) {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            View cardView = inflater.inflate(R.layout.item_card_presence_rh, viewCongeAttenteContainer, false);

            TextView tvNomComplet = cardView.findViewById(R.id.nomComplet);
            TextView tvPoste = cardView.findViewById(R.id.poste);
            TextView tvDepartement = cardView.findViewById(R.id.departement);
            TextView tvNp = cardView.findViewById(R.id.np);

            // Remplir les données
            tvNomComplet.setText(name);

            // Afficher le poste avec le statut
            String displayStatus = formatStatus(status);
            String posteWithStatus = poste + " - " + displayStatus;
            tvPoste.setText(posteWithStatus);

            // Colorer le texte du poste selon le statut
            switch (status) {
                case "present":
                    tvPoste.setTextColor(ContextCompat.getColor(this, R.color.green));
                    break;
                case "absent_justifie":
                case "conge":
                    tvPoste.setTextColor(ContextCompat.getColor(this, R.color.orange));
                    break;
                case "absent":
                    tvPoste.setTextColor(ContextCompat.getColor(this, R.color.red));
                    break;
                default:
                    tvPoste.setTextColor(ContextCompat.getColor(this, R.color.grey));
                    break;
            }

            // Département
            tvDepartement.setText(department);

            // Initiales (colorées selon le statut)
            String initials = getInitials(name);
            tvNp.setText(initials);

            // Changer la couleur du cercle selon le statut
            switch (status) {
                case "present":
                    tvNp.setBackgroundResource(R.drawable.bg_circle_blue);
                    break;
                case "absent_justifie":
                case "conge":
                    tvNp.setBackgroundResource(R.drawable.bg_circle_blue);
                    break;
                case "absent":
                    tvNp.setBackgroundResource(R.drawable.bg_circle_blue);
                    break;
                default:
                    tvNp.setBackgroundResource(R.drawable.bg_circle_blue);
                    break;
            }

            viewCongeAttenteContainer.addView(cardView);

        } catch (Exception e) {
            Log.e(TAG, "Erreur création carte employé", e);
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "??";

        String cleanedName = fullName.trim().replaceAll("\\s+", " ");
        String[] parts = cleanedName.split(" ");

        StringBuilder initials = new StringBuilder();

        if (parts.length > 0 && !parts[0].isEmpty()) {
            initials.append(parts[0].charAt(0));
        }

        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials.append(parts[1].charAt(0));
        }

        if (initials.length() == 0) {
            return "??";
        }

        return initials.toString().toUpperCase(Locale.getDefault());
    }

    private String formatStatus(String status) {
        switch (status) {
            case "present":
                return "Présent";
            case "absent_justifie":
                return "Absence justifiée";
            case "conge":
                return "Congé";
            case "absent":
                return "Absent";
            case "unmarked":
            default:
                return "Non marqué";
        }
    }

    private void displayNoResultsMessage() {
        TextView tv = new TextView(this);
        tv.setText("Aucun employé trouvé pour ces critères.");
        tv.setPadding(15, 30, 15, 30);
        tv.setTextSize(16f);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(ContextCompat.getColor(this, R.color.grey));
        viewCongeAttenteContainer.addView(tv);
    }

    private int parseTotalEmployees() {
        try {
            String text = nbreTotalTextView.getText().toString();
            return Integer.parseInt(text);
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

        progressFill.post(() -> {
            View container = (View) progressFill.getParent();
            if (container != null) {
                int containerWidth = container.getWidth();
                int newWidth = (int) (containerWidth * (taux / 100.0));
                ViewGroup.LayoutParams layoutParams = progressFill.getLayoutParams();
                layoutParams.width = newWidth;
                progressFill.setLayoutParams(layoutParams);
            }
        });
    }

    private void setupClickListeners() {
        datePresenceTextView.setOnClickListener(v -> showDatePickerDialog());

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

    private void updateTabSelection(TextView selectedButton) {
        btnTous.setBackgroundResource(R.drawable.button_tab_unselected);
        btnPresents.setBackgroundResource(R.drawable.button_tab_unselected);
        btnAbsents.setBackgroundResource(R.drawable.button_tab_unselected);
        selectedButton.setBackgroundResource(R.drawable.button_tab_selected);
    }

    private void setupSpinnerListener() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.departements_recherche,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        departementSpinner.setAdapter(adapter);

        departementSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Département sélectionné: " + selectedDepartment);

                // Recharger avec le nouveau département
                loadPresenceStatsForDate(currentDateRaw);
                loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Rien à faire
            }
        });
    }

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
                    executorService.execute(() -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(selectedYear, selectedMonth, selectedDay);

                        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

                        currentDateRaw = rawFormat.format(selectedCal.getTime());
                        final String displayDate = displayFormat.format(selectedCal.getTime());

                        runOnUiThread(() -> {
                            datePresenceTextView.setText(displayDate);
                        });

                        Log.d(TAG, "Date sélectionnée: " + currentDateRaw);

                        // Recharger les données pour la nouvelle date
                        loadPresenceStatsForDate(currentDateRaw);
                        loadEmployeeListForStatus(currentDateRaw, currentStatusFilter, selectedDepartment);
                    });
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void setupFooterNavigation() {
        if (footerAccueil != null) {
            footerAccueil.setOnClickListener(v -> navigateToHome());
        }
        if (footerEmployes != null) {
            footerEmployes.setOnClickListener(v -> navigateToEmployees());
        }
        if (footerConges != null) {
            footerConges.setOnClickListener(v -> navigateToConges());
        }
        if (footerReunions != null) {
            footerReunions.setOnClickListener(v -> navigateToReunions());
        }
        if (footerProfile != null) {
            footerProfile.setOnClickListener(v -> navigateToProfile());
        }
    }

    private void navigateToHome() {
        startActivity(new Intent(PresenceRhActivity.this, AcceuilRhActivity.class));
    }

    private void navigateToEmployees() {
        startActivity(new Intent(PresenceRhActivity.this, EmployeActivity.class));
    }

    private void navigateToConges() {
        startActivity(new Intent(PresenceRhActivity.this, CongesActivity.class));
    }

    private void navigateToReunions() {
        startActivity(new Intent(PresenceRhActivity.this, reunionActivity.class));
    }

    private void navigateToProfile() {
        startActivity(new Intent(PresenceRhActivity.this, ProfileActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données quand l'activité revient au premier plan
        loadTotalEmployeeCount();
    }
}