package com.example.rhapp;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcceuilEmployeActivity extends AppCompatActivity {
    private static final String TAG = "AcceuilEmployeActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Views
    private LinearLayout presencefooter, congesfooter, reunionsfooter, profilefooter, attestation;
    private LinearLayout presence, conges, reunions, actionConge, actionPresence, actionReunions, notifRecentesContainer;
    private TextView etatPrsence, soldeConge, reunionVenir, notifPresence, notifConge, notifAttestation, notifReunion, employeConnecte, aucuneNotif;
    private TextView posteDepartement;
    private static final String PREF_NAME = "AcceuilPresencePrefs";
    private static final String KEY_LAST_DAY = "last_day";
    private static final String KEY_PRESENCE_MARKED_TODAY = "presence_marked_today";
    private static final String KEY_EMPLOYEE_CACHE = "employee_cache";
    private static final String KEY_NOTIFICATIONS_CACHE = "notifications_cache";
    private static final String KEY_NOTIFICATIONS_TIMESTAMP = "notifications_timestamp";

    // Écouteurs Firestore TEMPS RÉEL
    private ListenerRegistration congesListener;
    private ListenerRegistration attestationsListener;
    private ListenerRegistration reunionsListener;
    private ListenerRegistration presenceListener;
    private ListenerRegistration employeListener;

    // Thread management
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Debouncing pour éviter les rechargements trop fréquents
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_DELAY = 2000; // 2 secondes
    private Runnable debouncedUpdateTask;

    // Données de l'employé
    private String employeeId = "";
    private String employeeNom = "";
    private String employeePrenom = "";
    private String employeePoste = "";
    private String employeeDepartement = "";
    private int soldeCongeValue = 0;
    private ImageView iconepresence;

    // Classe NotificationItem INTERNE
    private static class NotificationItem {
        String title;
        String subtitle;
        int iconRes;
        String type;
        String statut;
        Date date;

        NotificationItem(String title, String subtitle, int iconRes, String type, String statut, Date date) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.type = type;
            this.statut = statut;
            this.date = date;
        }
    }

    // Classe ReunionNotification INTERNE
    private static class ReunionNotification {
        String titre;
        String dateStr;
        String heure;
        String lieu;
        Date date;

        ReunionNotification(String titre, String dateStr, String heure, String lieu, Date date) {
            this.titre = titre;
            this.dateStr = dateStr;
            this.heure = heure;
            this.lieu = lieu;
            this.date = date;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_employe);

        Log.d(TAG, "=== DÉBUT onCreate ===");

        initializeFirebase();
        initializeViews();
        setupNavigation();

        // Vérifier et réinitialiser l'état à minuit
        checkAndResetForNewDay();

        // Afficher les données en cache IMMÉDIATEMENT
        loadCachedData();
        loadCachedNotifications();

        // Configurer TOUS les écouteurs Firestore temps réel
        setupAllRealtimeListeners();
        testFirestoreDataStructure();
    }

    private void loadCachedData() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String cachedData = prefs.getString(KEY_EMPLOYEE_CACHE, "");

        if (!cachedData.isEmpty()) {
            String[] parts = cachedData.split("\\|");
            if (parts.length >= 5) {
                employeePrenom = parts[0];
                employeeNom = parts[1];
                employeePoste = parts[2];
                employeeDepartement = parts[3];
                soldeCongeValue = Integer.parseInt(parts[4]);

                // Mettre à jour l'UI IMMÉDIATEMENT
                runOnUiThread(() -> {
                    updateEmployeUI();
                    updateSoldeCongeUI();
                });
                Log.d(TAG, "Données employé affichées depuis le cache");
            }
        } else {
            // Afficher un message temporaire si pas de cache
            runOnUiThread(() -> {
                if (employeConnecte != null) {
                    employeConnecte.setText("Chargement...");
                }
            });
        }
    }

    private void saveToCache() {
        String cacheData = employeePrenom + "|" + employeeNom + "|" +
                employeePoste + "|" + employeeDepartement + "|" + soldeCongeValue;

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_EMPLOYEE_CACHE, cacheData);
        editor.apply();

        Log.d(TAG, "Données employé sauvegardées en cache");
    }

    private void checkAndResetForNewDay() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastDay = prefs.getString(KEY_LAST_DAY, "");

        Calendar today = Calendar.getInstance();
        String todayStr = today.get(Calendar.YEAR) + "-" +
                (today.get(Calendar.MONTH) + 1) + "-" +
                today.get(Calendar.DAY_OF_MONTH);

        if (!lastDay.equals(todayStr)) {
            Log.d(TAG, "Nouveau jour détecté, réinitialisation...");

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_LAST_DAY, todayStr);
            editor.putBoolean(KEY_PRESENCE_MARKED_TODAY, false);
            editor.apply();

            runOnUiThread(() -> {
                if (etatPrsence != null) {
                    etatPrsence.setText("Non marquée");
                    etatPrsence.setTextColor(Color.parseColor("#666666"));
                    iconepresence.setImageResource(R.drawable.refuse);
                    if (notifPresence != null) {
                        notifPresence.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Utilisateur connecté: " + currentUser.getEmail());
    }

    private void initializeViews() {
        // Header
        employeConnecte = findViewById(R.id.employeConnecte);
        posteDepartement = findViewById(R.id.posteDepartement);
        iconepresence = findViewById(R.id.iconepresence);

        // Cartes statistiques
        etatPrsence = findViewById(R.id.etatPrsence);
        soldeConge = findViewById(R.id.soldeConge);
        reunionVenir = findViewById(R.id.reunionVenir);

        // Actions rapides
        actionPresence = findViewById(R.id.actionPresence);
        actionConge = findViewById(R.id.actionConge);
        actionReunions = findViewById(R.id.actionReunions);
        attestation = findViewById(R.id.attestation);

        // Notifications badges
        notifPresence = findViewById(R.id.notifPresence);
        notifConge = findViewById(R.id.notifConge);
        notifAttestation = findViewById(R.id.notifAttestation);
        notifReunion = findViewById(R.id.notifReunion);

        // Notifications récentes
        notifRecentesContainer = findViewById(R.id.notifRecententesContainer);
        aucuneNotif = findViewById(R.id.aucuneNotif);

        // Footer

        // Initialiser les badges comme cachés
        notifPresence.setVisibility(View.GONE);
        notifConge.setVisibility(View.GONE);
        notifAttestation.setVisibility(View.GONE);
        notifReunion.setVisibility(View.GONE);
    }

    private void setupNavigation() {
        try {
            // Actions rapides
            if (actionPresence != null) {
                actionPresence.setOnClickListener(v ->
                        startActivity(new Intent(this, PresenceActivity.class)));
            }

            if (actionConge != null) {
                actionConge.setOnClickListener(v ->
            }

            if (actionReunions != null) {
                actionReunions.setOnClickListener(v ->
                        startActivity(new Intent(this, ReunionEmployeActivity.class)));
            }

            if (attestation != null) {
                attestation.setOnClickListener(v ->
                        startActivity(new Intent(this, AttestationEmployeActivity.class)));
            }

            // Footer navigation
            findViewById(R.id.iconPresence).setOnClickListener(v ->
                    startActivity(new Intent(this, PresenceActivity.class)));

            findViewById(R.id.conge).setOnClickListener(v ->

            findViewById(R.id.profil).setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileEmployeActivity.class)));

            // Ajouter la navigation pour les réunions dans le footer
            LinearLayout reunionsLayout = findViewById(R.id.reunionsfooter);
            if (reunionsLayout != null) {
                reunionsLayout.setOnClickListener(v ->
                        startActivity(new Intent(this, ReunionEmployeActivity.class)));
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur setupNavigation: " + e.getMessage());
        }
    }

    private void setupAllRealtimeListeners() {
        Log.d(TAG, "Configuration de TOUS les écouteurs temps réel");

        // 1. Écouteur employé (principal)
        setupEmployeListener();

        // 2. Écouteur présence
        setupPresenceListener();

        // 3. Écouteur réunions
        setupReunionsListener();

        // 4. Écouteur congés (pour badge)
        setupCongesListener();

        // 5. Écouteur attestations (pour badge)
        setupAttestationsListener();
    }

    private void setupEmployeListener() {
        if (currentUser == null) return;

        String userEmail = currentUser.getEmail();
        Log.d(TAG, "Démarrage écouteur temps réel employé pour: " + userEmail);

        // Chercher l'ID de l'employé d'abord
        backgroundExecutor.execute(() -> {
            try {
                QuerySnapshot querySnapshot = Tasks.await(
                        db.collection("employees")
                                .whereEqualTo("email", userEmail)
                                .limit(1)
                                .get()
                );

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    DocumentSnapshot employeDoc = querySnapshot.getDocuments().get(0);
                    employeeId = employeDoc.getId();

                    // Maintenant démarrer l'écouteur temps réel sur ce document
                    startEmployeRealtimeListener();
                } else {
                    // Fallback sur Users
                    Log.w(TAG, "Aucun employé trouvé, recherche dans Users");
                    searchInUsersCollection(userEmail);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur recherche employé: " + e.getMessage());
            }
        });
    }

    private void startEmployeRealtimeListener() {
        if (employeeId.isEmpty()) return;

        employeListener = db.collection("employees").document(employeeId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute employé temps réel: " + error.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Mettre à jour les données IMMÉDIATEMENT
                        employeeNom = documentSnapshot.getString("nom");
                        employeePrenom = documentSnapshot.getString("prenom");
                        employeePoste = documentSnapshot.getString("poste");
                        employeeDepartement = documentSnapshot.getString("departement");

                        Long solde = documentSnapshot.getLong("soldeConge");
                        soldeCongeValue = solde != null ? solde.intValue() : 0;

                        // Sauvegarder en cache
                        saveToCache();

                        // Mettre à jour l'UI sur le thread principal
                        mainHandler.post(() -> {
                            updateEmployeUI();
                            updateSoldeCongeUI();
                        });

                        Log.d(TAG, "Données employé mises à jour en temps réel");

                        // Déclencher une mise à jour des notifications avec délai
                        scheduleNotificationsUpdate();
                    }
                });
    }

    private void searchInUsersCollection(String userEmail) {
        backgroundExecutor.execute(() -> {
            try {
                QuerySnapshot querySnapshot = Tasks.await(
                        db.collection("Users")
                                .whereEqualTo("email", userEmail)
                                .limit(1)
                                .get()
                );

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);

                    employeeNom = userDoc.getString("nom");
                    employeePrenom = userDoc.getString("prenom");
                    employeePoste = "Non défini";
                    employeeDepartement = "Non défini";
                    soldeCongeValue = 0;

                    mainHandler.post(() -> {
                        updateEmployeUI();
                        updateSoldeCongeUI();
                    });

                    Log.d(TAG, "Données utilisateur chargées depuis Users");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur recherche utilisateur: " + e.getMessage());
            }
        });
    }

    private void updateEmployeUI() {
        try {
            if (employeConnecte != null) {
                String nomComplet = employeePrenom + " " + employeeNom;
                employeConnecte.setVisibility(VISIBLE);
                employeConnecte.setText(nomComplet);
            }

            if (posteDepartement != null) {
                String posteDept = employeePoste + " • " + employeeDepartement;
                posteDepartement.setVisibility(VISIBLE);
                posteDepartement.setText(posteDept);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur updateEmployeUI: " + e.getMessage());
        }
    }

    private void updateSoldeCongeUI() {
        try {
            if (soldeConge != null) {
                soldeConge.setText(soldeCongeValue + " jours");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur updateSoldeCongeUI: " + e.getMessage());
        }
    }

    private void setupPresenceListener() {
        if (currentUser == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String userEmail = currentUser.getEmail();

        Log.d(TAG, "Démarrage écouteur temps réel présence pour: " + userEmail);

        presenceListener = db.collection("PresenceHistory")
                .whereEqualTo("userId", userEmail)
                .whereEqualTo("date", today)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute présence: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        boolean presenceMarqueeFirestore = false;

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String status = doc.getString("status");
                                if ("present".equals(status)) {
                                    presenceMarqueeFirestore = true;
                                    break;
                                }
                            }
                        }

                        // Sauvegarder dans les préférences
                        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_PRESENCE_MARKED_TODAY, presenceMarqueeFirestore);
                        editor.apply();

                        // Calculer l'heure actuelle
                        Calendar now = Calendar.getInstance();
                        int currentHour = now.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = now.get(Calendar.MINUTE);
                        int totalMinutes = (currentHour * 60) + currentMinute;
                        int seuil9h = 9 * 60;

                        final String etatText;
                        final int etatColor;
                        final int iconResource;
                        final boolean showNotification;

                        if (presenceMarqueeFirestore) {
                            etatText = "Marquée";
                            etatColor = Color.parseColor("#0FAC71");
                            iconResource = R.drawable.approuve;
                            showNotification = false;
                        } else {
                            if (totalMinutes >= seuil9h) {
                                etatText = "Non marquée";
                                etatColor = Color.parseColor("#FF0000");
                                showNotification = true;
                            } else {
                                etatText = "Non marquée";
                                etatColor = Color.parseColor("#666666");
                                showNotification = false;
                            }
                            iconResource = R.drawable.refuse;
                        }

                        mainHandler.post(() -> {
                            try {
                                if (etatPrsence != null) {
                                    etatPrsence.setText(etatText);
                                    etatPrsence.setTextColor(etatColor);
                                    iconepresence.setImageResource(iconResource);

                                    if (notifPresence != null) {
                                        notifPresence.setVisibility(showNotification ? VISIBLE : View.GONE);
                                        if (showNotification) notifPresence.setText("!");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur UI présence: " + e.getMessage());
                            }
                        });

                        // Déclencher une mise à jour des notifications
                        scheduleNotificationsUpdate();
                    });
                });
    }

    private void setupReunionsListener() {
        Log.d(TAG, "Démarrage écouteur temps réel réunions");

        reunionsListener = db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute réunions: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        int reunionsAVenir = 0;
                        Date aujourdhui = new Date();

                        if (querySnapshot != null) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String dateStr = doc.getString("date");
                                String timeStr = doc.getString("heure");

                                if (dateStr != null && timeStr != null) {
                                    try {
                                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                        Date reunionDateTime = sdf.parse(dateStr + " " + timeStr);

                                        if (reunionDateTime != null && reunionDateTime.after(aujourdhui)) {
                                            reunionsAVenir++;
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Erreur parsing date: " + e.getMessage());
                                    }
                                }
                            }
                        }

                        final int finalCount = reunionsAVenir;
                        mainHandler.post(() -> {
                            try {
                                if (reunionVenir != null) {
                                    reunionVenir.setText(finalCount + " à venir");
                                }

                                if (notifReunion != null) {
                                    notifReunion.setText(String.valueOf(finalCount));
                                    notifReunion.setVisibility(finalCount > 0 ? VISIBLE : View.GONE);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur mise à jour réunions UI: " + e.getMessage());
                            }
                        });

                        // Déclencher une mise à jour des notifications
                        scheduleNotificationsUpdate();
                    });
                });
    }

    private void setupCongesListener() {
        if (currentUser == null) return;
        String userEmail = currentUser.getEmail();

        Log.d(TAG, "Démarrage écouteur temps réel congés pour: " + userEmail);
        Log.d(TAG, "UID utilisateur: " + currentUser.getUid());
        Log.d(TAG, "Email utilisateur: " + userEmail);

        congesListener = db.collection("conges")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute congés: " + error.getMessage());
                        Log.e(TAG, "Détails erreur: ", error);
                        return;
                    }

                    // DEBUG: Vérifiez ce qui est retourné
                    if (querySnapshot != null) {
                        Log.d(TAG, "QuerySnapshot reçu - taille: " + querySnapshot.size());
                        Log.d(TAG, "Metadata: " + querySnapshot.getMetadata());

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Log.d(TAG, "Document trouvé: ID=" + doc.getId() +
                                    ", statut=" + doc.getString("statut") +
                                    ", userEmail=" + doc.getString("userEmail"));
                        }
                    } else {
                        Log.d(TAG, "QuerySnapshot est null pour les congés");
                    }

                    int congesEnAttente = querySnapshot != null ? querySnapshot.size() : 0;
                    Log.d(TAG, "Congés en attente calculés: " + congesEnAttente);

                    mainHandler.post(() -> {
                        try {
                            if (notifConge != null) {
                                notifConge.setText(String.valueOf(congesEnAttente));
                                notifConge.setVisibility(congesEnAttente > 0 ? VISIBLE : View.GONE);
                                Log.d(TAG, "UI Badge congés mis à jour: " + congesEnAttente);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur mise à jour congés UI: " + e.getMessage(), e);
                        }
                    });

                    // Déclencher une mise à jour des notifications
                    scheduleNotificationsUpdate();
                });
    }

    private void setupAttestationsListener() {
        if (currentUser == null) return;

        Log.d(TAG, "Démarrage écouteur temps réel attestations");
        Log.d(TAG, "Recherche attestations avec UID: " + currentUser.getUid());

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("employeeId", currentUser.getUid())
                .whereEqualTo("statut", "en_attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute attestations: " + error.getMessage());
                        Log.e(TAG, "Détails erreur: ", error);
                        return;
                    }

                    // DEBUG: Vérifiez ce qui est retourné
                    if (querySnapshot != null) {
                        Log.d(TAG, "Attestations QuerySnapshot - taille: " + querySnapshot.size());

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Log.d(TAG, "Attestation document: ID=" + doc.getId() +
                                    ", statut=" + doc.getString("statut") +
                                    ", employeeId=" + doc.getString("employeeId") +
                                    ", type=" + doc.getString("typeAttestation"));
                        }
                    } else {
                        Log.d(TAG, "QuerySnapshot est null pour les attestations");
                    }

                    int attestationsEnAttente = querySnapshot != null ? querySnapshot.size() : 0;
                    Log.d(TAG, "Attestations en attente calculées: " + attestationsEnAttente);

                    mainHandler.post(() -> {
                        try {
                            if (notifAttestation != null) {
                                notifAttestation.setText(String.valueOf(attestationsEnAttente));
                                notifAttestation.setVisibility(attestationsEnAttente > 0 ? VISIBLE : View.GONE);
                                Log.d(TAG, "UI Badge attestations mis à jour: " + attestationsEnAttente);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur mise à jour attestations UI: " + e.getMessage(), e);
                        }
                    });

                    // Déclencher une mise à jour des notifications
                    scheduleNotificationsUpdate();
                });
    }

    private void testFirestoreDataStructure() {
        if (currentUser == null) return;

        Log.d(TAG, "=== TEST STRUCTURE DONNÉES FIRESTORE ===");

        // Tester la collection congés
        db.collection("conges")
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Structure congés - documents trouvés: " + task.getResult().size());
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Log.d(TAG, "Conge Doc ID: " + doc.getId());
                            Log.d(TAG, "  userEmail: " + doc.getString("userEmail"));
                            Log.d(TAG, "  userId: " + doc.getString("userId"));
                            Log.d(TAG, "  statut: " + doc.getString("statut"));
                            Log.d(TAG, "  typeConge: " + doc.getString("typeConge"));
                            Log.d(TAG, "  userEmail de l'utilisateur courant: " + currentUser.getEmail());
                        }
                    } else {
                        Log.e(TAG, "Erreur test congés: " + task.getException());
                    }
                });

        // Tester la collection attestations
        db.collection("Attestations")
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Structure attestations - documents trouvés: " + task.getResult().size());
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Log.d(TAG, "Attestation Doc ID: " + doc.getId());
                            Log.d(TAG, "  employeeId: " + doc.getString("employeeId"));
                            Log.d(TAG, "  employeeNom: " + doc.getString("employeeNom"));
                            Log.d(TAG, "  statut: " + doc.getString("statut"));
                            Log.d(TAG, "  UID utilisateur courant: " + currentUser.getUid());
                        }
                    } else {
                        Log.e(TAG, "Erreur test attestations: " + task.getException());
                    }
                });
    }

    private void scheduleNotificationsUpdate() {
        // Annuler la tâche précédente si elle existe
        if (debouncedUpdateTask != null) {
            debounceHandler.removeCallbacks(debouncedUpdateTask);
        }

        // Créer une nouvelle tâche avec délai
        debouncedUpdateTask = () -> {
            Log.d(TAG, "Déclenchement différé de la mise à jour des notifications");
            updateAllNotifications();
        };

        // Planifier la tâche avec délai (debouncing)
        debounceHandler.postDelayed(debouncedUpdateTask, DEBOUNCE_DELAY);
    }

    private void updateAllNotifications() {
        if (currentUser == null) return;

        Log.d(TAG, "Mise à jour des notifications démarrée");

        backgroundExecutor.execute(() -> {
            try {
                List<NotificationItem> allNotifications = new ArrayList<>();
                String userEmail = currentUser.getEmail();

                // 1. Vérifier présence (limiter à 1) - PRIORITAIRE
                checkPresenceForNotification(userEmail, allNotifications);

                // 2. Récupérer les congés (limiter à 2 par type)
                loadCongesForNotificationsLimited(userEmail, allNotifications);

                // 3. Récupérer les réunions (limiter à 2 par type)
                loadReunionsForNotificationsLimited(allNotifications);

                // 4. Récupérer les attestations (limiter à 2 par type)
                loadAttestationsForNotificationsLimited(allNotifications);

                // TRI : Présence en PREMIER, puis les autres par date
                Collections.sort(allNotifications, (n1, n2) -> {
                    // 1. La présence est TOUJOURS en premier
                    if ("presence".equals(n1.type) && !"presence".equals(n2.type)) {
                        return -1; // n1 (présence) avant n2
                    }
                    if (!"presence".equals(n1.type) && "presence".equals(n2.type)) {
                        return 1; // n2 (présence) avant n1
                    }

                    // 2. Pour les autres, trier par date (plus récent d'abord)
                    return n2.date.compareTo(n1.date);
                });

                // Limiter à 7 notifications les plus récentes au total
                final List<NotificationItem> finalNotifications =
                        allNotifications.subList(0, Math.min(allNotifications.size(), 7));

                // Sauvegarder dans le cache
                saveNotificationsToCache(finalNotifications);

                // Afficher sur le thread principal
                mainHandler.post(() -> displayNotifications(finalNotifications));

                Log.d(TAG, "Notifications mises à jour: " + finalNotifications.size() +
                        " (présence prioritaire, 2 max par type)");

            } catch (Exception e) {
                Log.e(TAG, "Erreur mise à jour notifications: " + e.getMessage());
            }
        });
    }

    // Méthode pour les congés (limitée à 2)
    private void loadCongesForNotificationsLimited(String userEmail, List<NotificationItem> notifications) {
        try {
            Log.d(TAG, "Chargement congés (limités à 2) pour: " + userEmail);

            QuerySnapshot congesSnapshot = Tasks.await(
                    db.collection("conges")
                            .whereEqualTo("userEmail", userEmail)
                            .orderBy("dateDemande", Query.Direction.DESCENDING)
                            .limit(2) // Seulement 2 congés les plus récents
                            .get()
            );

            if (congesSnapshot != null && !congesSnapshot.isEmpty()) {
                for (DocumentSnapshot doc : congesSnapshot.getDocuments()) {
                    String typeConge = doc.getString("typeConge");
                    String statut = doc.getString("statut");
                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("dateDemande");

                    if (timestamp == null) continue;

                    Date dateDemande = timestamp.toDate();
                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(dateDemande);

                    String type = typeConge != null ? typeConge : "Congé";

                    String titre;
                    switch (statut != null ? statut : "") {
                        case "En attente":
                            titre = "Congé en attente";
                            break;
                        case "Approuvé":
                        case "Approuvée":
                            titre = "Congé approuvé";
                            break;
                        case "Refusé":
                        case "Refusée":
                            titre = "Congé refusé";
                            break;
                        default:
                            titre = "Congé";
                    }

                    notifications.add(new NotificationItem(
                            titre,
                            type + " - Demandé le " + date,
                            getCongeIcon(statut),
                            "conges",
                            statut,
                            dateDemande
                    ));

                    Log.d(TAG, "Notification congé ajoutée: " + titre + " - Date: " + date);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement congés: " + e.getMessage(), e);
        }
    }

    // Méthode pour les réunions (limitée à 2)
    private void loadReunionsForNotificationsLimited(List<NotificationItem> notifications) {
        try {
            QuerySnapshot reunionsSnapshot = Tasks.await(
                    db.collection("Reunions")
                            .orderBy("date", Query.Direction.DESCENDING)
                            .limit(2) // Seulement 2 réunions les plus proches
                            .get()
            );

            List<ReunionNotification> reunionsList = new ArrayList<>();
            Date aujourdhui = new Date();

            if (reunionsSnapshot != null && !reunionsSnapshot.isEmpty()) {
                for (DocumentSnapshot doc : reunionsSnapshot.getDocuments()) {
                    String dateStr = doc.getString("date");
                    String timeStr = doc.getString("heure");
                    String titre = doc.getString("titre");
                    String lieu = doc.getString("lieu");

                    if (dateStr != null && timeStr != null && titre != null) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
                            Date dateReunion = sdf.parse(dateStr + " " + timeStr);

                            if (dateReunion != null && dateReunion.after(aujourdhui)) {
                                reunionsList.add(new ReunionNotification(
                                        titre, dateStr, timeStr, lieu != null ? lieu : "", dateReunion
                                ));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing date réunion: " + e.getMessage());
                        }
                    }
                }
            }

            // Trier par date (les plus proches d'abord) et prendre max 2
            Collections.sort(reunionsList, (r1, r2) -> r1.date.compareTo(r2.date));

            for (int i = 0; i < Math.min(2, reunionsList.size()); i++) {
                ReunionNotification reunion = reunionsList.get(i);
                String lieuText = (!reunion.lieu.isEmpty()) ? " à " + reunion.lieu : "";

                notifications.add(new NotificationItem(
                        "Réunion à venir",
                        reunion.titre + "\nprogrammé le " + reunion.dateStr + " " + reunion.heure + lieuText,
                        R.drawable.userb,
                        "reunion",
                        null,
                        reunion.date
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement réunions: " + e.getMessage());
        }
    }

    // Méthode pour les attestations (limitée à 2)
    private void loadAttestationsForNotificationsLimited(List<NotificationItem> notifications) {
        if (currentUser == null) return;

        try {
            QuerySnapshot attestationsSnapshot = Tasks.await(
                    db.collection("Attestations")
                            .whereEqualTo("employeeId", currentUser.getUid())
                            .orderBy("dateDemande", Query.Direction.DESCENDING)
                            .limit(2) // Seulement 2 attestations les plus récentes
                            .get()
            );

            if (attestationsSnapshot != null && !attestationsSnapshot.isEmpty()) {
                for (DocumentSnapshot doc : attestationsSnapshot.getDocuments()) {
                    String type = doc.getString("typeAttestation");
                    String statut = doc.getString("statut");
                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("dateDemande");

                    if (timestamp == null) continue;

                    Date dateDemande = timestamp.toDate();
                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(dateDemande);

                    String titre;
                    switch (statut != null ? statut : "") {
                        case "en_attente":
                            titre = "Attestation en attente";
                            break;
                        case "approuvee":
                        case "approuvé":
                            titre = "Attestation approuvée";
                            break;
                        case "refusee":
                        case "refusé":
                            titre = "Attestation refusée";
                            break;
                        default:
                            titre = "Attestation";
                    }

                    notifications.add(new NotificationItem(
                            titre,
                            (type != null ? type : "Attestation") + "\nDemandée le " + date,
                            getAttestationIcon(statut),
                            "attestation",
                            statut,
                            dateDemande
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement attestations: " + e.getMessage());
        }
    }

    // Notification de présence (limitée à 1, PRIORITAIRE)
    private void checkPresenceForNotification(String userEmail, List<NotificationItem> notifications) {
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        int currentMinute = cal.get(Calendar.MINUTE);
        int totalMinutes = (currentHour * 60) + currentMinute;

        if (totalMinutes >= (9 * 60)) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            try {
                QuerySnapshot presenceSnapshot = Tasks.await(
                        db.collection("PresenceHistory")
                                .whereEqualTo("userId", userEmail)
                                .whereEqualTo("date", today)
                                .limit(1)
                                .get()
                );

                boolean presenceMarqueeFirestore = false;
                if (presenceSnapshot != null && !presenceSnapshot.isEmpty()) {
                    DocumentSnapshot presenceDoc = presenceSnapshot.getDocuments().get(0);
                    String status = presenceDoc.getString("status");
                    presenceMarqueeFirestore = "present".equals(status);
                }

                SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                boolean presenceMarqueeLocal = prefs.getBoolean(KEY_PRESENCE_MARKED_TODAY, false);

                if (!presenceMarqueeFirestore && !presenceMarqueeLocal) {
                    // Date très récente pour que la présence soit en premier
                    Calendar now = Calendar.getInstance();
                    now.add(Calendar.MINUTE, -1); // 1 minute dans le passé
                    Date recentDate = now.getTime();

                    notifications.add(new NotificationItem(
                            "Présence non marquée !",
                            "N'oubliez pas de marquer votre présence",
                            R.drawable.alerticon,
                            "presence",
                            null,
                            recentDate
                    ));

                    Log.d(TAG, "Notification présence ajoutée (PRIORITAIRE)");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur vérification présence: " + e.getMessage());
            }
        }
    }

    // Mettre à jour saveNotificationsToCache pour la date
    private void saveNotificationsToCache(List<NotificationItem> notifications) {
        if (notifications.isEmpty()) return;

        StringBuilder cacheBuilder = new StringBuilder();
        for (int i = 0; i < Math.min(notifications.size(), 7); i++) {
            NotificationItem notif = notifications.get(i);
            cacheBuilder.append(notif.type).append("|")
                    .append(notif.title).append("|")
                    .append(notif.subtitle).append("|")
                    .append(notif.iconRes).append("|")
                    .append(notif.statut != null ? notif.statut : "null").append("|")
                    .append(notif.date.getTime()); // Ajouter le timestamp
            if (i < notifications.size() - 1 && i < 6) {
                cacheBuilder.append("§");
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NOTIFICATIONS_CACHE, cacheBuilder.toString());
        editor.putLong(KEY_NOTIFICATIONS_TIMESTAMP, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "Notifications sauvegardées en cache: " + notifications.size());
    }

    // Mettre à jour loadCachedNotifications pour lire la date
    private void loadCachedNotifications() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String cachedNotifications = prefs.getString(KEY_NOTIFICATIONS_CACHE, "");
        long cacheTime = prefs.getLong(KEY_NOTIFICATIONS_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();

        // Utiliser le cache si moins de 5 minutes
        if (!cachedNotifications.isEmpty() && (currentTime - cacheTime) < 300000) {
            try {
                String[] notifications = cachedNotifications.split("§");
                List<NotificationItem> notificationList = new ArrayList<>();

                for (String notifData : notifications) {
                    String[] parts = notifData.split("\\|");
                    if (parts.length >= 5) {
                        String type = parts[0];
                        String title = parts[1];
                        String subtitle = parts[2];
                        int icon = Integer.parseInt(parts[3]);
                        String statut = parts[4];
                        Date date = new Date();

                        if ("null".equals(statut)) statut = null;

                        // Lire la date si disponible
                        if (parts.length >= 6) {
                            try {
                                long timestamp = Long.parseLong(parts[5]);
                                date = new Date(timestamp);
                            } catch (NumberFormatException e) {
                                // Garder la date actuelle par défaut
                            }
                        }

                        notificationList.add(new NotificationItem(title, subtitle, icon, type, statut, date));
                    }
                }

                // Trier les notifications du cache avec priorité pour présence
                Collections.sort(notificationList, (n1, n2) -> {
                    // Présence en premier même dans le cache
                    if ("presence".equals(n1.type) && !"presence".equals(n2.type)) {
                        return -1;
                    }
                    if (!"presence".equals(n1.type) && "presence".equals(n2.type)) {
                        return 1;
                    }
                    return n2.date.compareTo(n1.date);
                });

                // Prendre seulement les 7 plus récentes même du cache
                final List<NotificationItem> finalList =
                        notificationList.subList(0, Math.min(notificationList.size(), 7));

                if (!finalList.isEmpty()) {
                    mainHandler.post(() -> displayNotifications(finalList));
                    Log.d(TAG, "Notifications affichées depuis le cache: " + finalList.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur parsing cache notifications: " + e.getMessage());
            }
        }
    }

    private int getCongeIcon(String statut) {
        switch (statut) {
            default:
                return R.drawable.conge;
        }
    }

    private int getAttestationIcon(String statut) {
        switch (statut) {
            default:
                return R.drawable.attestation;
        }
    }

    private void displayNotifications(List<NotificationItem> notifications) {
        try {
            if (notifRecentesContainer == null || aucuneNotif == null) return;

            notifRecentesContainer.removeAllViews();

            if (notifications.isEmpty()) {
                aucuneNotif.setText("Aucune notification");
                aucuneNotif.setVisibility(VISIBLE);
                notifRecentesContainer.setVisibility(View.GONE);
                return;
            }

            aucuneNotif.setVisibility(View.GONE);
            notifRecentesContainer.setVisibility(VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < notifications.size(); i++) {
                NotificationItem notif = notifications.get(i);

                View notificationView = inflater.inflate(R.layout.layout_notification_item,
                        notifRecentesContainer, false);

                // Définir le background selon le type de notification
                int backgroundRes = getNotificationBackground(notif.type);
                notificationView.setBackgroundResource(backgroundRes);

                ImageView icon = notificationView.findViewById(R.id.notificationIcon);
                TextView title = notificationView.findViewById(R.id.notificationTitle);
                TextView subtitle = notificationView.findViewById(R.id.notificationSubtitle);

                if (icon != null && title != null && subtitle != null) {
                    icon.setImageResource(notif.iconRes);
                    title.setText(notif.title);
                    subtitle.setText(notif.subtitle);

                    notificationView.setOnClickListener(v -> {
                        // Effet visuel temporaire au clic
                        int originalBackground = backgroundRes;
                        int highlightBackground = getHighlightBackground(notif.type);

                        // Appliquer un background plus foncé temporairement
                        notificationView.setBackgroundResource(highlightBackground);

                        // Réinitialiser après un court délai
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (notificationView != null && notificationView.getParent() != null) {
                                notificationView.setBackgroundResource(originalBackground);
                            }
                        }, 200);

                        // Lancer l'activité correspondante
                        launchActivityForNotification(notif.type);
                    });

                    notifRecentesContainer.addView(notificationView);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur displayNotifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getNotificationBackground(String type) {
        switch (type) {
            case "presence":
                return R.drawable.border_redlight;
            case "conges":
                return R.drawable.border_icon_grey;
            case "reunion":
                return R.drawable.border_icon_blue;
            case "attestation":
                return R.drawable.border_icon_purple;
            default:
                return R.drawable.border_gris;
        }
    }

    private int getHighlightBackground(String type) {
        switch (type) {
            case "presence":
                return R.drawable.border_redlight;
            case "conges":
                return R.drawable.border_green;
            case "reunion":
                return R.drawable.border_7jours;
            case "attestation":
                return R.drawable.border_icon_purple;
            default:
                return R.drawable.border_gris;
        }
    }

    private void launchActivityForNotification(String type) {
        Intent intent = null;

        switch (type) {
            case "conges":
                break;
            case "reunion":
                intent = new Intent(this, ReunionEmployeActivity.class);
                break;
            case "presence":
                intent = new Intent(this, PresenceActivity.class);
                break;
            case "attestation":
                intent = new Intent(this, AttestationEmployeActivity.class);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== onResume ===");
        // Rien à recharger manuellement - tout est en temps réel !
        checkAndResetForNewDay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== onDestroy ===");

        // Nettoyer tous les écouteurs
        if (presenceListener != null) presenceListener.remove();
        if (congesListener != null) congesListener.remove();
        if (attestationsListener != null) attestationsListener.remove();
        if (reunionsListener != null) reunionsListener.remove();
        if (employeListener != null) employeListener.remove();

        // Nettoyer le handler de debouncing
        if (debouncedUpdateTask != null) {
            debounceHandler.removeCallbacks(debouncedUpdateTask);
        }

        backgroundExecutor.shutdown();
    }
}