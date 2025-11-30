package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    // TextViews pour les statistiques
    private TextView congeEnAttente, totalPresents, totalEmploye, attestationEnAttente;
    private TextView notifPresence, notifConge, notifAttestation, notifReunion;
    private TextView rhConnecte;
    private LinearLayout activitesRecentesContainer;

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

        // Initialiser les badges de notification
        notifPresence = findViewById(R.id.notifPresence);
        notifConge = findViewById(R.id.notifConge);
        notifAttestation = findViewById(R.id.notifAttestation);
        notifReunion = findViewById(R.id.notifReunion);

        // Initialiser le conteneur d'activités récentes
        activitesRecentesContainer = findViewById(R.id.activitesRecentesContainer);
        aucuneActiviteText = findViewById(R.id.aucuneActiviteText);
    }

    private void setDefaultValues() {
        runOnUiThread(() -> {
            // Valeurs par défaut pour éviter les null et donner un feedback immédiat
            if (congeEnAttente != null) congeEnAttente.setText("0");
            if (totalEmploye != null) totalEmploye.setText("0");
            if (totalPresents != null) totalPresents.setText("0");
            if (attestationEnAttente != null) attestationEnAttente.setText("0");
            if (rhConnecte != null) rhConnecte.setText("Chargement...");

            // Cacher tous les badges de notification au démarrage
            if (notifPresence != null) notifPresence.setVisibility(View.GONE);
            if (notifConge != null) notifConge.setVisibility(View.GONE);
            if (notifAttestation != null) notifAttestation.setVisibility(View.GONE);
            if (notifReunion != null) notifReunion.setVisibility(View.GONE);

            // Afficher le message "Aucune activité" par défaut
            if (aucuneActiviteText != null) {
                aucuneActiviteText.setVisibility(View.VISIBLE);
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
        });
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

    private void processActivitesConges(java.util.List<DocumentSnapshot> documents) {
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

    private void processActivitesAttestations(java.util.List<DocumentSnapshot> documents) {
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
        setNavigationListener(R.id.presence, PresenceActivity.class);
        setNavigationListener(R.id.actionPresence, PresenceActivity.class);
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
        } catch (Exception e) {
            Log.e(TAG, "Erreur nettoyage listeners: " + e.getMessage());
        }
    }

    /**
     * Gère la déconnexion de l'utilisateur.
     */
    private void logoutUser() {
        try {
            Log.d(TAG, "logoutUser: Déconnexion en cours");
            cleanupListeners();
            mAuth.signOut();
            Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(AcceuilRhActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Erreur déconnexion: " + e.getMessage());
        }
    }
}