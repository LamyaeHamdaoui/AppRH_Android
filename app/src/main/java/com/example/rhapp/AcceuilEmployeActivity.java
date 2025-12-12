package com.example.rhapp;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
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
import androidx.core.content.ContextCompat;

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

    // Écouteurs Firestore TEMPS RÉEL
    private ListenerRegistration congesListener;
    private ListenerRegistration attestationsListener;
    private ListenerRegistration reunionsListener;
    private ListenerRegistration presenceListener;
    private ListenerRegistration employeListener;
    private ListenerRegistration notificationsListener; // Nouvel écouteur pour notifications

    // Thread management
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Données de l'employé
    private String employeeId = "";
    private String employeeNom = "";
    private String employeePrenom = "";
    private String employeePoste = "";
    private String employeeDepartement = "";
    private int soldeCongeValue = 0;
    private ImageView iconepresence;

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

        // Configurer TOUS les écouteurs Firestore temps réel
        setupAllRealtimeListeners();
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
                Log.d(TAG, "Données affichées depuis le cache");
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

        Log.d(TAG, "Données sauvegardées en cache");
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
        presencefooter = findViewById(R.id.presencefooter);
        congesfooter = findViewById(R.id.congesfooter);
        reunionsfooter = findViewById(R.id.reunionsfooter);
        profilefooter = findViewById(R.id.profilefooter);

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
                        startActivity(new Intent(this, CongesEmployeActivity.class)));
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
                    startActivity(new Intent(this, CongesEmployeActivity.class)));

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

        // 6. Écouteur notifications (pour les notifications récentes)
        setupNotificationsListener();
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
                    });
                });
    }

    private void setupCongesListener() {
        if (currentUser == null) return;
        String userEmail = currentUser.getEmail();

        Log.d(TAG, "Démarrage écouteur temps réel congés pour: " + userEmail);

        congesListener = db.collection("conges")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute congés: " + error.getMessage());
                        return;
                    }

                    int congesEnAttente = querySnapshot != null ? querySnapshot.size() : 0;

                    mainHandler.post(() -> {
                        try {
                            if (notifConge != null) {
                                notifConge.setText(String.valueOf(congesEnAttente));
                                notifConge.setVisibility(congesEnAttente > 0 ? VISIBLE : View.GONE);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur mise à jour congés UI: " + e.getMessage());
                        }
                    });
                });
    }

    private void setupAttestationsListener() {
        if (currentUser == null) return;

        Log.d(TAG, "Démarrage écouteur temps réel attestations");

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("employeeId", currentUser.getUid())
                .whereEqualTo("statut", "en_attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute attestations: " + error.getMessage());
                        return;
                    }

                    int attestationsEnAttente = querySnapshot != null ? querySnapshot.size() : 0;

                    mainHandler.post(() -> {
                        try {
                            if (notifAttestation != null) {
                                notifAttestation.setText(String.valueOf(attestationsEnAttente));
                                notifAttestation.setVisibility(attestationsEnAttente > 0 ? VISIBLE : View.GONE);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur mise à jour attestations UI: " + e.getMessage());
                        }
                    });
                });
    }

    private void setupNotificationsListener() {
        Log.d(TAG, "Démarrage écouteur temps réel notifications");

        // Cet écouteur combiné va surveiller toutes les sources de notifications
        backgroundExecutor.execute(() -> {
            try {
                // Initialiser les notifications avec les données actuelles
                updateAllNotifications();

                // Démarrer les écouteurs spécifiques pour les notifications
                startPresenceNotificationListener();
                startCongesNotificationListener();
                startReunionsNotificationListener();
                startAttestationsNotificationListener();

            } catch (Exception e) {
                Log.e(TAG, "Erreur initialisation notifications: " + e.getMessage());
            }
        });
    }

    private void updateAllNotifications() {
        if (currentUser == null) return;

        backgroundExecutor.execute(() -> {
            try {
                List<NotificationItem> notifications = new ArrayList<>();
                String userEmail = currentUser.getEmail();

                // 1. Vérifier présence
                checkPresenceForNotification(userEmail, notifications);

                // 2. Congés en attente
                loadCongesForNotifications(userEmail, notifications);

                // 3. Réunions à venir
                loadReunionsForNotifications(notifications);

                // 4. Attestations en attente
                loadAttestationsForNotifications(notifications);

                // Trier par priorité
                Collections.sort(notifications, (n1, n2) -> {
                    int p1 = getPriority(n1.type);
                    int p2 = getPriority(n2.type);
                    return Integer.compare(p2, p1);
                });

                // Afficher
                final List<NotificationItem> finalNotifications =
                        notifications.subList(0, Math.min(notifications.size(), 7));

                mainHandler.post(() -> displayNotifications(finalNotifications));

            } catch (Exception e) {
                Log.e(TAG, "Erreur mise à jour notifications: " + e.getMessage());
            }
        });
    }

    private void startPresenceNotificationListener() {
        if (currentUser == null) return;

        String userEmail = currentUser.getEmail();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Écouteur spécifique pour les notifications de présence
        db.collection("PresenceHistory")
                .whereEqualTo("userId", userEmail)
                .whereEqualTo("date", today)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    // Mettre à jour toutes les notifications quand la présence change
                    updateAllNotifications();
                });
    }

    private void startCongesNotificationListener() {
        if (currentUser == null) return;

        String userEmail = currentUser.getEmail();

        db.collection("conges")
                .whereEqualTo("userEmail", userEmail)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    // Mettre à jour quand les congés changent
                    updateAllNotifications();
                });
    }

    private void startReunionsNotificationListener() {
        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    // Mettre à jour quand les réunions changent
                    updateAllNotifications();
                });
    }

    private void startAttestationsNotificationListener() {
        if (currentUser == null) return;

        db.collection("Attestations")
                .whereEqualTo("employeeId", currentUser.getUid())
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    // Mettre à jour quand les attestations changent
                    updateAllNotifications();
                });
    }

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
                    notifications.add(new NotificationItem(
                            "Présence non marquée ! ",
                            "N'oubliez pas de marquer votre présence",
                            R.drawable.alerticon,
                            "presence"
                    ));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur vérification présence: " + e.getMessage());
            }
        }
    }

    private void loadCongesForNotifications(String userEmail, List<NotificationItem> notifications) {
        try {
            QuerySnapshot congesSnapshot = Tasks.await(
                    db.collection("conges")
                            .whereEqualTo("userEmail", userEmail)
                            .whereEqualTo("statut", "En attente")
                            .orderBy("dateDemande", Query.Direction.DESCENDING)
                            .limit(2)
                            .get()
            );

            if (congesSnapshot != null && !congesSnapshot.isEmpty()) {
                for (DocumentSnapshot doc : congesSnapshot.getDocuments()) {
                    String typeConge = doc.getString("typeConge");
                    String typeCongé = doc.getString("typeCongé");

                    String type = typeConge;
                    if (type == null || type.isEmpty()) {
                        type = typeCongé;
                    }
                    if (type == null) type = "Congé";

                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("dateDemande");
                    String date = (timestamp != null) ?
                            new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(timestamp.toDate()) :
                            "Date inconnue";

                    notifications.add(new NotificationItem(
                            "Congé en attente",
                            type + " - Demandé le " + date,
                            R.drawable.docpurple,
                            "conges"
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement congés: " + e.getMessage());
        }
    }

    private void loadReunionsForNotifications(List<NotificationItem> notifications) {
        try {
            QuerySnapshot reunionsSnapshot = Tasks.await(
                    db.collection("Reunions")
                            .limit(10)
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
                            // Ignorer les erreurs de parsing
                        }
                    }
                }
            }

            Collections.sort(reunionsList, (r1, r2) -> r1.date.compareTo(r2.date));

            int limit = Math.min(2, reunionsList.size());
            for (int i = 0; i < limit; i++) {
                ReunionNotification reunion = reunionsList.get(i);
                String lieuText = (!reunion.lieu.isEmpty()) ? " à " + reunion.lieu : "";

                notifications.add(new NotificationItem(
                        "Réunion à venir",
                        reunion.titre + " - " + reunion.dateStr + " " + reunion.heure + lieuText,
                        R.drawable.userb,
                        "reunion"
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement réunions: " + e.getMessage());
        }
    }

    private void loadAttestationsForNotifications(List<NotificationItem> notifications) {
        if (currentUser == null) return;

        try {
            QuerySnapshot attestationsSnapshot = Tasks.await(
                    db.collection("Attestations")
                            .whereEqualTo("employeeId", currentUser.getUid())
                            .whereEqualTo("statut", "en_attente")
                            .limit(2)
                            .get()
            );

            if (attestationsSnapshot != null && !attestationsSnapshot.isEmpty()) {
                for (DocumentSnapshot doc : attestationsSnapshot.getDocuments()) {
                    String type = doc.getString("typeAttestation");
                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("dateDemande");
                    String date = (timestamp != null) ?
                            new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(timestamp.toDate()) :
                            "Date inconnue";

                    notifications.add(new NotificationItem(
                            "Attestation en attente",
                            (type != null ? type : "Attestation") + " - " + date,
                            R.drawable.attestation,
                            "attestation"
                    ));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement attestations: " + e.getMessage());
        }
    }

    private int getPriority(String type) {
        switch (type) {
            case "presence": return 100;
            case "reunion": return 80;
            case "conges": return 60;
            case "attestation": return 40;
            default: return 0;
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

            // Supprimer le background par défaut du layout XML
            // et appliquer un background différent selon le type

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
                return R.drawable.border_redlight; // Rouge clair pour présence
            case "conges":
                return R.drawable.border_orangelight; // Orange clair pour congés
            case "reunion":
                return R.drawable.border_blueclair; // Bleu pour réunions
            case "attestation":
                return R.drawable.border_icon_purple; // Vert clair pour attestations
            default:
                return R.drawable.border_gris; // Gris par défaut
        }
    }

    private int getHighlightBackground(String type) {
        switch (type) {
            case "presence":
                return R.drawable.border_redlight; // Rouge foncé pour effet clic
            case "conges":
                return R.drawable.border_green; // Orange foncé
            case "reunion":
                return R.drawable.border_blue_bg; // Bleu foncé
            case "attestation":
                return R.drawable.border_icon_purple; // Vert foncé
            default:
                return R.drawable.border_gris; // Gris foncé
        }
    }

    private void launchActivityForNotification(String type) {
        Intent intent = null;

        switch (type) {
            case "conges":
                intent = new Intent(this, CongesEmployeActivity.class);
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
    }    @Override
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

        backgroundExecutor.shutdown();
    }

    // Classes internes
    private static class NotificationItem {
        String title;
        String subtitle;
        int iconRes;
        String type;

        NotificationItem(String title, String subtitle, int iconRes, String type) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.type = type;
        }
    }

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
}