package com.example.rhapp;

import android.content.Intent;
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

    // Écouteurs Firestore
    private ListenerRegistration congesListener;
    private ListenerRegistration attestationsListener;
    private ListenerRegistration reunionsListener;
    private ListenerRegistration presenceListener;

    // Thread management
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Données de l'employé
    private String employeeId = "";
    private String employeeNom = "";
    private String employeePrenom = "";
    private String employeePoste = "";
    private String employeeDepartement = "";
    private int soldeCongeValue = 25;

    // Liste pour stocker les notifications
    private List<NotificationItem> notifications = new ArrayList<>();

    // Classe interne pour représenter une notification
    private static class NotificationItem {
        String type; // "CONGE", "ATTESTATION", "REUNION"
        String title;
        String subtitle;
        long timestamp;
        int iconResId;
        int backgroundResId;
        String documentId;

        NotificationItem(String type, String title, String subtitle, long timestamp,
                         int iconResId, int backgroundResId, String documentId) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.timestamp = timestamp;
            this.iconResId = iconResId;
            this.backgroundResId = backgroundResId;
            this.documentId = documentId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_employe);

        Log.d(TAG, "onCreate: Création de l'activité");

        // Initialisation des vues
        initializeViews();
        setupClickListeners();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Vérifier si l'utilisateur est connecté
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        Log.d(TAG, "Utilisateur connecté: " + currentUser.getUid() + ", email: " + currentUser.getEmail());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Démarrage de l'activité");

        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Charger toutes les données
        loadEmployeeProfileData();
        setupFirestoreListeners();
        loadAllNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vérifier la présence
       // checkTodayPresence();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Arrêt des écouteurs");
        cleanupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Nettoyage complet");
        cleanupListeners();
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews : initialisation des vues");

        // Header views
        employeConnecte = findViewById(R.id.employeConnecte);
        posteDepartement = findViewById(R.id.posteDepartement);

        // Sous header
        presence = findViewById(R.id.presence);
        conges = findViewById(R.id.conges);
        reunions = findViewById(R.id.reunions);

        // Statistiques principales
        etatPrsence = findViewById(R.id.etatPrsence);
        soldeConge = findViewById(R.id.soldeConge);
        reunionVenir = findViewById(R.id.reunionVenir);

        // Notifications badges
        notifPresence = findViewById(R.id.notifPresence);
        notifConge = findViewById(R.id.notifConge);
        notifAttestation = findViewById(R.id.notifAttestation);
        notifReunion = findViewById(R.id.notifReunion);

        // Actions rapides
        actionPresence = findViewById(R.id.actionPresence);
        actionConge = findViewById(R.id.actionConge);
        actionReunions = findViewById(R.id.actionReunions);
        attestation = findViewById(R.id.attestation);

        // Container des notifications récentes
        notifRecentesContainer = findViewById(R.id.notifRecententesContainer);
        aucuneNotif = findViewById(R.id.aucuneNotif);

        // Footer navigation
        presencefooter = findViewById(R.id.presencefooter);
        congesfooter = findViewById(R.id.congesfooter);
        reunionsfooter = findViewById(R.id.reunionsfooter);
        profilefooter = findViewById(R.id.profilefooter);

        // Initialiser les badges comme invisibles
       // if (notifPresence != null) notifPresence.setVisibility(View.GONE);
       // if (notifConge != null) notifConge.setVisibility(View.GONE);
       // if (notifAttestation != null) notifAttestation.setVisibility(View.GONE);
       // if (notifReunion != null) notifReunion.setVisibility(View.GONE);

        // Afficher un message temporaire
       /* if (employeConnecte != null) {
            employeConnecte.setText("Chargement...");
        }*/
    }

    private void setupClickListeners() {
        // Sous header nav
        if (presence != null) {
            presence.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, PresenceActivity.class));
            });
        }

        if (conges != null) {
            conges.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, CongesEmployeActivity.class));
            });
        }

        if (reunions != null) {
            reunions.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ReunionEmployeActivity.class));
            });
        }

        // Actions rapides
        if (actionPresence != null) {
            actionPresence.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, PresenceActivity.class));
            });
        }

        if (actionConge != null) {
            actionConge.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, CongesEmployeActivity.class));
            });
        }

        if (actionReunions != null) {
            actionReunions.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ReunionEmployeActivity.class));
            });
        }

        if (attestation != null) {
            attestation.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, AttestationEmployeActivity.class));
            });
        }

        // Footer navigation
        if (presencefooter != null) {
            presencefooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, PresenceActivity.class));
            });
        }

        if (congesfooter != null) {
            congesfooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, CongesEmployeActivity.class));
            });
        }

        if (reunionsfooter != null) {
            reunionsfooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ReunionEmployeActivity.class));
            });
        }

        if (profilefooter != null) {
            profilefooter.setOnClickListener(v -> {
                startActivity(new Intent(AcceuilEmployeActivity.this, ProfileEmployeActivity.class));
            });
        }
    }

    /**
     * Charger les données du profil employé
     */
    private void loadEmployeeProfileData() {
        if (currentUser == null || isFinishing()) return;

        String userEmail = currentUser.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            Log.e(TAG, "Email utilisateur null ou vide");
            displayDefaultName();
            return;
        }

        String searchEmail = userEmail.toLowerCase(Locale.ROOT).trim();
        Log.d(TAG, "Recherche employé avec email: " + searchEmail);

        db.collection("employees")
                .whereEqualTo("email", searchEmail)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot employeeDoc = task.getResult().getDocuments().get(0);
                        employeeId = employeeDoc.getId();

                        // Récupérer les données
                        employeeNom = employeeDoc.getString("nom") != null ? employeeDoc.getString("nom") : "";
                        employeePrenom = employeeDoc.getString("prenom") != null ? employeeDoc.getString("prenom") : "";
                        employeePoste = employeeDoc.getString("poste") != null ? employeeDoc.getString("poste") : "";
                        employeeDepartement = employeeDoc.getString("departement") != null ? employeeDoc.getString("departement") : "";

                        // Solde de congés
                        Long solde = employeeDoc.getLong("soldeConge");
                        if (solde != null) {
                            soldeCongeValue = solde.intValue();
                        }

                        Log.d(TAG, "Employé trouvé: " + employeePrenom + " " + employeeNom);

                        // Afficher les données
                        displayEmployeeName();
                        updatePosteDepartement();
                        updateSoldeConge();

                    } else {
                        Log.e(TAG, "Employé non trouvé avec email: " + searchEmail);
                        displayDefaultName();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur recherche employé: " + e.getMessage());
                    displayDefaultName();
                });
    }

    /**
     * Vérifier la présence du jour

    private void checkTodayPresence() {
        if (employeeId == null || employeeId.isEmpty()) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        String today = dateFormat.format(new Date());

        db.collection("presences")
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("date", today)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot presenceDoc = task.getResult().getDocuments().get(0);
                        String statut = presenceDoc.getString("statut");

                        if (statut != null) {
                            updatePresenceDisplay(statut);
                        }
                    } else {
                        // Pas de présence marquée
                        Calendar calendar = Calendar.getInstance();
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);

                        if (hour >= 9) {
                            updatePresenceDisplay("non marqué");
                        } else {
                            updatePresenceDisplay("à marquer");
                        }
                    }
                });
    }*/

    /**
     * Mettre à jour l'affichage de la présence
     */
    private void updatePresenceDisplay(String status) {
        runOnUiThread(() -> {
            if (etatPrsence != null && !isFinishing()) {
                switch (status.toLowerCase()) {
                    case "présent":
                    case "present":
                        etatPrsence.setText("Présent");
                        etatPrsence.setTextColor(Color.parseColor("#0FAC71"));
                        break;
                    case "retard":
                        etatPrsence.setText("En retard");
                        etatPrsence.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    case "absent":
                        etatPrsence.setText("Absent");
                        etatPrsence.setTextColor(Color.parseColor("#F44336"));
                        break;
                    case "non marqué":
                        etatPrsence.setText("Non marqué");
                        etatPrsence.setTextColor(Color.parseColor("#FF9800"));
                        break;
                    default:
                        etatPrsence.setText("À marquer");
                        etatPrsence.setTextColor(Color.parseColor("#666666"));
                        break;
                }
            }
        });
    }

    /**
     * Configurer les écouteurs Firestore
     */
    private void setupFirestoreListeners() {
        if (currentUser == null || isFinishing()) {
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Configuration des écouteurs Firestore pour userId: " + userId);

        // Nettoyer les anciens listeners
        cleanupListeners();

        // 1. Écouteur pour les congés en attente
        congesListener = db.collection("conges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écouteur congés: " + error.getMessage());
                        return;
                    }

                    if (isFinishing()) return;

                    int count = 0;
                    if (value != null && !value.isEmpty()) {
                        count = value.size();
                        Log.d(TAG, "Congés en attente: " + count);
                    }

                    updateBadge(notifConge, count);
                    loadAllNotifications();
                });

        // 2. Écouteur pour les attestations en attente
        attestationsListener = db.collection("Attestations")
                .whereEqualTo("employeeId", userId)
                .whereEqualTo("statut", "en_attente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écouteur attestations: " + error.getMessage());
                        return;
                    }

                    if (isFinishing()) return;

                    int count = 0;
                    if (value != null && !value.isEmpty()) {
                        count = value.size();
                        Log.d(TAG, "Attestations en attente: " + count);
                    }

                    updateBadge(notifAttestation, count);
                    loadAllNotifications();
                });

        // 3. Écouteur pour les réunions
        reunionsListener = db.collection("Reunions")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écouteur réunions: " + error.getMessage());
                        return;
                    }

                    if (isFinishing()) return;

                    int upcomingCount = 0;
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                String dateStr = doc.getString("date");
                                if (isUpcomingReunion(dateStr)) {
                                    upcomingCount++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur traitement réunion: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "Réunions à venir: " + upcomingCount);
                    }

                    updateReunionStats(upcomingCount);
                    updateBadge(notifReunion, upcomingCount);
                    loadAllNotifications();
                });

        // 4. Écouteur pour les présences nécessitant attention
        presenceListener = db.collection("presences")
                .whereEqualTo("employeeId", userId)
                .whereEqualTo("needAttention", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écouteur présence: " + error.getMessage());
                        return;
                    }

                    if (isFinishing()) return;

                    int count = 0;
                    if (value != null && !value.isEmpty()) {
                        count = value.size();
                        Log.d(TAG, "Notifications présence: " + count);
                    }

                    updateBadge(notifPresence, count);
                });
    }

    /**
     * Charger toutes les notifications
     */
    private void loadAllNotifications() {
        if (isFinishing() || currentUser == null) return;

        backgroundExecutor.execute(() -> {
            try {
                List<NotificationItem> allNotifications = new ArrayList<>();
                long currentTime = System.currentTimeMillis();

                // 1. Charger les congés récents
                try {
                    QuerySnapshot congesSnapshot = Tasks.await(
                            db.collection("conges")
                                    .whereEqualTo("userId", currentUser.getUid())
                                    .orderBy("dateDemande", Query.Direction.DESCENDING)
                                    .limit(10)
                                    .get()
                    );

                    if (congesSnapshot != null && !congesSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : congesSnapshot.getDocuments()) {
                            try {
                                String statut = doc.getString("statut");
                                String typeConge = doc.getString("typeConge");
                                Date dateDemande = doc.getDate("dateDemande");
                                String docId = doc.getId();

                                // Déterminer le background selon le statut
                                int backgroundResId;
                                if (statut != null && statut.equals("En attente")) {
                                    backgroundResId = R.drawable.recentborder; // Orange pour en attente
                                } else if (statut != null && (statut.equals("Approuvé") || statut.equals("Approuvee"))) {
                                    backgroundResId = R.drawable.approuve_border; // Vert pour approuvé
                                } else if (statut != null && (statut.equals("Refusé") || statut.equals("Refusee"))) {
                                    backgroundResId = R.drawable.border_redlight; // Rouge pour refusé
                                } else {
                                    backgroundResId = R.drawable.recentborder2; // Bleu pour autres
                                }

                                String title = "Demande de congé " + (typeConge != null ? typeConge : "");
                                String subtitle;
                                if (statut != null && statut.equals("En attente")) {
                                    subtitle = "En cours de validation par RH";
                                } else {
                                    subtitle = "Statut: " + statut;
                                }

                                NotificationItem notification = new NotificationItem(
                                        "CONGE",
                                        title,
                                        subtitle,
                                        dateDemande != null ? dateDemande.getTime() : currentTime,
                                        getCongeIcon(statut),
                                        backgroundResId,
                                        docId
                                );
                                allNotifications.add(notification);
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur création notification congé: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement congés: " + e.getMessage());
                }

                // 2. Charger les attestations récentes
                try {
                    QuerySnapshot attestationsSnapshot = Tasks.await(
                            db.collection("Attestations")
                                    .whereEqualTo("employeeId", currentUser.getUid())
                                    .orderBy("dateDemande", Query.Direction.DESCENDING)
                                    .limit(10)
                                    .get()
                    );

                    if (attestationsSnapshot != null && !attestationsSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : attestationsSnapshot.getDocuments()) {
                            try {
                                String statut = doc.getString("statut");
                                String typeAttestation = doc.getString("typeAttestation");
                                Date dateDemande = doc.getDate("dateDemande");
                                String docId = doc.getId();

                                // Déterminer le background selon le statut
                                int backgroundResId;
                                if (statut != null && statut.equals("en_attente")) {
                                    backgroundResId = R.drawable.recentborder; // Orange pour en attente
                                } else if (statut != null && (statut.equals("approuvée") || statut.equals("approuvee"))) {
                                    backgroundResId = R.drawable.approuve_border; // Vert pour approuvé
                                } else if (statut != null && (statut.equals("refusée") || statut.equals("refusee"))) {
                                    backgroundResId = R.drawable.border_redlight; // Rouge pour refusé
                                } else {
                                    backgroundResId = R.drawable.recentborder2; // Bleu pour autres
                                }

                                String title = "Demande d'attestation " + (typeAttestation != null ? typeAttestation : "");
                                String subtitle;
                                if (statut != null && statut.equals("en_attente")) {
                                    subtitle = "En cours de validation par RH";
                                } else {
                                    subtitle = "Statut: " + statut;
                                }

                                NotificationItem notification = new NotificationItem(
                                        "ATTESTATION",
                                        title,
                                        subtitle,
                                        dateDemande != null ? dateDemande.getTime() : currentTime,
                                        getAttestationIcon(statut),
                                        backgroundResId,
                                        docId
                                );
                                allNotifications.add(notification);
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur création notification attestation: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement attestations: " + e.getMessage());
                }

                // 3. Charger les réunions récentes
                try {
                    QuerySnapshot reunionsSnapshot = Tasks.await(
                            db.collection("Reunions")
                                    .orderBy("date", Query.Direction.DESCENDING)
                                    .limit(10)
                                    .get()
                    );

                    if (reunionsSnapshot != null && !reunionsSnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : reunionsSnapshot.getDocuments()) {
                            try {
                                String titre = doc.getString("titre");
                                String dateStr = doc.getString("date");
                                String docId = doc.getId();

                                // Toujours bleu pour les réunions
                                int backgroundResId = R.drawable.recentborder2;

                                String title = "Réunion: " + (titre != null ? titre : "");
                                String subtitle = formatTimeAgo(dateStr);

                                NotificationItem notification = new NotificationItem(
                                        "REUNION",
                                        title,
                                        subtitle,
                                        currentTime,
                                        R.drawable.userpurple,
                                        backgroundResId,
                                        docId
                                );
                                allNotifications.add(notification);
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur création notification réunion: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement réunions: " + e.getMessage());
                }

                // Trier par timestamp (plus récent en premier)
                Collections.sort(allNotifications, (a1, a2) ->
                        Long.compare(a2.timestamp, a1.timestamp));

                // Garder seulement les 5 plus récentes
                final List<NotificationItem> recent = allNotifications.size() > 5 ?
                        allNotifications.subList(0, 5) : allNotifications;

                mainHandler.post(() -> {
                    if (!isFinishing()) {
                        notifications.clear();
                        notifications.addAll(recent);
                        displayNotifications();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la récupération des notifications: " + e.getMessage());
                mainHandler.post(() -> {
                    if (!isFinishing()) {
                        showNoNotificationsMessage();
                    }
                });
            }
        });
    }

    /**
     * Afficher les notifications dans le layout personnalisé
     */
    private void displayNotifications() {
        runOnUiThread(() -> {
            if (isFinishing() || notifRecentesContainer == null) return;

            notifRecentesContainer.removeAllViews();

            if (notifications.isEmpty()) {
                showNoNotificationsMessage();
                return;
            }

            // Masquer le message "Aucune notification"
            if (aucuneNotif != null) {
                aucuneNotif.setVisibility(View.GONE);
            }

            // S'assurer que le container est visible
            notifRecentesContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < notifications.size(); i++) {
                NotificationItem notification = notifications.get(i);
                try {
                    // Créer le layout de notification personnalisé
                    View notificationView = LayoutInflater.from(AcceuilEmployeActivity.this)
                            .inflate(R.layout.layout_notification_item, notifRecentesContainer, false);

                    // Appliquer le background selon le type
                    LinearLayout container = notificationView.findViewById(R.id.notificationContainer);
                    if (notification.backgroundResId != 0) {
                        container.setBackgroundResource(notification.backgroundResId);
                    }

                    // Configurer l'icône
                    ImageView icon = notificationView.findViewById(R.id.notificationIcon);
                    if (icon != null && notification.iconResId != 0) {
                        icon.setImageResource(notification.iconResId);
                    }

                    // Configurer le titre
                    TextView title = notificationView.findViewById(R.id.notificationTitle);
                    if (title != null) {
                        title.setText(notification.title);
                    }

                    // Configurer le sous-titre
                    TextView subtitle = notificationView.findViewById(R.id.notificationSubtitle);
                    if (subtitle != null) {
                        subtitle.setText(notification.subtitle);
                    }

                    // Ajouter un click listener
                    final int index = i;
                    notificationView.setOnClickListener(v -> {
                        if (index < notifications.size()) {
                            navigateToNotification(notifications.get(index).type);
                        }
                    });

                    // Ajouter la vue au container
                    notifRecentesContainer.addView(notificationView);

                    // Ajouter une marge entre les notifications
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) notificationView.getLayoutParams();
                    /*if (i < notifications.size() - 1) {
                        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.notification_margin);
                    }*/
                    notificationView.setLayoutParams(params);

                } catch (Exception e) {
                    Log.e(TAG, "Erreur création vue notification: " + e.getMessage());
                }
            }

            // Forcer le re-layout
            notifRecentesContainer.requestLayout();
        });
    }

    private void showNoNotificationsMessage() {
        runOnUiThread(() -> {
            if (aucuneNotif != null && notifRecentesContainer != null && !isFinishing()) {
                aucuneNotif.setVisibility(View.VISIBLE);
                notifRecentesContainer.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Formater "Il y'a X" pour les réunions
     */
    private String formatTimeAgo(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            Date reunionDate = sdf.parse(dateStr);

            if (reunionDate != null) {
                long diff = System.currentTimeMillis() - reunionDate.getTime();
                long days = diff / (24 * 60 * 60 * 1000);

                if (days > 0) {
                    return "Il y'a " + days + " jour" + (days > 1 ? "s" : "");
                } else {
                    return "Aujourd'hui";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur formatage date: " + e.getMessage());
        }
        return dateStr != null ? dateStr : "";
    }

    private void navigateToNotification(String type) {
        Intent intent = null;

        switch (type) {
            case "CONGE":
                intent = new Intent(this, CongesEmployeActivity.class);
                break;
            case "ATTESTATION":
                intent = new Intent(this, AttestationEmployeActivity.class);
                break;
            case "REUNION":
                intent = new Intent(this, ReunionEmployeActivity.class);
                break;
            default:
                return;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    /**
     * Afficher le nom de l'employé
     */
    private void displayEmployeeName() {
        runOnUiThread(() -> {
            if (employeConnecte != null && !isFinishing()) {
                String fullName;

                if (!employeePrenom.isEmpty() && !employeeNom.isEmpty()) {
                    fullName = employeePrenom + " " + employeeNom.toUpperCase();
                } else if (!employeeNom.isEmpty()) {
                    fullName = employeeNom.toUpperCase();
                } else if (!employeePrenom.isEmpty()) {
                    fullName = employeePrenom.toUpperCase();
                } else {
                    fullName = currentUser != null && currentUser.getDisplayName() != null ?
                            currentUser.getDisplayName().toUpperCase() : "EMPLOYÉ";
                }

                employeConnecte.setText(fullName);
            }
        });
    }

    private void displayDefaultName() {
        runOnUiThread(() -> {
            if (employeConnecte != null && !isFinishing()) {
                String defaultName = currentUser != null && currentUser.getDisplayName() != null ?
                        currentUser.getDisplayName().toUpperCase() : "EMPLOYÉ";
                employeConnecte.setText(defaultName);
            }
        });
    }

    private void updatePosteDepartement() {
        runOnUiThread(() -> {
            if (posteDepartement != null && !isFinishing()) {
                String posteDept = "";

                if (!employeePoste.isEmpty() && !employeeDepartement.isEmpty()) {
                    posteDept = employeePoste + " • " + employeeDepartement;
                } else if (!employeePoste.isEmpty()) {
                    posteDept = employeePoste;
                } else if (!employeeDepartement.isEmpty()) {
                    posteDept = employeeDepartement;
                }

                if (!posteDept.isEmpty()) {
                    posteDepartement.setText(posteDept);
                }
            }
        });
    }

    private void updateSoldeConge() {
        runOnUiThread(() -> {
            if (soldeConge != null && !isFinishing()) {
                soldeConge.setText(soldeCongeValue + " jours");
                soldeConge.setTextColor(Color.parseColor("#4669EB"));
            }
        });
    }

    private void updateReunionStats(int upcomingCount) {
        runOnUiThread(() -> {
            if (reunionVenir != null && !isFinishing()) {
                reunionVenir.setText(upcomingCount + " à venir");
                reunionVenir.setTextColor(Color.parseColor("#D000D0"));
            }
        });
    }

    private void updateBadge(TextView badgeView, int count) {
        if (badgeView != null && !isFinishing()) {
            runOnUiThread(() -> {
                if (count > 0) {
                    badgeView.setText(String.valueOf(count));
                    badgeView.setVisibility(View.VISIBLE);

                    // Animation du badge
                    badgeView.setScaleX(0.5f);
                    badgeView.setScaleY(0.5f);
                    badgeView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start();
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            });
        }
    }

    private boolean isUpcomingReunion(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            Date reunionDate = sdf.parse(dateStr);
            Date today = new Date();

            if (reunionDate == null) return false;

            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyyMMdd", Locale.FRENCH);
            String reunionDateStr = dateOnly.format(reunionDate);
            String todayStr = dateOnly.format(today);

            return reunionDateStr.compareTo(todayStr) >= 0;
        } catch (Exception e) {
            Log.e(TAG, "Erreur parsing date réunion: " + e.getMessage());
            return false;
        }
    }

    private int getCongeIcon(String statut) {
        if (statut == null) return R.drawable.orangecalendar;

        switch (statut.toLowerCase()) {
            case "en attente":
                return R.drawable.orangecalendar;
            case "approuvé":
            case "approuvee":
                return R.drawable.approuve;
            case "refusé":
            case "refusee":
                return R.drawable.refuse;
            default:
                return R.drawable.orangecalendar;
        }
    }

    private int getAttestationIcon(String statut) {
        if (statut == null) return R.drawable.docpurple;

        switch (statut.toLowerCase()) {
         //   case "en_attente":
              //  return R.drawable.docpurple;
         //   case "approuvée":
         //   case "approuvee":
           //     return R.drawable.greendoc;
         //   case "refusée":
        //    case "refusee":
          //      return R.drawable.reddoc;
           default:
                return R.drawable.docpurple;
        }
    }

    private void redirectToLogin() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Session expirée. Veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void cleanupListeners() {
        if (congesListener != null) {
            congesListener.remove();
            congesListener = null;
        }
        if (attestationsListener != null) {
            attestationsListener.remove();
            attestationsListener = null;
        }
        if (reunionsListener != null) {
            reunionsListener.remove();
            reunionsListener = null;
        }
        if (presenceListener != null) {
            presenceListener.remove();
            presenceListener = null;
        }
    }
}