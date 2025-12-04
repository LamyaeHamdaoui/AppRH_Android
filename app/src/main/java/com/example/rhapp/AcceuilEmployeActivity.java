package com.example.rhapp;

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

    // Écouteurs Firestore
    private ListenerRegistration congesListener;
    private ListenerRegistration attestationsListener;
    private ListenerRegistration reunionsListener;
    private ListenerRegistration presenceListener;
    private ListenerRegistration employeListener;

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

        // Charger les données de l'employé connecté
        loadEmployeData();

        // Configurer les écouteurs Firestore
        setupFirestoreListeners();

        // Charger les notifications
        loadNotificationsRecentetes();
    }
    // AJOUTEZ CETTE MÉTHODE POUR VÉRIFIER MINUIT :
    private void checkAndResetForNewDay() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String lastDay = prefs.getString(KEY_LAST_DAY, "");

        // Obtenir la date d'aujourd'hui
        Calendar today = Calendar.getInstance();
        String todayStr = today.get(Calendar.YEAR) + "-" +
                (today.get(Calendar.MONTH) + 1) + "-" +
                today.get(Calendar.DAY_OF_MONTH);

        Log.d(TAG, "Vérification jour: Dernier=" + lastDay + ", Aujourd'hui=" + todayStr);

        if (!lastDay.equals(todayStr)) {
            // C'est un nouveau jour, réinitialiser l'état de présence
            Log.d(TAG, "Nouveau jour détecté, réinitialisation...");

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_LAST_DAY, todayStr);
            editor.putBoolean(KEY_PRESENCE_MARKED_TODAY, false); // Réinitialiser à non marqué
            editor.apply();

            // Mettre à jour l'UI immédiatement
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
        } else {
            Log.d(TAG, "Même jour, pas de réinitialisation");
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

    private void loadEmployeData() {
        if (currentUser == null) return;

        String userEmail = currentUser.getEmail();
        Log.d(TAG, "Chargement employé pour email: " + userEmail);

        // Chercher l'employé par email (point commun entre User et Employe)
        backgroundExecutor.execute(() -> {
            try {
                // Chercher dans la collection Employes
                QuerySnapshot querySnapshot = Tasks.await(
                        db.collection("employees")
                                .whereEqualTo("email", userEmail)
                                .limit(1)
                                .get()
                );

                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot employeDoc = querySnapshot.getDocuments().get(0);

                    if (employeDoc.exists()) {
                        employeeId = employeDoc.getId();
                        employeeNom = employeDoc.getString("nom");
                        employeePrenom = employeDoc.getString("prenom");
                        employeePoste = employeDoc.getString("poste");
                        employeeDepartement = employeDoc.getString("departement");

                        Long solde = employeDoc.getLong("soldeConge");
                        soldeCongeValue = solde != null ? solde.intValue() : 0;

                        Log.d(TAG, "Employé trouvé: " + employeePrenom + " " + employeeNom);
                        Log.d(TAG, "Solde congé: " + soldeCongeValue);

                        mainHandler.post(() -> {
                            updateEmployeUI();
                            // Mettre à jour le badge solde congé
                            updateSoldeCongeUI();
                        });
                    } else {
                        Log.w(TAG, "Document employé existe mais vide");
                    }
                } else {
                    Log.w(TAG, "Aucun employé trouvé pour email: " + userEmail);
                    // Chercher dans la collection Users comme fallback
                    loadUserDataAsFallback(userEmail);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement employé: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void loadUserDataAsFallback(String userEmail) {
        try {
            QuerySnapshot querySnapshot = Tasks.await(
                    db.collection("Users")
                            .whereEqualTo("email", userEmail)
                            .limit(1)
                            .get()
            );

            if (!querySnapshot.isEmpty()) {
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
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement utilisateur fallback: " + e.getMessage());
        }
    }

    private void updateEmployeUI() {
        try {
            if (employeConnecte != null) {
                String nomComplet = employeePrenom + " " + employeeNom;
                employeConnecte.setText(nomComplet);
                Log.d(TAG, "Nom employé mis à jour: " + nomComplet);
            }

            if (posteDepartement != null) {
                String posteDept = employeePoste + " • " + employeeDepartement;
                posteDepartement.setText(posteDept);
                Log.d(TAG, "Poste/Département mis à jour: " + posteDept);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur updateEmployeUI: " + e.getMessage());
        }
    }

    private void updateSoldeCongeUI() {
        try {
            if (soldeConge != null) {
                soldeConge.setText(soldeCongeValue + " jours");
                Log.d(TAG, "Solde congé mis à jour: " + soldeCongeValue + " jours");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur updateSoldeCongeUI: " + e.getMessage());
        }
    }

    private void setupFirestoreListeners() {
        Log.d(TAG, "Configuration des écouteurs Firestore");
        setupPresenceListener();
        setupReunionsListener();
        setupCongesListener();
        setupAttestationsListener();
        setupEmployeListener(); // Écouter les changements de l'employé
    }

    private void setupEmployeListener() {
        if (currentUser == null || employeeId.isEmpty()) return;

        employeListener = db.collection("employees").document(employeeId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute employé: " + error.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        backgroundExecutor.execute(() -> {
                            employeeNom = documentSnapshot.getString("nom");
                            employeePrenom = documentSnapshot.getString("prenom");
                            employeePoste = documentSnapshot.getString("poste");
                            employeeDepartement = documentSnapshot.getString("departement");

                            Long solde = documentSnapshot.getLong("soldeConge");
                            soldeCongeValue = solde != null ? solde.intValue() : 0;

                            mainHandler.post(() -> {
                                updateEmployeUI();
                                updateSoldeCongeUI();
                            });
                        });
                    }
                });
    }

    private void setupPresenceListener() {
        if (currentUser == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Log.d(TAG, "Écoute présence pour date: " + today);

        presenceListener = db.collection("Presence")
                .document(currentUser.getUid())
                .collection("history")
                .document(today)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute présence: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        // Vérifier si la présence est marquée dans Firestore
                        boolean presenceMarqueeFirestore = documentSnapshot != null &&
                                documentSnapshot.exists() &&
                                "present".equals(documentSnapshot.getString("status"));

                        // Sauvegarder l'état localement
                        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_PRESENCE_MARKED_TODAY, presenceMarqueeFirestore);
                        editor.apply();

                        // Obtenir l'heure actuelle
                        Calendar now = Calendar.getInstance();
                        int currentHour = now.get(Calendar.HOUR_OF_DAY);
                        int currentMinute = now.get(Calendar.MINUTE);

                        // Calculer en minutes depuis minuit
                        int totalMinutes = (currentHour * 60) + currentMinute;
                        int seuil9h = 9 * 60; // 9h00 en minutes

                        // Déterminer l'état à afficher
                        final String etatText;
                        final int etatColor;
                        final int iconResource;
                        final boolean showNotification;

                        if (presenceMarqueeFirestore) {
                            // Présence marquée - TOUJOURS vert
                            etatText = "Marquée";
                            etatColor = Color.parseColor("#0FAC71"); // Vert
                            iconResource = R.drawable.approuve;
                            showNotification = false;
                        } else {
                            // Présence non marquée
                            if (totalMinutes >= seuil9h) {
                                // Après 9h - rouge avec notification
                                etatText = "Non marquée";
                                etatColor = Color.parseColor("#FF0000"); // Rouge
                                showNotification = true;
                            } else {
                                // Avant 9h - gris sans notification
                                etatText = "Non marquée";
                                etatColor = Color.parseColor("#666666"); // Gris
                                showNotification = false;
                            }
                            iconResource = R.drawable.refuse;
                        }

                        // Mettre à jour l'UI sur le thread principal
                        mainHandler.post(() -> {
                            try {
                                if (etatPrsence != null) {
                                    etatPrsence.setText(etatText);
                                    etatPrsence.setTextColor(etatColor);
                                    iconepresence.setImageResource(iconResource);

                                    if (notifPresence != null) {
                                        if (showNotification) {
                                            notifPresence.setVisibility(View.VISIBLE);
                                            notifPresence.setText("!");
                                        } else {
                                            notifPresence.setVisibility(View.GONE);
                                        }
                                    }
                                }

                                Log.d(TAG, String.format("Présence: %s (Heure: %02d:%02d, Firestore: %s)",
                                        etatText, currentHour, currentMinute, presenceMarqueeFirestore));

                            } catch (Exception e) {
                                Log.e(TAG, "Erreur UI présence: " + e.getMessage());
                            }
                        });
                    });
                });
    }

    private void setupReunionsListener() {
        Log.d(TAG, "Configuration écouteur réunions");

        reunionsListener = db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute réunions: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        try {
                            int reunionsAVenir = 0;
                            Date aujourdhui = new Date();

                            if (querySnapshot != null) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    String dateStr = doc.getString("date");
                                    String timeStr = doc.getString("heure");

                                    if (dateStr != null && timeStr != null) {
                                        try {
                                            // Convertir la string en Date
                                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                            Date reunionDateTime = sdf.parse(dateStr + " " + timeStr);

                                            // Comparer avec maintenant
                                            if (reunionDateTime != null && reunionDateTime.after(aujourdhui)) {
                                                reunionsAVenir++;
                                                Log.d(TAG, "Réunion à venir trouvée: " +
                                                        doc.getString("titre") + " le " + dateStr);
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Erreur parsing date: " + dateStr + " " + timeStr);
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
                                        notifReunion.setVisibility(finalCount > 0 ? View.VISIBLE : View.GONE);
                                    }
                                    Log.d(TAG, "Réunions à venir: " + finalCount);
                                } catch (Exception e) {
                                    Log.e(TAG, "Erreur mise à jour réunions UI: " + e.getMessage());
                                }
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Erreur traitement réunions: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                });
    }
    private void setupCongesListener() {
        if (currentUser == null) return;
        String userEmail = currentUser.getEmail();
        Log.d(TAG, "Configuration écouteur congés pour userId: " + currentUser.getUid());

        // CORRECTION : Chercher directement dans la collection conges
        congesListener = db.collection("conges")
                .whereEqualTo("userEmail", userEmail)
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute congés: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        int congesEnAttente = querySnapshot != null ? querySnapshot.size() : 0;

                        // DEBUG
                        Log.d(TAG, "Congés trouvés: " + congesEnAttente);
                        if (querySnapshot != null) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Log.d(TAG, "Congé doc: " + doc.getId() +
                                        ", type: " + doc.getString("typeConge") +
                                        ", typeCongé: " + doc.getString("typeCongé"));
                            }
                        }

                        mainHandler.post(() -> {
                            try {
                                if (notifConge != null) {
                                    notifConge.setText(String.valueOf(congesEnAttente));
                                    notifConge.setVisibility(congesEnAttente > 0 ? View.VISIBLE : View.GONE);
                                    Log.d(TAG, "Congés en attente: " + congesEnAttente);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur mise à jour congés UI: " + e.getMessage());
                            }
                        });
                    });
                });
    }

    private void setupAttestationsListener() {
        if (currentUser == null) return;

        Log.d(TAG, "Configuration écouteur attestations");

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("employeeId", currentUser.getUid())
                .whereEqualTo("statut", "en_attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute attestations: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        int attestationsEnAttente = querySnapshot != null ? querySnapshot.size() : 0;

                        mainHandler.post(() -> {
                            try {
                                if (notifAttestation != null) {
                                    notifAttestation.setText(String.valueOf(attestationsEnAttente));
                                    notifAttestation.setVisibility(attestationsEnAttente > 0 ? View.VISIBLE : View.GONE);
                                    Log.d(TAG, "Attestations en attente: " + attestationsEnAttente);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur mise à jour attestations UI: " + e.getMessage());
                            }
                        });
                    });
                });
    }

    private int compareDate(String date, String time) {
        try {
            // Format de vos dates: "07/11/2025" et heure: "11:15"
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date reunionDate = dateFormat.parse(date + " " + time);
            Date now = new Date();

            if (reunionDate == null) return -1;

            if (reunionDate.after(now)) return 1; // À venir
            if (reunionDate.before(now)) return -1; // Passée
            return 0; // Maintenant (peu probable)

        } catch (Exception e) {
            Log.e(TAG, "Erreur comparaison date: " + e.getMessage());
            return -1;
        }
    }
// Dans AcceuilEmployeActivity.java, modifiez la méthode loadNotificationsRecentetes()

    private void loadNotificationsRecentetes() {

        Log.d(TAG, "=== DÉBUT loadNotificationsRecentetes ===");
        Log.d(TAG, "User ID: " + (currentUser != null ? currentUser.getUid() : "null"));
        Log.d(TAG, "User Email: " + (currentUser != null ? currentUser.getEmail() : "null"));
        if (currentUser == null) {
            Log.e(TAG, "Utilisateur non connecté");
            return;
        }
        final String userId = currentUser.getUid();
        final String userEmail = currentUser.getEmail();

        Log.d(TAG, "User ID pour requêtes: " + userId);


        backgroundExecutor.execute(() -> {
            try {
                List<NotificationItem> notifications = new ArrayList<>();
                Log.d(TAG, "Début du chargement des notifications");


                // 1. VÉRIFIER SI ON EST APRÈS 9H ET PRÉSENCE NON MARQUÉE
                Calendar cal = Calendar.getInstance();
                int currentHour = cal.get(Calendar.HOUR_OF_DAY);
                int currentMinute = cal.get(Calendar.MINUTE);
                int totalMinutes = (currentHour * 60) + currentMinute;

                if (totalMinutes >= (9 * 60)) { // Après 9h
                    // Vérifier dans Firestore
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    try {
                        DocumentSnapshot presenceDoc = Tasks.await(
                                db.collection("Presence")
                                        .document(userId)
                                        .collection("history")
                                        .document(today)
                                        .get()
                        );

                        boolean presenceMarqueeFirestore = presenceDoc.exists() &&
                                "present".equals(presenceDoc.getString("status"));

                        // Vérifier aussi dans les préférences locales
                        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                        boolean presenceMarqueeLocal = prefs.getBoolean(KEY_PRESENCE_MARKED_TODAY, false);

                        // Si ni Firestore ni local n'indiquent la présence marquée
                        if (!presenceMarqueeFirestore && !presenceMarqueeLocal) {
                            notifications.add(new NotificationItem(
                                    "⚠Présence non marquée",
                                    "N'oubliez pas de marquez votre présence",
                                    R.drawable.alerticon,
                                    "presence"
                            ));
                            Log.d(TAG, "Notification présence ajoutée (après 9h)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur vérification présence: " + e.getMessage());
                    }
                }

                // 2. CHARGER CONGÉS EN ATTENTE - CORRIGÉ
                // 2. CHARGER CONGÉS EN ATTENTE - AVEC PLUS DE LOGS
                try {
                    Log.d(TAG, "Tentative de chargement des congés...");
                    Log.d(TAG, "Collection: conges, userId: " + userId + ", statut: En attente");

                    QuerySnapshot congesSnapshot = Tasks.await(
                            db.collection("conges")
                                    .whereEqualTo("userEmail", userEmail)
                                    .whereEqualTo("statut", "En attente")
                                    .orderBy("dateDemande", Query.Direction.DESCENDING)
                                    .limit(2)
                                    .get()
                    );

                    Log.d(TAG, "Congés snapshot obtenu, size: " + (congesSnapshot != null ? congesSnapshot.size() : "null"));

                    if (congesSnapshot != null && !congesSnapshot.isEmpty()) {
                        Log.d(TAG, "Nombre de congés trouvés: " + congesSnapshot.size());

                        for (DocumentSnapshot doc : congesSnapshot.getDocuments()) {
                            String docId = doc.getId();
                            String typeConge = doc.getString("typeConge");
                            String typeCongé = doc.getString("typeCongé");
                            String statut = doc.getString("statut");
                            Object userIdDoc = doc.get("userId");

                            Log.d(TAG, "Document congé trouvé:");
                            Log.d(TAG, "  - ID: " + docId);
                            Log.d(TAG, "  - typeConge: " + typeConge);
                            Log.d(TAG, "  - typeCongé: " + typeCongé);
                            Log.d(TAG, "  - statut: " + statut);
                            Log.d(TAG, "  - userId dans doc: " + userIdDoc);
                            Log.d(TAG, "  - userId attendu: " + userId);

                            // ESSAYER LES DEUX NOMS DE CHAMPS POSSIBLES
                            String type = typeConge;
                            if (type == null || type.isEmpty()) {
                                type = typeCongé; // Avec accent
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

                            Log.d(TAG, "Notification congé ajoutée: " + type + " - " + date);
                        }
                    } else {
                        Log.d(TAG, "Aucun congé trouvé pour cet utilisateur");
                        // TEST: Vérifier tous les congés sans filtre
                        try {
                            QuerySnapshot allConges = Tasks.await(
                                    db.collection("conges").limit(5).get()
                            );
                            Log.d(TAG, "Tous les congés (premiers 5): " + allConges.size());
                            for (DocumentSnapshot doc : allConges.getDocuments()) {
                                Log.d(TAG, "Congé disponible - ID: " + doc.getId() +
                                        ", userId: " + doc.getString("userId") +
                                        ", statut: " + doc.getString("statut"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur test tous les congés: " + e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement congés: " + e.getMessage());
                    e.printStackTrace();
                }

                Log.d(TAG, "Notifications après congés: " + notifications.size());

                // 3. CHARGER ATTESTATIONS EN ATTENTE
                try {
                    // Essayer les deux champs possibles
                    QuerySnapshot attestationsSnapshot = null;

                    try {
                        attestationsSnapshot = Tasks.await(
                                db.collection("Attestations")
                                        .whereEqualTo("employeId", userId)
                                        .whereEqualTo("statut", "en_attente")
                                        .limit(2)
                                        .get()
                        );
                    } catch (Exception e) {
                        Log.d(TAG, "Essai avec employeId échoué: " + e.getMessage());
                    }

                    if (attestationsSnapshot == null || attestationsSnapshot.isEmpty()) {
                        try {
                            attestationsSnapshot = Tasks.await(
                                    db.collection("Attestations")
                                            .whereEqualTo("employeeId", userId)
                                            .whereEqualTo("statut", "en_attente")
                                            .limit(2)
                                            .get()
                            );
                        } catch (Exception e) {
                            Log.d(TAG, "Essai avec employeeId échoué: " + e.getMessage());
                        }
                    }

                    if (attestationsSnapshot != null) {
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
                        Log.d(TAG, "Attestations chargées: " + attestationsSnapshot.size());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement attestations: " + e.getMessage());
                }

                // 4. CHARGER RÉUNIONS À VENIR - CORRIGÉ POUR STRING DATE

                // 4. CHARGER RÉUNIONS À VENIR - AVEC PLUS DE LOGS
                // 4. CHARGER RÉUNIONS À VENIR - CORRIGÉ
                try {
                    QuerySnapshot reunionsSnapshot = Tasks.await(
                            db.collection("Reunions")
                                    .get()
                    );

                    List<ReunionNotification> reunionsList = new ArrayList<>();
                    Date aujourdhui = new Date();

                    Log.d(TAG, "Nombre total de réunions: " + (reunionsSnapshot != null ? reunionsSnapshot.size() : 0));

                    if (reunionsSnapshot != null && !reunionsSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : reunionsSnapshot.getDocuments()) {
                            String dateStr = doc.getString("date");
                            String timeStr = doc.getString("heure");
                            String titre = doc.getString("titre");
                            String lieu = doc.getString("lieu");

                            if (dateStr != null && timeStr != null && titre != null) {
                                try {
                                    // Convertir "07/11/2025 11:15" en Date
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
                                    Date dateReunion = sdf.parse(dateStr + " " + timeStr);

                                    Log.d(TAG, "Vérification réunion: " + titre + " - " + dateStr + " " + timeStr);
                                    Log.d(TAG, "Date parsée: " + dateReunion + ", après aujourd'hui? " +
                                            (dateReunion != null ? dateReunion.after(aujourdhui) : "null"));

                                    if (dateReunion != null && dateReunion.after(aujourdhui)) {
                                        reunionsList.add(new ReunionNotification(
                                                titre,
                                                dateStr,
                                                timeStr,
                                                lieu != null ? lieu : "",
                                                dateReunion
                                        ));
                                        Log.d(TAG, "Réunion à venir DÉTECTÉE: " + titre);
                                    }
                                } catch (ParseException e) {
                                    Log.e(TAG, "Erreur parsing date réunion: " + dateStr + " " + timeStr + " - " + e.getMessage());
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Réunions à venir trouvées: " + reunionsList.size());

                    // Trier par date (les plus proches en premier)
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
                        Log.d(TAG, "Réunion AJOUTÉE aux notifications: " + reunion.titre);
                    }
                    Log.d(TAG, "Réunions ajoutées aux notifications: " + limit);

                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement réunions: " + e.getMessage());
                    e.printStackTrace();
                }

                Log.d(TAG, "Total notifications avant tri: " + notifications.size());

                // Trier les notifications par priorité
                Collections.sort(notifications, (n1, n2) -> {
                    int p1 = getPriority(n1.type);
                    int p2 = getPriority(n2.type);
                    return Integer.compare(p2, p1);
                });


                // Limiter à 7 notifications
                final List<NotificationItem> finalNotifications =
                        notifications.subList(0, Math.min(notifications.size(), 7));

                Log.d(TAG, "Total notifications final: " + finalNotifications.size());

                // Afficher sur le thread principal
                mainHandler.post(() -> {
                    displayNotifications(finalNotifications);
                    Log.d(TAG, "=== FIN loadNotificationsRecentetes ===");
                    Log.d(TAG, "Total notifications affichées: " + finalNotifications.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Erreur générale notifications: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private int getPriority(String type) {
        switch (type) {
            case "presence": return 100; // Priorité MAXIMUM
            case "conges": return 80;
            case "attestation": return 60;
            case "reunion": return 40;
            default: return 0;
        }
    }

    private void displayNotifications(List<NotificationItem> notifications) {
        Log.d(TAG, "=== DÉBUT displayNotifications ===");
        Log.d(TAG, "Nombre de notifications reçues: " + notifications.size());

        for (int i = 0; i < notifications.size(); i++) {
            NotificationItem notif = notifications.get(i);
            Log.d(TAG, "Notification " + i + ": " + notif.title + " - " + notif.subtitle + " (type: " + notif.type + ")");
        }
        try {
            if (notifRecentesContainer == null || aucuneNotif == null) return;

            notifRecentesContainer.removeAllViews();

            if (notifications.isEmpty()) {
                aucuneNotif.setVisibility(View.VISIBLE);
                notifRecentesContainer.setVisibility(View.GONE);
                Log.d(TAG, "Aucune notification à afficher");
                return;
            }

            aucuneNotif.setVisibility(View.GONE);
            notifRecentesContainer.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(this);
            final int[] selectedPosition = {-1};

            // List pour stocker les drawables originaux par position
            final List<Drawable> originalBackgrounds = new ArrayList<>();

            for (int i = 0; i < notifications.size(); i++) {
                NotificationItem notif = notifications.get(i);
                final int position = i;

                View notificationView = inflater.inflate(R.layout.layout_notification_item,
                        notifRecentesContainer, false);

                // FORCER LE DESSIN POUR AVOIR LE BACKGROUND
                notificationView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                notificationView.layout(0, 0,
                        notificationView.getMeasuredWidth(),
                        notificationView.getMeasuredHeight());

                // MAINTENANT on peut obtenir le background
                Drawable originalBackground = notificationView.getBackground();
                if (originalBackground == null) {
                    // Si toujours null, utiliser le background par défaut
                    originalBackground = ContextCompat.getDrawable(this,
                            R.drawable.border); // C
                }
                originalBackgrounds.add(originalBackground);

                ImageView icon = notificationView.findViewById(R.id.notificationIcon);
                TextView title = notificationView.findViewById(R.id.notificationTitle);
                TextView subtitle = notificationView.findViewById(R.id.notificationSubtitle);

                if (icon != null && title != null && subtitle != null) {
                    icon.setImageResource(notif.iconRes);
                    title.setText(notif.title);
                    subtitle.setText(notif.subtitle);

                    // Ajouter le clic
                    notificationView.setOnClickListener(v -> {
                        // Réinitialiser le background de l'ancienne notification sélectionnée
                        if (selectedPosition[0] != -1) {
                            View previousView = notifRecentesContainer.getChildAt(selectedPosition[0]);
                            if (previousView != null && originalBackgrounds.size() > selectedPosition[0]) {
                                previousView.setBackground(originalBackgrounds.get(selectedPosition[0]));
                            }
                        }

                        // Appliquer le nouveau background
                        int backgroundRes = 0;
                        Intent intent = null;

                        switch (notif.type) {
                            case "conges":
                                intent = new Intent(this, CongesEmployeActivity.class);
                                backgroundRes = R.drawable.border_orangelight;
                                break;
                            case "reunion":
                                intent = new Intent(this, ReunionEmployeActivity.class);
                                backgroundRes = R.drawable.border_blue_bg;
                                break;
                            case "presence":
                                intent = new Intent(this, PresenceActivity.class);
                                backgroundRes = R.drawable.border_redlight;
                                break;
                            case "attestation":
                                intent = new Intent(this, AttestationEmployeActivity.class);
                                backgroundRes = R.drawable.border_orangelight;
                                break;
                        }

                        if (backgroundRes != 0) {
                            // Appliquer le nouveau background
                            notificationView.setBackgroundResource(backgroundRes);
                            selectedPosition[0] = position;

                            // Animation de retour après un délai
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (notificationView != null && originalBackgrounds.size() > position) {
                                    notificationView.setBackground(originalBackgrounds.get(position));
                                    selectedPosition[0] = -1;
                                }
                            }, 300); // Réduit à 300ms pour une meilleure expérience
                        }

                        // Lancer l'activité
                        if (intent != null) {
                            startActivity(intent);
                        }
                    });

                    notifRecentesContainer.addView(notificationView);
                }
            }

            Log.d(TAG, notifications.size() + " notifications affichées");

        } catch (Exception e) {
            Log.e(TAG, "Erreur displayNotifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void navigateToActivity(String type) {
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


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== onResume ===");

        // Vérifier si c'est un nouveau jour
        checkAndResetForNewDay();

        // Recharger les données
        if (currentUser != null) {
            loadEmployeData();
            loadNotificationsRecentetes();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== onDestroy ===");

        // Nettoyer les écouteurs
        if (presenceListener != null) presenceListener.remove();
        if (congesListener != null) congesListener.remove();
        if (attestationsListener != null) attestationsListener.remove();
        if (reunionsListener != null) reunionsListener.remove();
        if (employeListener != null) employeListener.remove();

        backgroundExecutor.shutdown();
    }

    // Classe interne pour représenter une notification
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
}