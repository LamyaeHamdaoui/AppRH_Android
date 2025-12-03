package com.example.rhapp;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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
    private static final String COLLECTION_USERS = "employees";
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
    private LinearLayout viewCongeAttenteContainer, footerAccueil, footerEmployes, footerConges, footerReunions, footerProfile;

    // Variables de Filtre
    private String currentDateRaw;
    private String selectedDepartment = "Tous les départements";
    private String currentStatusFilter = "all";

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

        // Utiliser la collection 'employees'
        usersCollection = db.collection(COLLECTION_USERS);

        // 1. Planifier la mise à jour quotidienne (IMPORTANT)
        scheduleDailyUpdate();

        // 2. Liaison des éléments UI
        linkUiElements();

        // 3. Configuration des listeners
        setupClickListeners();
        setupSpinnerListener();

        // 4. Chargement initial des données
        loadTotalEmployeeCountAndProceed();
        setupFooterNavigation();
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

    private void loadTotalEmployeeCountAndProceed() {
        usersCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int total = queryDocumentSnapshots.size();
            nbreTotalTextView.setText(String.valueOf(total));

            // ********* Appel clé : Continuer le chargement *********
            initializeDateAndDataLoad();

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erreur lors du comptage des employés: ", e);
            nbreTotalTextView.setText("?");
            initializeDateAndDataLoad();
        });
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

    private void initializeDateAndDataLoad() {
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

    // [omitted scheduleDailyUpdate for brevity, assuming Worker class exists]
    private void scheduleDailyUpdate() {
        Calendar midnight = Calendar.getInstance();
        midnight.setTimeInMillis(System.currentTimeMillis());

        Calendar targetTime = Calendar.getInstance();
        targetTime.set(Calendar.HOUR_OF_DAY, 0);
        targetTime.set(Calendar.MINUTE, 5);
        targetTime.set(Calendar.SECOND, 0);
        targetTime.set(Calendar.MILLISECOND, 0);

        if (targetTime.before(Calendar.getInstance())) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        long delay = targetTime.getTimeInMillis() - System.currentTimeMillis();

        PeriodicWorkRequest repeatedWorkRequest =
                new PeriodicWorkRequest.Builder(
                        DailyPresenceUpdateWorker.class,
                        24,
                        TimeUnit.HOURS)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag("DailyPresenceRecurring")
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        "PresenceDailyUpdate",
                        ExistingPeriodicWorkPolicy.KEEP,
                        repeatedWorkRequest);

        Log.i(TAG, "Tâche de mise à jour quotidienne planifiée.");
    }


    // -----------------------------------------------------
    // PARTIE 4: FILTRAGE ET AFFICHAGE DES EMPLOYÉS (CORRIGÉE pour utiliser l'email)
    // -----------------------------------------------------

    private void loadEmployeeListForStatus(String rawDate, String statusFilter, String departmentFilter) {
        viewCongeAttenteContainer.removeAllViews();

        // 1. Récupérer les statuts de présence pour la date sélectionnée
        db.collection(COLLECTION_PRESENCE)
                .whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(presenceSnapshots -> {

                    // CLE: userId (qui semble être l'email de l'employé dans l'app d'enregistrement) -> VALEUR: status
                    Map<String, String> userIdToStatus = new HashMap<>();

                    for (QueryDocumentSnapshot doc : presenceSnapshots) {
                        String status = doc.getString("status");
                        String userId = doc.getString("userId");

                        if (userId != null) {
                            // On stocke le statut en utilisant le userId de l'enregistrement de présence
                            userIdToStatus.put(userId, status);
                        }
                    }

                    // 2. Récupérer tous les employés (filtrés par Département si besoin)
                    Query usersQuery = db.collection(COLLECTION_USERS);

                    if (!"Tous les départements".equals(departmentFilter)) {
                        usersQuery = usersQuery.whereEqualTo("departement", departmentFilter);
                    }

                    usersQuery.get().addOnSuccessListener(userSnapshots -> {

                        int displayCount = 0;

                        for (QueryDocumentSnapshot userDoc : userSnapshots) {
                            // On tente d'utiliser l'email comme clé de jointure, car il est unique et présent dans 'employees'
                            String employeeIdentifier = userDoc.getString("email");

                            String employeeName = userDoc.getString("nomComplet");
                            String employeeDepartment = userDoc.getString("departement");
                            String employeePoste = userDoc.getString("poste");

                            if (employeeName == null) employeeName = "Nom Inconnu";
                            if (employeeDepartment == null) employeeDepartment = "N/A";
                            if (employeePoste == null) employeePoste = "Poste Inconnu";

                            // DÉBUT DE LA CORRECTION : Tenter de trouver le statut
                            String presenceStatus = "unmarked";

                            // Tenter de trouver le statut en utilisant l'identifiant (email) comme clé
                            if (employeeIdentifier != null) {
                                String statusByEmail = userIdToStatus.get(employeeIdentifier);
                                if (statusByEmail != null) {
                                    presenceStatus = statusByEmail;
                                }
                            }
                            // Si l'email n'a pas marché (mais l'ID du document est utilisé comme userId)
                            // La vérification de l'ID du document est conservée en dernier recours
                            if ("unmarked".equals(presenceStatus)) {
                                String documentId = userDoc.getId(); // L'ID du document est l'ID de l'utilisateur RH
                                String statusById = userIdToStatus.get(documentId);
                                if (statusById != null) {
                                    presenceStatus = statusById;
                                }
                            }
                            // FIN DE LA CORRECTION : Tenter de trouver le statut

                            boolean shouldDisplay = false;

                            // 3. Appliquer le filtre de statut
                            switch (statusFilter) {
                                case "all":
                                    // Affiche tous les employés, quel que soit leur statut
                                    shouldDisplay = true;
                                    break;
                                case "present":
                                    if ("present".equals(presenceStatus)) {
                                        shouldDisplay = true;
                                    }
                                    break;
                                case "absent":
                                    // Affiche tous ceux qui ne sont PAS "present".
                                    if (!"present".equals(presenceStatus)) {
                                        shouldDisplay = true;
                                    }
                                    break;
                            }

                            if (shouldDisplay) {
                                displayCount++;

                                // Appel à la fonction d'affichage de la carte
                                addEmployeeCard(employeeName, employeeDepartment, employeePoste, presenceStatus);
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

    /**
     * NOUVELLE MÉTHODE: Remplace l'ancien addEmployeeView
     * Utilise le layout item_card_presence_rh.xml pour l'affichage.
     */
    private void addEmployeeCard(String name, String department, String poste, String status) {
        // 1. Gonfler le layout de la carte
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_card_presence_rh, viewCongeAttenteContainer, false);

        // 2. Lier les éléments UI de la carte
        TextView tvNomComplet = cardView.findViewById(R.id.nomComplet);
        TextView tvPoste = cardView.findViewById(R.id.poste);
        TextView tvDepartement = cardView.findViewById(R.id.departement);
        TextView tvNp = cardView.findViewById(R.id.np);

        // 3. Remplir les données
        tvNomComplet.setText(name);
        tvPoste.setText(poste);
        tvDepartement.setText(department);

        // Initiales pour le cercle (Première lettre du nom et première lettre du prénom)
        String initials = getInitials(name);
        tvNp.setText(initials);

        // 4. Mettre à jour la couleur du département et gérer le statut de présence

        // Pour des besoins de démonstration, nous mettons le statut dans le poste
        String displayStatus = formatStatus(status);
        tvPoste.setText(String.format(Locale.getDefault(), "%s (%s)", poste, displayStatus));


        // 5. Ajouter la carte au conteneur
        viewCongeAttenteContainer.addView(cardView);
    }

    /**
     * Fonction utilitaire pour obtenir les initiales.
     * Prend la première lettre du premier mot et la première lettre du deuxième mot.
     * Exemple : "ELAAMMARI Oumeyma" -> "EO"
     */
    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "??";

        // Nettoyer la chaîne et la séparer par des espaces
        String cleanedName = fullName.trim().replaceAll("\\s+", " ");
        String[] parts = cleanedName.split(" ");

        StringBuilder initials = new StringBuilder();

        // Récupérer la première lettre du premier mot (Prénom/Nom)
        if (parts.length > 0 && !parts[0].isEmpty()) {
            initials.append(parts[0].charAt(0));
        }

        // Récupérer la première lettre du deuxième mot (Nom/Prénom)
        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials.append(parts[1].charAt(0));
        }

        // Si un seul mot, on garde juste la première lettre
        if (initials.length() == 0) {
            return "??";
        }

        return initials.toString().toUpperCase(Locale.getDefault());
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

    private void loadPresenceStatsForDate(String rawDate) {

        int totalEmployees = parseTotalEmployees();
        if (totalEmployees == 0) return;

        int finalTotalEmployees = totalEmployees;
        db.collection(COLLECTION_PRESENCE)
                .whereEqualTo("date", rawDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int presents = 0;
                    int absentsJustifies = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");

                        if ("present".equals(status)) {
                            presents++;
                        }
                        else if ("absent_justifie".equals(status) || "conge".equals(status)) {
                            absentsJustifies++;
                        }
                    }

                    int totalAbsents = finalTotalEmployees - presents;
                    int absentsNonJustifies = totalAbsents - absentsJustifies;

                    nbrePresentsTextView.setText(String.valueOf(presents));
                    nbreAbsentsTextView.setText(String.valueOf(absentsNonJustifies));
                    nbreJustifiesTextView.setText(String.valueOf(absentsJustifies));

                    updateTauxDePresence(presents, finalTotalEmployees);

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

    // Vous devez avoir une classe DailyPresenceUpdateWorker pour que ceci fonctionne
    @SuppressLint("WorkerHasAPublicModifier")
    private static class DailyPresenceUpdateWorker extends androidx.work.Worker {
        public DailyPresenceUpdateWorker(android.content.Context context, androidx.work.WorkerParameters params) {
            super(context, params);
        }

        @Override
        public androidx.work.ListenableWorker.Result doWork() {
            // Logique de mise à jour quotidienne (par exemple, marquer les non-présents comme "absent")
            Log.i("DailyWorker", "Tâche quotidienne de présence exécutée.");
            return androidx.work.ListenableWorker.Result.success();
        }
    }
}