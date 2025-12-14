package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcceuilRhActivity extends AppCompatActivity {

    private static final String TAG = "AcceuilRhActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextView aucuneActiviteText;

    // Écouteurs Firestore
    private ListenerRegistration congesListener;
    private ListenerRegistration attestationsListener;
    private ListenerRegistration reunionsListener;
    private ListenerRegistration employesListener;
    private ListenerRegistration userListener;
    private ListenerRegistration activitesCongesListener;
    private ListenerRegistration activitesAttestationsListener;
    private ListenerRegistration presenceStatsListener;

    // TextViews pour les statistiques
    private TextView congeEnAttente, totalPresents, totalEmploye, attestationEnAttente;
    private TextView notifPresence, notifConge, notifAttestation, notifReunion;
    private TextView rhConnecte;
    private TextView absenceNonJustifie;
    private LinearLayout activitesRecentesContainer;
    private LinearLayout alertCard;

    // Executor pour les opérations en arrière-plan
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceuil_rh);

        Log.d(TAG, "onCreate: Démarrage de l'activité");

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialiser les vues
        initializeViews();

        // Configurer la navigation
        gererNavigation();

        // Définir les valeurs par défaut immédiatement
        setDefaultValues();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Démarrage des écouteurs");

        // Démarrer tous les écouteurs Firestore
        setupFirestoreListeners();
        loadUserData();
        setupActivitesListener();
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
        Log.d(TAG, "initializeViews: Initialisation des vues");

        // Initialiser les statistiques
        congeEnAttente = findViewById(R.id.congeEnAttente);
        totalEmploye = findViewById(R.id.totalEmploye);
        totalPresents = findViewById(R.id.totalPresents);
        attestationEnAttente = findViewById(R.id.attestationEnAttente);
        rhConnecte = findViewById(R.id.rhConnecte);
        absenceNonJustifie = findViewById(R.id.absenceNonJustifie);

        // Initialiser les badges de notification
        notifPresence = findViewById(R.id.notifPresence);
        notifConge = findViewById(R.id.notifConge);
        notifAttestation = findViewById(R.id.notifAttestation);
        notifReunion = findViewById(R.id.notifReunion);

        // Initialiser le conteneur d'activités récentes
        activitesRecentesContainer = findViewById(R.id.activitesRecentesContainer);
        aucuneActiviteText = findViewById(R.id.aucuneActiviteText);

        // Initialiser la carte d'alerte
        alertCard = findViewById(R.id.alertCard);
    }

    private void setDefaultValues() {
        runOnUiThread(() -> {
            // Valeurs par défaut pour éviter les null et donner un feedback immédiat
            if (congeEnAttente != null) congeEnAttente.setText("0");
            if (totalEmploye != null) totalEmploye.setText("0");
            if (totalPresents != null) totalPresents.setText("0");
            if (attestationEnAttente != null) attestationEnAttente.setText("0");
            if (absenceNonJustifie != null) absenceNonJustifie.setText("0");

            // Cacher tous les badges de notification au démarrage
            if (notifPresence != null) notifPresence.setVisibility(View.GONE);
            if (notifConge != null) notifConge.setVisibility(View.GONE);
            if (notifAttestation != null) notifAttestation.setVisibility(View.GONE);
            if (notifReunion != null) notifReunion.setVisibility(View.GONE);

            // Afficher le message "Aucune activité" par défaut
            if (aucuneActiviteText != null) {
                aucuneActiviteText.setVisibility(View.VISIBLE);
            }

            // Cacher la carte d'alerte par défaut
            if (alertCard != null) {
                alertCard.setVisibility(View.GONE);
            }
        });
    }

    private void setupFirestoreListeners() {
        Log.d(TAG, "setupFirestoreListeners: Configuration des écouteurs");

        // Utiliser l'executor pour les opérations lourdes
        backgroundExecutor.execute(() -> {
            setupCongesListener();
            setupAttestationsListener();
            setupReunionsListener();
            setupEmployesListener();
            setupPresenceStatsListener();
        });
    }

    private void setupPresenceStatsListener() {
        Log.d(TAG, "setupPresenceStatsListener: Configuration écouteur statistiques présence");

        // Obtenir la date d'aujourd'hui au format yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        presenceStatsListener = db.collection("PresenceHistory")
                .whereEqualTo("date", todayDate)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur présence stats: " + error.getMessage());
                        return;
                    }

                    // Charger le nombre total d'employés d'abord
                    db.collection("employees").get().addOnSuccessListener(employeeSnapshots -> {
                        int totalEmployees = employeeSnapshots != null ? employeeSnapshots.size() : 0;

                        // Compter les présents
                        int presents = 0;
                        int absentsJustifies = 0;
                        int conges = 0;

                        if (value != null && !value.isEmpty()) {
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                String status = doc.getString("status");
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
                        int absentsNonJustifies = Math.max(0, totalEmployees - totalMarked);

                        Log.d(TAG, "Statistiques présence - Total: " + totalEmployees +
                                ", Présents: " + presents +
                                ", Absents non justifiés: " + absentsNonJustifies);

                        // Mettre à jour les statistiques
                        updatePresenceStats(totalEmployees, presents, absentsNonJustifies);
                        updateDepartmentStats(todayDate);

                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur chargement employés: " + e.getMessage());
                    });
                });
    }

    private void updatePresenceStats(int totalEmployees, int presents, int absentsNonJustifies) {
        runOnUiThread(() -> {
            try {
                // Mettre à jour le nombre de présents
                if (totalPresents != null) {
                    totalPresents.setText(String.valueOf(presents));
                }

                // Mettre à jour le nombre d'absences non justifiées
                if (absenceNonJustifie != null) {
                    absenceNonJustifie.setText(String.valueOf(absentsNonJustifies));
                }

                // Afficher/masquer la carte d'alerte selon le nombre d'absences
                if (alertCard != null) {
                    if (absentsNonJustifies > 0) {
                        alertCard.setVisibility(View.VISIBLE);

                        // Ajouter un écouteur sur le bouton "Voir"
                        View btnVoir = alertCard.findViewById(R.id.btnVoirAbsences);
                        if (btnVoir == null) {
                            // Si le bouton n'a pas d'ID spécifique, trouver le TextView "Voir"
                            LinearLayout parentLayout = (LinearLayout) alertCard.getChildAt(0);
                            for (int i = 0; i < parentLayout.getChildCount(); i++) {
                                View child = parentLayout.getChildAt(i);
                                if (child instanceof TextView) {
                                    TextView textView = (TextView) child;
                                    if ("Voir".equals(textView.getText().toString())) {
                                        btnVoir = textView;
                                        break;
                                    }
                                }
                            }
                        }

                        if (btnVoir != null) {
                            btnVoir.setOnClickListener(v -> {
                                // Naviguer vers l'activité des présences
                                startActivity(new Intent(AcceuilRhActivity.this, PresenceRhActivity.class));
                            });
                        }
                    } else {
                        alertCard.setVisibility(View.GONE);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur updatePresenceStats: " + e.getMessage());
            }
        });
    }

    private void updateDepartmentStats(String todayDate) {
        // Récupérer tous les employés
        db.collection("employees").get().addOnSuccessListener(employeeSnapshots -> {
            if (employeeSnapshots == null || employeeSnapshots.isEmpty()) {
                Log.d(TAG, "Aucun employé trouvé pour les statistiques par département");
                return;
            }

            // Map pour stocker les statistiques par département
            Map<String, DepartmentStats> statsMap = new HashMap<>();

            // Initialiser les statistiques pour chaque département
            for (DocumentSnapshot employeeDoc : employeeSnapshots.getDocuments()) {
                String departement = employeeDoc.getString("departement");
                if (departement != null && !departement.isEmpty()) {
                    if (!statsMap.containsKey(departement)) {
                        statsMap.put(departement, new DepartmentStats(departement));
                    }
                    statsMap.get(departement).totalEmployees++;
                }
            }

            // Récupérer les présences pour aujourd'hui
            db.collection("PresenceHistory")
                    .whereEqualTo("date", todayDate)
                    .get()
                    .addOnSuccessListener(presenceSnapshots -> {
                        if (presenceSnapshots != null && !presenceSnapshots.isEmpty()) {
                            for (DocumentSnapshot presenceDoc : presenceSnapshots.getDocuments()) {
                                String userId = presenceDoc.getString("userId");
                                String status = presenceDoc.getString("status");

                                if ("present".equals(status) && userId != null) {
                                    // Trouver l'employé correspondant
                                    for (DocumentSnapshot employeeDoc : employeeSnapshots.getDocuments()) {
                                        String employeeEmail = employeeDoc.getString("email");
                                        String employeeId = employeeDoc.getId();

                                        // Vérifier par email ou par ID
                                        if (userId.equals(employeeEmail) || userId.equals(employeeId)) {
                                            String departement = employeeDoc.getString("departement");
                                            if (departement != null && statsMap.containsKey(departement)) {
                                                statsMap.get(departement).presents++;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        // Mettre à jour l'UI avec les statistiques
                        runOnUiThread(() -> {
                            updateDepartmentStatsUI(statsMap);
                        });

                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur chargement présences pour stats département: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erreur chargement employés pour stats département: " + e.getMessage());
        });
    }

    // Classe pour stocker les statistiques par département
    private static class DepartmentStats {
        String name;
        int totalEmployees;
        int presents;

        DepartmentStats(String name) {
            this.name = name;
            this.totalEmployees = 0;
            this.presents = 0;
        }

        double getPresenceRate() {
            if (totalEmployees == 0) return 0.0;
            return (double) presents / totalEmployees * 100.0;
        }
    }

    private void updateDepartmentStatsUI(Map<String, DepartmentStats> statsMap) {
        try {
            // IDs des vues de statistiques dans votre XML
            String[] statIds = {"statDev", "statMarketing", "statSales", "statSport"};
            int index = 0;

            // Parcourir les départements et mettre à jour les vues
            for (Map.Entry<String, DepartmentStats> entry : statsMap.entrySet()) {
                if (index >= statIds.length) break;

                DepartmentStats stats = entry.getValue();
                int viewId = getResources().getIdentifier(statIds[index], "id", getPackageName());
                LinearLayout statView = findViewById(viewId);

                if (statView != null) {
                    TextView tvTitle = statView.findViewById(R.id.tvTitle);
                    TextView tvPercent = statView.findViewById(R.id.tvPercent);
                    View barFilled = statView.findViewById(R.id.barFilled);
                    View barEmpty = statView.findViewById(R.id.barEmpty);

                    if (tvTitle != null) {
                        tvTitle.setText(stats.name);
                    }

                    double rate = stats.getPresenceRate();
                    if (tvPercent != null) {
                        tvPercent.setText(String.format(Locale.getDefault(), "%.1f%%", rate));
                    }

                    // Définir la couleur de la barre en fonction du taux
                    int barColor;
                    if (rate >= 80) {
                        barColor = ContextCompat.getColor(this, R.color.green);
                    } else if (rate >= 60) {
                        barColor = ContextCompat.getColor(this, R.color.orange);
                    } else {
                        barColor = ContextCompat.getColor(this, R.color.red);
                    }

                    if (barFilled != null) {
                        barFilled.setBackgroundColor(barColor);

                        // Calculer la largeur de la barre
                        statView.post(() -> {
                            int totalWidth = statView.getWidth();
                            if (totalWidth > 0) {
                                int filledWidth = (int) (totalWidth * (rate / 100.0));

                                ViewGroup.LayoutParams filledParams = barFilled.getLayoutParams();
                                filledParams.width = filledWidth;
                                barFilled.setLayoutParams(filledParams);

                                ViewGroup.LayoutParams emptyParams = barEmpty.getLayoutParams();
                                emptyParams.width = totalWidth - filledWidth;
                                barEmpty.setLayoutParams(emptyParams);
                            }
                        });
                    }
                }

                index++;
            }

            // Si moins de départements que de vues, cacher les vues inutilisées
            for (int i = index; i < statIds.length; i++) {
                int viewId = getResources().getIdentifier(statIds[i], "id", getPackageName());
                LinearLayout statView = findViewById(viewId);
                if (statView != null) {
                    statView.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur updateDepartmentStatsUI: " + e.getMessage());
        }
    }

    private void setupActivitesListener() {
        Log.d(TAG, "setupActivitesListener: Configuration écouteur activités récentes");

        // Écouter les congés récents (limité aux 5 derniers)
        activitesCongesListener = db.collection("conges")
                .whereEqualTo("statut", "En attente")
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur activités congés: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Activités congés trouvées: " + value.size());
                        processActivitesConges(value.getDocuments());
                    } else {
                        Log.d(TAG, "Aucune activité congé trouvée");
                        updateAucuneActiviteVisibility();
                    }
                });

        // Écouter les attestations récentes (limité aux 5 derniers)
        activitesAttestationsListener = db.collection("Attestations")
                .whereEqualTo("statut", "en_attente")
                .orderBy("dateDemande", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur activités attestations: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Activités attestations trouvées: " + value.size());
                        processActivitesAttestations(value.getDocuments());
                    } else {
                        Log.d(TAG, "Aucune activité attestation trouvée");
                        updateAucuneActiviteVisibility();
                    }
                });
    }

    private void processActivitesConges(List<DocumentSnapshot> documents) {
        runOnUiThread(() -> {
            for (DocumentSnapshot doc : documents) {
                try {
                    String userName = doc.getString("userName");
                    String typeConge = doc.getString("typeConge");
                    Date dateDemande = doc.getDate("dateDemande");

                    if (userName != null && typeConge != null) {
                        String message = userName + " a demandé un " + typeConge.toLowerCase();
                        String tempsEcoule = calculerTempsEcoule(dateDemande);
                        ajouterOuMettreAJourActivite(message, tempsEcoule, R.drawable.orangecalendar);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur traitement activité congé: " + e.getMessage());
                }
            }
            updateAucuneActiviteVisibility();
        });
    }

    private void processActivitesAttestations(List<DocumentSnapshot> documents) {
        runOnUiThread(() -> {
            for (DocumentSnapshot doc : documents) {
                try {
                    String employeeNom = doc.getString("employeeNom");
                    String typeAttestation = doc.getString("typeAttestation");
                    Date dateDemande = doc.getDate("dateDemande");

                    if (employeeNom != null && typeAttestation != null) {
                        String message = employeeNom + " demande une " + typeAttestation.toLowerCase();
                        String tempsEcoule = calculerTempsEcoule(dateDemande);
                        ajouterOuMettreAJourActivite(message, tempsEcoule, R.drawable.docpurple);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erreur traitement activité attestation: " + e.getMessage());
                }
            }
            updateAucuneActiviteVisibility();
        });
    }

    private String calculerTempsEcoule(Date dateDemande) {
        if (dateDemande == null) {
            return "Récemment";
        }

        long diff = new Date().getTime() - dateDemande.getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "À l'instant";
        } else if (minutes < 60) {
            return "Il y'a " + minutes + " min";
        } else if (hours < 24) {
            return "Il y'a " + hours + "h";
        } else if (days == 1) {
            return "Hier";
        } else {
            return "Il y'a " + days + " jours";
        }
    }

    private void ajouterOuMettreAJourActivite(String message, String tempsEcoule, int iconRes) {
        try {
            // Vérifier si cette activité existe déjà
            View activiteExistante = trouverActiviteParMessage(message);

            if (activiteExistante != null) {
                // Mettre à jour l'activité existante
                TextView time = activiteExistante.findViewById(R.id.activityTime);
                if (time != null) {
                    time.setText(tempsEcoule);
                }
                Log.d(TAG, "Activité mise à jour: " + message);
            } else {
                // Ajouter une nouvelle activité
                View activiteView = LayoutInflater.from(this).inflate(R.layout.item_activity_recente, activitesRecentesContainer, false);

                // Configurer les éléments de l'activité
                ImageView icon = activiteView.findViewById(R.id.activityIcon);
                TextView title = activiteView.findViewById(R.id.activityTitle);
                TextView time = activiteView.findViewById(R.id.activityTime);

                if (icon != null) icon.setImageResource(iconRes);
                if (title != null) title.setText(message);
                if (time != null) time.setText(tempsEcoule);

                // Ajouter au début du conteneur (les plus récents en premier)
                activitesRecentesContainer.addView(activiteView, 0);

                // Limiter à 5 activités maximum
                if (activitesRecentesContainer.getChildCount() > 5) {
                    activitesRecentesContainer.removeViewAt(activitesRecentesContainer.getChildCount() - 1);
                }

                Log.d(TAG, "Nouvelle activité ajoutée: " + message);
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur ajout/mise à jour activité: " + e.getMessage());
        }
    }

    private View trouverActiviteParMessage(String message) {
        for (int i = 0; i < activitesRecentesContainer.getChildCount(); i++) {
            View child = activitesRecentesContainer.getChildAt(i);
            TextView title = child.findViewById(R.id.activityTitle);
            if (title != null && title.getText().toString().equals(message)) {
                return child;
            }
        }
        return null;
    }

    private void updateAucuneActiviteVisibility() {
        runOnUiThread(() -> {
            if (aucuneActiviteText != null && activitesRecentesContainer != null) {
                if (activitesRecentesContainer.getChildCount() > 0) {
                    // Il y a des activités, cacher le message
                    aucuneActiviteText.setVisibility(View.GONE);
                    Log.d(TAG, "Message 'Aucune activité' caché - " + activitesRecentesContainer.getChildCount() + " activités affichées");
                } else {
                    // Aucune activité, afficher le message
                    aucuneActiviteText.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Message 'Aucune activité' affiché - 0 activité");
                }
            }
        });
    }

    private void loadUserData() {
        if (currentUser != null) {
            Log.d(TAG, "loadUserData: Chargement des données utilisateur pour " + currentUser.getUid());

            userListener = db.collection("Users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Erreur chargement user: " + error.getMessage());
                            runOnUiThread(() -> {
                                if (rhConnecte != null) {
                                    rhConnecte.setText("Responsable RH");
                                }
                            });
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            updateUserInfo(documentSnapshot);
                        } else {
                            Log.w(TAG, "Document utilisateur non trouvé");
                            runOnUiThread(() -> {
                                if (rhConnecte != null) {
                                    rhConnecte.setText("Responsable RH");
                                }
                            });
                        }
                    });
        } else {
            Log.w(TAG, "loadUserData: Aucun utilisateur connecté");
            redirectToLogin();
        }
    }

    private void updateUserInfo(DocumentSnapshot document) {
        try {
            String nom = document.getString("nom");
            String prenom = document.getString("prenom");

            Log.d(TAG, "updateUserInfo: Nom=" + nom + ", Prénom=" + prenom);

            // Formater le nom complet
            String nomComplet = formatUserName(prenom, nom);

            runOnUiThread(() -> {
                if (rhConnecte != null) {
                    rhConnecte.setText(nomComplet);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur mise à jour user info: " + e.getMessage());
            runOnUiThread(() -> {
                if (rhConnecte != null) {
                    rhConnecte.setText("Responsable RH");
                }
            });
        }
    }

    private String formatUserName(String prenom, String nom) {
        if (prenom == null && nom == null) {
            return "Responsable RH";
        }
        if (prenom == null) {
            return nom != null ? nom : "Responsable RH";
        }
        if (nom == null) {
            return prenom;
        }

        // Capitaliser la première lettre de chaque mot
        String prenomFormate = capitalizeFirstLetter(prenom);
        String nomFormate = capitalizeFirstLetter(nom);

        return prenomFormate + " " + nomFormate;
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
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

    private void setupCongesListener() {
        Log.d(TAG, "setupCongesListener: Configuration écouteur congés");

        congesListener = db.collection("conges")
                .whereEqualTo("statut", "En attente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur conges: " + error.getMessage());
                        return;
                    }

                    int count = value != null ? value.size() : 0;
                    Log.d(TAG, "Congés en attente: " + count);
                    updateCongeStats(count);
                });
    }

    private void setupAttestationsListener() {
        Log.d(TAG, "setupAttestationsListener: Configuration écouteur attestations");

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("statut", "en_attente")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur attestations: " + error.getMessage());
                        return;
                    }

                    int count = value != null ? value.size() : 0;
                    Log.d(TAG, "Attestations en attente: " + count);
                    updateAttestationStats(count);
                });
    }

    private void setupReunionsListener() {
        Log.d(TAG, "setupReunionsListener: Configuration écouteur réunions");

        reunionsListener = db.collection("Reunions")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur réunions: " + error.getMessage());
                        return;
                    }

                    int reunionsCount = value != null ? value.size() : 0;
                    Log.d(TAG, "Réunions totales: " + reunionsCount);
                    updateReunionNotification(reunionsCount);
                });
    }

    private void setupEmployesListener() {
        Log.d(TAG, "setupEmployesListener: Configuration écouteur employés");

        employesListener = db.collection("employees")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur employés: " + error.getMessage());
                        return;
                    }

                    int count = value != null ? value.size() : 0;
                    Log.d(TAG, "Total employés: " + count);
                    updateEmployesStats(count);
                });
    }

    private void updateCongeStats(int count) {
        runOnUiThread(() -> {
            if (congeEnAttente != null) {
                congeEnAttente.setText(String.valueOf(count));
            }
            updateNotificationBadge(notifConge, count);
        });
    }

    private void updateAttestationStats(int count) {
        runOnUiThread(() -> {
            if (attestationEnAttente != null) {
                attestationEnAttente.setText(String.valueOf(count));
            }
            updateNotificationBadge(notifAttestation, count);
        });
    }

    private void updateEmployesStats(int count) {
        runOnUiThread(() -> {
            if (totalEmploye != null) {
                totalEmploye.setText(String.valueOf(count));
            }
        });
    }

    private void updateReunionNotification(int count) {
        runOnUiThread(() -> {
            // Afficher une notification si au moins une réunion existe
            updateNotificationBadge(notifReunion, count > 0 ? 1 : 0);
        });
    }

    private void updateNotificationBadge(TextView badgeView, int count) {
        if (badgeView != null) {
            if (count > 0) {
                badgeView.setText(String.valueOf(count));
                badgeView.setVisibility(View.VISIBLE);
            } else {
                badgeView.setVisibility(View.GONE);
            }
        }
    }

    private void gererNavigation() {
        Log.d(TAG, "gererNavigation: Configuration de la navigation");

        // Navigation footer
        setNavigationListener(R.id.reunions_interface, reunionActivity.class);
        setNavigationListener(R.id.actionReunions, reunionActivity.class);
        setNavigationListener(R.id.conges_interface, CongesActivity.class);
        setNavigationListener(R.id.employes_interface, EmployeActivity.class);
        setNavigationListener(R.id.profile_interface, ProfileActivity.class);
        setNavigationListener(R.id.attestation, AttestationsActivity.class);
        setNavigationListener(R.id.actionAttestation, AttestationsActivity.class);
        setNavigationListener(R.id.employes, EmployeActivity.class);
        setNavigationListener(R.id.presence, PresenceRhActivity.class);
        setNavigationListener(R.id.actionPresence, PresenceRhActivity.class);
        setNavigationListener(R.id.conges, CongesActivity.class);
        setNavigationListener(R.id.ActionConge, CongesActivity.class);


    }

    private void setNavigationListener(int viewId, Class<?> destination) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                Log.d(TAG, "Navigation vers: " + destination.getSimpleName());
                startActivity(new Intent(this, destination));
            });
        } else {
            Log.w(TAG, "View non trouvée pour l'ID: " + viewId);
        }
    }

    private void cleanupListeners() {
        Log.d(TAG, "cleanupListeners: Nettoyage des écouteurs");

        // Nettoyer tous les écouteurs de manière sécurisée
        try {
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
            if (employesListener != null) {
                employesListener.remove();
                employesListener = null;
            }
            if (userListener != null) {
                userListener.remove();
                userListener = null;
            }
            if (activitesCongesListener != null) {
                activitesCongesListener.remove();
                activitesCongesListener = null;
            }
            if (activitesAttestationsListener != null) {
                activitesAttestationsListener.remove();
                activitesAttestationsListener = null;
            }
            if (presenceStatsListener != null) {
                presenceStatsListener.remove();
                presenceStatsListener = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur nettoyage listeners: " + e.getMessage());
        }
    }
}