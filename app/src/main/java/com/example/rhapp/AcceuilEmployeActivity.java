package com.example.rhapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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

        // Charger les données de l'employé connecté
        loadEmployeData();

        // Configurer les écouteurs Firestore
        setupFirestoreListeners();

        // Charger les notifications
        loadNotificationsRecentetes();
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
                        boolean presenceMarquee = documentSnapshot != null &&
                                documentSnapshot.exists() &&
                                "present".equals(documentSnapshot.getString("status"));

                        mainHandler.post(() -> {
                            try {
                                if (etatPrsence != null) {
                                    if (presenceMarquee) {
                                        etatPrsence.setText("Marquée");
                                        etatPrsence.setTextColor(Color.parseColor("#0FAC71"));
                                        iconepresence.setImageResource(R.drawable.approuve);
                                        if (notifPresence != null) {
                                            notifPresence.setVisibility(View.GONE);
                                        }
                                    } else {

                                        etatPrsence.setText("Non marquée");
                                        etatPrsence.setTextColor(Color.parseColor("#FF0000"));
                                        iconepresence.setImageResource(R.drawable.refuse);

                                        if (notifPresence != null) {
                                            notifPresence.setVisibility(View.VISIBLE);
                                            notifPresence.setText("!");
                                        }
                                    }
                                    Log.d(TAG, "Présence mise à jour: " + (presenceMarquee ? "Marquée" : "Non marquée"));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur mise à jour présence UI: " + e.getMessage());
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

                            if (querySnapshot != null) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    String dateStr = doc.getString("date");
                                    String timeStr = doc.getString("heure");

                                    if (dateStr != null && timeStr != null) {
                                        int etatDate = compareDate(dateStr, timeStr);
                                        if (etatDate == 1) { // À venir
                                            reunionsAVenir++;
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
                        }
                    });
                });
    }

    private void setupCongesListener() {
        if (currentUser == null) return;

        Log.d(TAG, "Configuration écouteur congés pour userId: " + currentUser.getUid());

        congesListener = db.collection("conges")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute congés: " + error.getMessage());
                        return;
                    }

                    backgroundExecutor.execute(() -> {
                        int congesEnAttente = querySnapshot != null ? querySnapshot.size() : 0;

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
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date reunionDate = dateFormat.parse(date + " " + time);
            Date now = new Date();

            if (reunionDate == null) return -1;

            if (reunionDate.after(now)) return 1; // À venir
            if (reunionDate.before(now)) return -1; // Passée
            return 0; // Aujourd'hui

        } catch (Exception e) {
            Log.e(TAG, "Erreur comparaison date: " + e.getMessage());
            return -1;
        }
    }

    private void loadNotificationsRecentetes() {
        Log.d(TAG, "Chargement des notifications récentes");

        backgroundExecutor.execute(() -> {
            try {
                List<NotificationItem> notifications = new ArrayList<>();

                // 1. Congés en attente
                if (currentUser != null) {
                    QuerySnapshot congesSnapshot = Tasks.await(
                            db.collection("conges")
                                    .whereEqualTo("userId", currentUser.getUid())
                                    .whereEqualTo("statut", "En attente")
                                    .orderBy("dateDemande", Query.Direction.DESCENDING)
                                    .limit(3)
                                    .get()
                    );

                    for (DocumentSnapshot doc : congesSnapshot.getDocuments()) {
                        String type = doc.getString("typeConge");
                        Date dateDemande = doc.getDate("dateDemande");
                        String date = (dateDemande != null) ?
                                new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(dateDemande) :
                                "Date inconnue";

                        notifications.add(new NotificationItem(
                                "Demande de congé en attente",
                                "En cours de validation par RH - " + date,
                                R.drawable.docpurple,
                                "conges"
                        ));
                    }
                }

                // 2. Réunions à venir (les 2 plus proches)
                try {
                    // Obtenir la date d'aujourd'hui au format "dd/MM/yyyy"
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
                    String today = dateFormat.format(cal.getTime());

                    // Option 1: Récupérer les 2 premières réunions à partir d'aujourd'hui
                    QuerySnapshot reunionsSnapshot = Tasks.await(
                            db.collection("Reunions")
                                    .whereGreaterThanOrEqualTo("date", today) // À partir d'aujourd'hui
                                    .orderBy("date", Query.Direction.ASCENDING) // Plus proche en premier
                                    .orderBy("heure", Query.Direction.ASCENDING) // Puis par heure
                                    .limit(2) // Seulement 2 résultats
                                    .get()
                    );

                    for (DocumentSnapshot doc : reunionsSnapshot.getDocuments()) {
                        String titre = doc.getString("titre");
                        String date = doc.getString("date");
                        String heure = doc.getString("heure");

                        if (titre != null && date != null && heure != null) {
                            notifications.add(new NotificationItem(
                                    titre,
                                    "Réunion le " + date + " à " + heure,
                                    R.drawable.userb,
                                    "reunion"
                            ));
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Erreur chargement réunions: " + e.getMessage());
                }
                // 3. Présence non marquée (si après 9h du matin)
                Calendar cal = Calendar.getInstance();
                if (cal.get(Calendar.HOUR_OF_DAY) >= 9) {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    DocumentSnapshot presenceDoc = Tasks.await(
                            db.collection("Presence")
                                    .document(currentUser.getUid())
                                    .collection("history")
                                    .document(today)
                                    .get()
                    );

                    if (!presenceDoc.exists() || !"present".equals(presenceDoc.getString("status"))) {
                        notifications.add(new NotificationItem(
                                "Présence non marquée",
                                "N'oubliez pas de marquer votre présence aujourd'hui",
                                R.drawable.alerticon,
                                "presence"
                        ));
                    }
                }

                // 4. Attestations en attente
                if (currentUser != null) {
                    QuerySnapshot attestationsSnapshot = Tasks.await(
                            db.collection("Attestations")
                                    .whereEqualTo("employeeId", currentUser.getUid())
                                    .whereEqualTo("statut", "en_attente")
                                    .limit(2)
                                    .get()
                    );

                    for (DocumentSnapshot doc : attestationsSnapshot.getDocuments()) {
                        String type = doc.getString("typeAttestation");

                        notifications.add(new NotificationItem(
                                "Demande d'attestation en attente",
                                (type != null ? type : "Attestation") + " - En cours de traitement",
                                R.drawable.attestation,
                                "attestation"
                        ));
                    }
                }

                // Trier par date/priorité et limiter à 7
                Collections.sort(notifications, (n1, n2) -> {
                    int priority1 = getPriority(n1.type);
                    int priority2 = getPriority(n2.type);
                    return Integer.compare(priority2, priority1);
                });

                final List<NotificationItem> finalNotifications =
                        notifications.subList(0, Math.min(notifications.size(), 7));

                mainHandler.post(() -> {
                    displayNotifications(finalNotifications);
                    Log.d(TAG, "Notifications affichées: " + finalNotifications.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement notifications: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private int getPriority(String type) {
        switch (type) {
            case "presence": return 4;
            case "conges": return 3;
            case "reunion": return 2;
            case "attestation": return 1;
            default: return 0;
        }
    }

    private void displayNotifications(List<NotificationItem> notifications) {
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

            // Pour suivre quelle notification est actuellement sélectionnée
            final int[] selectedPosition = {-1};

            for (int i = 0; i < notifications.size(); i++) {
                NotificationItem notif = notifications.get(i);
                final int position = i; // Position finale pour le click listener

                View notificationView = inflater.inflate(R.layout.layout_notification_item,
                        notifRecentesContainer, false);

                ImageView icon = notificationView.findViewById(R.id.notificationIcon);
                TextView title = notificationView.findViewById(R.id.notificationTitle);
                TextView subtitle = notificationView.findViewById(R.id.notificationSubtitle);

                if (icon != null && title != null && subtitle != null) {
                    icon.setImageResource(notif.iconRes);
                    title.setText(notif.title);
                    subtitle.setText(notif.subtitle);

                    // Stocker le drawable original pour le restaurer plus tard
                    final Drawable originalBackground = notificationView.getBackground();

                    // Ajouter le clic
                    notificationView.setOnClickListener(v -> {
                        Intent intent = null;
                        int backgroundRes = 0;

                        // Réinitialiser le background de l'ancienne notification sélectionnée
                        if (selectedPosition[0] != -1 && selectedPosition[0] < notifRecentesContainer.getChildCount()) {
                            View previousView = notifRecentesContainer.getChildAt(selectedPosition[0]);
                            if (previousView != null) {
                                previousView.setBackground(originalBackground);
                            }
                        }

                        // Appliquer le nouveau background
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

                        // Appliquer le background avec animation
                        if (backgroundRes != 0) {
                            notificationView.setBackgroundResource(backgroundRes);
                            selectedPosition[0] = position;

                            // Animation de retour après 500ms (si l'utilisateur reste sur l'activité)
                            new Handler().postDelayed(() -> {
                                notificationView.setBackground(originalBackground);
                                selectedPosition[0] = -1;
                            }, 500);
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
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== onResume ===");

        // Recharger les données quand l'activité revient au premier plan
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