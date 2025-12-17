package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rhapp.model.History;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PresenceActivity extends AppCompatActivity implements JustifyAbsenceFragment.JustificationListener {

    // --- Constantes de Persistance de l'État de Présence ---
    private static final String PREF_NAME = "PresencePrefs";
    private static final String KEY_ARRIVAL_TIME = "arrivalTime";

    private String userGender = "M";
    private String userId = null;

    // --- Constantes de Persistance pour le suivi de l'absence non justifiée ---
    private static final String KEY_LAST_ABSENCE_NOTIFICATION_DATE = "lastAbsenceNotificationDate";

    // Définitions des couleurs pour une utilisation dans l'Adapter
    private final int COLOR_GREEN = R.color.green;
    private final int COLOR_RED = R.color.red;
    private final int COLOR_BLUE = R.color.blue;
    private final int COLOR_GREY = R.color.grey;
    private final int COLOR_RED_LIGHT = R.color.red_light;

    // --- Constantes de Persistance des Notifications RH ---
    private static final String NOTIFICATION_PREF_NAME = "RhNotifications";
    private static final String NOTIFICATION_KEY = "justification_history";

    // Vues principales
    private LinearLayout presenceActionContainer;
    private Button btnMarquerPresence;
    private Button btnJustifierAbsence;
    private RecyclerView recyclerViewHistorique;

    // Vues des statistiques mensuelles
    private TextView nbrePresencesTextView;
    private TextView nbreAbsencesTextView;
    private TextView nbreTauxTextView;

    // Vues du Footer
    private ImageView iconAccueil;
    private TextView textAccueil;
    private ImageView iconPresence;
    private TextView textPresence;
    private ImageView iconConges;
    private TextView textConges;
    private ImageView iconReunions;
    private TextView textReunions;
    private ImageView iconProfil;
    private TextView textProfil;

    // Formats de date
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("EEEE", new Locale("fr", "FR"));
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    // Firestore
    private FirebaseFirestore db;
    private CollectionReference historyCollection;

    // Adapter data
    private PresenceHistoryAdapter adapter;
    private final List<PresenceDay> presenceDays = new ArrayList<>();

    // Executor pour les opérations en arrière-plan
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- Modèle interne pour l'affichage ---
    private static class PresenceDay {
        final String dayName;
        final String date;
        final String rawDate;
        final String status;
        final String details;
        final String arrivalTime;

        public PresenceDay(String dayName, String date, String rawDate, String status, String details, String arrivalTime) {
            this.dayName = dayName;
            this.date = date;
            this.rawDate = rawDate;
            this.status = status;
            this.details = details;
            this.arrivalTime = arrivalTime;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        // Initialisation Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getEmail();
            historyCollection = db.collection("PresenceHistory");
            Log.d("PresenceActivity", "Utilisateur authentifié: " + userId);
        } else {
            Log.w("PresenceActivity", "Utilisateur non authentifié.");
            Toast.makeText(this, "Veuillez vous reconnecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation des Vues
        presenceActionContainer = findViewById(R.id.presence_action_container);
        btnMarquerPresence = findViewById(R.id.btn_marquer_presence);
        btnJustifierAbsence = findViewById(R.id.btn_justifier_absence);
        recyclerViewHistorique = findViewById(R.id.recyclerViewHistorique);
        nbrePresencesTextView = findViewById(R.id.nbrePresences);
        nbreAbsencesTextView = findViewById(R.id.nbreAbsences);
        nbreTauxTextView = findViewById(R.id.nbreTaux);

        iconAccueil = findViewById(R.id.accueil);
        textAccueil = findViewById(R.id.textView3);
        iconPresence = findViewById(R.id.iconPresence);
        textPresence = findViewById(R.id.textPresence);
        iconConges = findViewById(R.id.conge);
        textConges = findViewById(R.id.textconge);
        iconReunions = findViewById(R.id.reunions);
        textReunions = findViewById(R.id.textreunion);
        iconProfil = findViewById(R.id.profil);
        textProfil = findViewById(R.id.textprofil);

        // Chargement du genre utilisateur
        userGender = loadUserGender();

        // Setup RecyclerView + Adapter
        adapter = new PresenceHistoryAdapter(presenceDays, userGender);
        recyclerViewHistorique.setAdapter(adapter);
        recyclerViewHistorique.setLayoutManager(new LinearLayoutManager(this));

        checkAndRestorePresenceState();
        setupClickListeners();
        setupFooterHighlight();

        // Charger l'historique réel depuis Firestore
        loadHistoryFromFirestoreForPreviousWeek();

        // Charger et afficher les statistiques du mois actuel
        loadMonthlyStatistics();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer l'executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private String loadUserGender() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        return prefs.getString("gender", "M");
    }

    // ---------- MÉTHODES DE STATISTIQUES MENSUELLES CORRIGÉES ----------
    private void loadMonthlyStatistics() {
        if (userId == null || historyCollection == null) {
            Log.e("STATS", "UserId ou collection non initialisés.");
            updateStatisticsUI(0, 0);
            return;
        }

        executorService.execute(() -> {
            try {
                // Obtenir le mois et l'année courants
                Calendar cal = Calendar.getInstance();
                int currentYear = cal.get(Calendar.YEAR);
                int currentMonth = cal.get(Calendar.MONTH); // Janvier = 0, Décembre = 11

                // Construire le préfixe du mois (ex: "2025-12-")
                String monthPrefix = String.format(Locale.getDefault(), "%04d-%02d-", currentYear, currentMonth + 1);

                Log.d("STATS", "=== DÉBUT STATISTIQUES ===");
                Log.d("STATS", "UserId: " + userId);
                Log.d("STATS", "Période recherchée: " + monthPrefix);
                Log.d("STATS", "Mois: " + (currentMonth + 1) + "/" + currentYear);

                // Rechercher tous les documents de l'utilisateur
                historyCollection
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                int totalDocuments = task.getResult().size();
                                Log.d("STATS", "Nombre total de documents trouvés: " + totalDocuments);

                                if (totalDocuments == 0) {
                                    Log.w("STATS", "Aucun document trouvé pour cet utilisateur");
                                    updateStatisticsUI(0, 0);
                                    return;
                                }

                                int monthPresences = 0;
                                int monthAbsences = 0;
                                int totalProcessed = 0;

                                // Parcourir tous les documents
                                for (QueryDocumentSnapshot doc : task.getResult()) {
                                    try {
                                        totalProcessed++;

                                        // Extraire les champs
                                        String docDate = doc.getString("date");
                                        String docStatus = doc.getString("status");

                                        // Log détaillé pour les premiers documents
                                        if (totalProcessed <= 3) {
                                            Log.d("STATS_DETAIL", "Document #" + totalProcessed + " - ID: " + doc.getId());
                                            Log.d("STATS_DETAIL", "  Date: " + docDate);
                                            Log.d("STATS_DETAIL", "  Status: " + docStatus);
                                        }

                                        // Vérifier si le document appartient au mois courant
                                        if (docDate != null && docDate.startsWith(monthPrefix)) {
                                            if (docStatus != null) {
                                                String statusLower = docStatus.toLowerCase(Locale.getDefault());

                                                if (statusLower.contains("present") ||
                                                        statusLower.contains("présent") ||
                                                        statusLower.equals("p")) {
                                                    monthPresences++;
                                                    Log.d("STATS", "  ✅ Présence comptée pour: " + docDate);
                                                } else if (statusLower.contains("absent") ||
                                                        statusLower.contains("absent_justifie") ||
                                                        statusLower.contains("conge") ||
                                                        statusLower.contains("congé") ||
                                                        statusLower.equals("a")) {
                                                    monthAbsences++;
                                                    Log.d("STATS", "  ❌ Absence comptée pour: " + docDate);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("STATS", "Erreur traitement document #" + totalProcessed + ": " + e.getMessage());
                                    }
                                }

                                // Afficher les résultats
                                Log.d("STATS", "=== RÉSULTATS FINAUX ===");
                                Log.d("STATS", "Documents traités: " + totalProcessed);
                                Log.d("STATS", "Présences ce mois: " + monthPresences);
                                Log.d("STATS", "Absences ce mois: " + monthAbsences);
                                Log.d("STATS", "Total jours (mois): " + (monthPresences + monthAbsences));

                                updateStatisticsUI(monthPresences, monthAbsences);

                            } else {
                                Log.e("STATS", "ERREUR Firestore: ", task.getException());
                                updateStatisticsUI(0, 0);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("STATS", "Échec requête Firestore: ", e);
                            updateStatisticsUI(0, 0);
                        });
            } catch (Exception e) {
                Log.e("STATS", "Exception dans loadMonthlyStatistics: ", e);
                mainHandler.post(() -> updateStatisticsUI(0, 0));
            }
        });
    }

    @Override
    public void onAbsenceJustified(String justificationDetails) {
        if (userId == null) {
            Toast.makeText(this, "Erreur: Utilisateur non identifié.", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                Date now = new Date();
                String todayDate = dateFormat.format(now);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String displayTime = timeFormat.format(now);

                savePresenceToFirebase(
                        userId,
                        "absent_justifie",
                        justificationDetails,
                        displayTime
                );

                String message = "Demande de justification d'absence reçue : " +
                        justificationDetails.substring(0, Math.min(justificationDetails.length(), 40)) + "...";
                saveNotificationForRh("absence_justifie", message, userId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Justification enregistrée et RH notifiés.", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans onAbsenceJustified: ", e);
            }
        });
    }

    private void updateStatisticsUI(int presences, int absences) {
        int totalDays = presences + absences;
        double taux;

        if (totalDays > 0) {
            taux = ((double) presences / totalDays) * 100;
        } else {
            taux = 0.0;
        }

        runOnUiThread(() -> {
            if (nbrePresencesTextView != null) {
                nbrePresencesTextView.setText(String.valueOf(presences));
            }
            if (nbreAbsencesTextView != null) {
                nbreAbsencesTextView.setText(String.valueOf(absences));
            }
            if (nbreTauxTextView != null) {
                nbreTauxTextView.setText(String.format(Locale.getDefault(), "%.1f%%", taux));
            }

            Log.d("STATS_UI", "UI mise à jour - Présences: " + presences + ", Absences: " + absences + ", Taux: " + String.format(Locale.getDefault(), "%.1f%%", taux));
        });
    }

    // ---------- ENREGISTRER DANS FIRESTORE ----------
    public void savePresenceToFirebase(String userId, String status, String details, String arrivalTime) {
        if (userId == null || historyCollection == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur: ID utilisateur ou collection non définie.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        executorService.execute(() -> {
            try {
                // Récupérer la date d'aujourd'hui
                String todayDate = dateFormat.format(new Date());
                Timestamp nowTimestamp = Timestamp.now();

                // CORRECTION: Créer un ID document unique qui combine userId et date
                String documentId = userId + "_" + todayDate;

                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                data.put("status", status);
                data.put("timestamp", nowTimestamp);
                data.put("details", details != null ? details : "Présence marquée");
                data.put("time", arrivalTime != null ? arrivalTime : "Non spécifié");
                data.put("date", todayDate);
                data.put("documentId", documentId); // Pour faciliter les requêtes

                Log.d("SAVE_FIRESTORE", "Sauvegarde pour: " + documentId + " - Status: " + status);

                // CORRECTION: Utiliser le documentId personnalisé
                historyCollection
                        .document(documentId)
                        .set(data)
                        .addOnSuccessListener(documentReference -> {
                            Log.d("PresenceActivity", "Présence enregistrée avec ID: " + documentId);
                            runOnUiThread(() -> {
                                Toast.makeText(PresenceActivity.this, "Présence enregistrée avec succès.", Toast.LENGTH_SHORT).show();
                            });

                            // Rafraîchir l'historique et les statistiques
                            runOnUiThread(() -> {
                                loadHistoryFromFirestoreForPreviousWeek();
                                loadMonthlyStatistics(); // Recharger les stats après enregistrement
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PresenceActivity", "Erreur de sauvegarde Firestore: " + e.getMessage(), e);
                            runOnUiThread(() -> {
                                Toast.makeText(PresenceActivity.this, "Erreur de sauvegarde : " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        });
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans savePresenceToFirebase: ", e);
            }
        });
    }

    private void saveNotificationForRh(String type, String message, String employeeId) {
        if (db == null) return;

        executorService.execute(() -> {
            try {
                CollectionReference rhAlerts = db.collection("RhNotificationsFeed");

                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", type);
                notificationData.put("message", message);
                notificationData.put("emitterId", employeeId);
                notificationData.put("timestamp", Timestamp.now());
                notificationData.put("isRead", false);

                rhAlerts.add(notificationData)
                        .addOnSuccessListener(documentReference -> {
                            Log.i("RH_NOTIF", "Alerte RH enregistrée: " + type);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RH_NOTIF", "Erreur enregistrement alerte RH: " + e.getMessage());
                        });
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans saveNotificationForRh: ", e);
            }
        });
    }

    // ---------- CHARGER L'HISTORIQUE ----------
    private void loadHistoryFromFirestoreForPreviousWeek() {
        if (userId == null || historyCollection == null) {
            Log.e("LOAD_HISTORY", "UserId ou collection null");
            return;
        }

        executorService.execute(() -> {
            try {
                Log.d("LOAD_HISTORY", "Chargement historique pour: " + userId);
                runOnUiThread(() -> {
                    presenceDays.clear();
                });

                List<String> daysRawToFetch = new ArrayList<>();
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -1); // Commencer hier

                SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                int daysCount = 0;

                // Récupérer les 5 derniers jours ouvrables
                while (daysCount < 5) {
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                    if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                        String rawDate = rawFormat.format(cal.getTime());
                        daysRawToFetch.add(rawDate);
                        daysCount++;
                        fetchHistoryForDay(rawDate);
                    }

                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    if (daysRawToFetch.size() > 10) break;
                }

                Log.d("LOAD_HISTORY", "Jours à charger: " + daysRawToFetch.size());
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans loadHistoryFromFirestoreForPreviousWeek: ", e);
            }
        });
    }

    private void fetchHistoryForDay(String rawDate) {
        if (userId == null || historyCollection == null) return;

        executorService.execute(() -> {
            try {
                Log.d("FETCH_DAY", "Recherche pour: " + rawDate + " - User: " + userId);

                // CORRECTION: Rechercher par documentId personnalisé
                String documentId = userId + "_" + rawDate;

                historyCollection
                        .document(documentId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Log.d("FETCH_DAY", "Document snapshot existe: " + documentSnapshot.exists());
                            handleDayHistory(documentSnapshot, rawDate);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FETCH_DAY", "Erreur pour " + rawDate + ": " + e.getMessage());
                            // Si erreur, essayer l'ancienne méthode de recherche
                            fallbackFetchForDay(rawDate);
                        });
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans fetchHistoryForDay: ", e);
            }
        });
    }

    private void fallbackFetchForDay(String rawDate) {
        // Méthode de secours: recherche par requête
        executorService.execute(() -> {
            try {
                historyCollection
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("date", rawDate)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                                handleDayHistory(documentSnapshot, rawDate);
                            } else {
                                addAbsentEntry(rawDate, "Pas d'enregistrement");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FETCH_DAY_FALLBACK", "Erreur fallback: " + e.getMessage());
                            addAbsentEntry(rawDate, "Erreur de lecture");
                        });
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans fallbackFetchForDay: ", e);
            }
        });
    }

    private void handleDayHistory(DocumentSnapshot doc, String rawDate) {
        try {
            Date d = dateFormat.parse(rawDate);
            String displayDay = capitalize(displayDayFormat.format(d));
            String displayDate = displayDateFormat.format(d);

            PresenceDay pd;

            if (doc.exists()) {
                History history = doc.toObject(History.class);

                if (history != null) {
                    Log.d("HANDLE_HISTORY", "History trouvé - Status: " + history.getStatus());
                    pd = new PresenceDay(
                            displayDay,
                            displayDate,
                            rawDate,
                            history.getStatus() != null ? history.getStatus() : "absent",
                            history.getDetails() != null ? history.getDetails() :
                                    (history.getStatus() != null && history.getStatus().equals("conge") ?
                                            "Jour de congé" : "Non spécifié"),
                            history.getTime()
                    );
                } else {
                    pd = new PresenceDay(displayDay, displayDate, rawDate, "absent", "Données incomplètes", null);
                }
            } else {
                pd = new PresenceDay(displayDay, displayDate, rawDate, "absent", "Absence non enregistrée", null);
            }

            updatePresenceDaysList(pd);

        } catch (Exception e) {
            Log.e("PresenceActivity", "Erreur traitement historique", e);
            addAbsentEntry(rawDate, "Erreur de traitement");
        }
    }

    private void addAbsentEntry(String rawDate, String details) {
        try {
            Date d = dateFormat.parse(rawDate);
            String displayDay = capitalize(displayDayFormat.format(d));
            String displayDate = displayDateFormat.format(d);

            PresenceDay pd = new PresenceDay(displayDay, displayDate, rawDate, "absent", details, null);
            updatePresenceDaysList(pd);
        } catch (ParseException e) {
            Log.e("PresenceActivity", "Erreur parsing date", e);
        }
    }

    private void updatePresenceDaysList(PresenceDay pd) {
        Log.d("UPDATE_LIST", "Mise à jour: " + pd.rawDate + " - Status: " + pd.status);

        runOnUiThread(() -> {
            boolean replaced = false;
            for (int i = 0; i < presenceDays.size(); i++) {
                if (presenceDays.get(i).rawDate.equals(pd.rawDate)) {
                    presenceDays.set(i, pd);
                    replaced = true;
                    Log.d("UPDATE_LIST", "Entrée remplacée");
                    break;
                }
            }
            if (!replaced) {
                presenceDays.add(pd);
                Log.d("UPDATE_LIST", "Nouvelle entrée ajoutée");
            }

            presenceDays.sort((a, b) -> b.rawDate.compareTo(a.rawDate));
            adapter.notifyDataSetChanged();
            Log.d("UPDATE_LIST", "Taille finale: " + presenceDays.size());
        });
    }

    // ---------- Utilities ----------
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    // ---------- Adapter ----------
    private class PresenceHistoryAdapter extends RecyclerView.Adapter<PresenceHistoryAdapter.ViewHolder> {
        private final List<PresenceDay> dataSet;
        private final String gender;

        public PresenceHistoryAdapter(List<PresenceDay> dataSet, String gender) {
            this.dataSet = dataSet;
            this.gender = gender;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_presence, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PresenceDay day = dataSet.get(position);

            Log.d("ADAPTER", "Affichage position " + position + ": " + day.rawDate + " - " + day.status);

            String statutBadgeText = getGenderSpecificStatusForDisplay(day.status, gender);
            holder.tvJourDate.setText(day.dayName + " " + day.date);

            if (day.status.equals("present") && day.arrivalTime != null) {
                holder.tvDetailsPresence.setText("Arrivée : " + day.arrivalTime);
            } else {
                holder.tvDetailsPresence.setText(day.details);
            }

            holder.tvStatutPresence.setText(statutBadgeText);

            int textColor;
            int iconRes;
            int backgroundRes;

            if (day.status.equals("present")) {
                textColor = ContextCompat.getColor(PresenceActivity.this, COLOR_GREEN);
                iconRes = R.drawable.approuve;
                backgroundRes = R.drawable.approuve_border;
            } else if (day.status.equals("absent_justifie")) {
                textColor = ContextCompat.getColor(PresenceActivity.this, COLOR_RED);
                iconRes = R.drawable.refuse;
                backgroundRes = R.drawable.border_redlight;
            } else if (day.status.equals("absent")) {
                textColor = ContextCompat.getColor(PresenceActivity.this, COLOR_RED);
                iconRes = R.drawable.refuse;
                backgroundRes = R.drawable.border_redlight;
            } else if (day.status.equals("conge")) {
                textColor = ContextCompat.getColor(PresenceActivity.this, COLOR_BLUE);
                iconRes = R.drawable.conge_blue;
                backgroundRes = R.drawable.border_blue_bg;
            } else {
                textColor = ContextCompat.getColor(PresenceActivity.this, COLOR_GREY);
                iconRes = R.drawable.time_bleu;
                backgroundRes = R.drawable.simple_border;
            }

            holder.tvStatutPresence.setTextColor(textColor);
            holder.statusIcon.setImageResource(iconRes);
            holder.tvStatutPresence.setBackgroundResource(backgroundRes);
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView statusIcon;
            final TextView tvJourDate;
            final TextView tvDetailsPresence;
            final TextView tvStatutPresence;

            public ViewHolder(View view) {
                super(view);
                statusIcon = view.findViewById(R.id.statusIcon);
                tvJourDate = view.findViewById(R.id.tv_jour_date);
                tvDetailsPresence = view.findViewById(R.id.tv_details_presence);
                tvStatutPresence = view.findViewById(R.id.tv_statut_presence);
            }
        }
    }

    private String getGenderSpecificStatusForDisplay(String statusKey, String gender) {
        if ("present".equals(statusKey)) {
            return "F".equals(gender) ? "Présente" : "Présent";
        } else if ("absent".equals(statusKey) || "absent_justifie".equals(statusKey)) {
            return "F".equals(gender) ? "Absente" : "Absent";
        } else if ("conge".equals(statusKey)) {
            return "Congé";
        }
        return capitalize(statusKey);
    }

    // ---------- Gestion de l'état de présence ----------
    private void checkAndRestorePresenceState() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedTime = prefs.getString(KEY_ARRIVAL_TIME, null);

        Date now = new Date();
        boolean isPresenceMarked = false;

        if (savedTime != null) {
            try {
                Date arrivalDateTime = dateTimeFormat.parse(savedTime);

                if (isSameDay(arrivalDateTime, now)) {
                    isPresenceMarked = true;
                    Log.i("PresenceActivity", "Restauration de l'état de présence marqué.");

                    SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String displayTime = displaySdf.format(arrivalDateTime);
                    restorePresenceCard(displayTime);
                } else {
                    Log.i("PresenceActivity", "Minuit est passé, réinitialisation.");
                    clearPresenceState();
                }
            } catch (ParseException e) {
                Log.e("PresenceActivity", "Erreur parsing heure persistante", e);
                clearPresenceState();
            }
        }

        if (!isPresenceMarked) {
            String lastNotifDateString = prefs.getString(KEY_LAST_ABSENCE_NOTIFICATION_DATE, "");
            String todayDateString = dateFormat.format(now);

            if (!todayDateString.equals(lastNotifDateString)) {
                sendUnjustifiedAbsenceNotification();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_LAST_ABSENCE_NOTIFICATION_DATE, todayDateString);
                editor.apply();
            }
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void savePresenceState(String dateTimeString) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_ARRIVAL_TIME, dateTimeString);
        editor.apply();
    }

    private void clearPresenceState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(KEY_ARRIVAL_TIME);
        editor.apply();
    }

    private void restorePresenceCard(String displayTime) {
        runOnUiThread(() -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View presenceCardView = inflater.inflate(R.layout.item_card_marquer_present, presenceActionContainer, false);

            TextView tvArrivee = presenceCardView.findViewById(R.id.tv_heure_arrivee);
            if (tvArrivee != null) {
                tvArrivee.setText("Arrivée : " + displayTime + " (Enregistrée)");
            }

            presenceActionContainer.removeAllViews();
            presenceActionContainer.addView(presenceCardView);
        });
    }

    private void setupClickListeners() {
        btnMarquerPresence.setOnClickListener(v -> replacePresenceActionWithCard());
        btnJustifierAbsence.setOnClickListener(v -> showJustifyAbsenceFragment());

        iconAccueil.setOnClickListener(v -> navigateTo(AcceuilEmployeActivity.class));
        iconReunions.setOnClickListener(v -> navigateTo(ReunionEmployeActivity.class));
        iconProfil.setOnClickListener(v -> navigateTo(ProfileEmployeActivity.class));
        iconConges.setOnClickListener(v -> navigateTo(CongesEmploye.class));
    }

    private void replacePresenceActionWithCard() {
        if (userId == null) {
            Toast.makeText(this, "Erreur: Utilisateur non identifié.", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            try {
                Date now = new Date();
                String fullDateTime = dateTimeFormat.format(now);
                savePresenceState(fullDateTime);

                SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String displayTime = displaySdf.format(now);

                runOnUiThread(() -> {
                    restorePresenceCard(displayTime);
                    Toast.makeText(this, "Présence marquée à " + displayTime, Toast.LENGTH_LONG).show();
                });

                // Sauvegarder dans Firebase
                savePresenceToFirebase(
                        userId,
                        "present",
                        "Présence marquée manuellement",
                        displayTime
                );

                sendPresenceMarkedNotification(displayTime);
            } catch (Exception e) {
                Log.e("PresenceActivity", "Erreur dans replacePresenceActionWithCard: ", e);
            }
        });
    }

    private void showJustifyAbsenceFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        JustifyAbsenceFragment fragment = new JustifyAbsenceFragment();
        fragment.show(fragmentManager, "JustifyAbsence");
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(PresenceActivity.this, destinationClass);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupFooterHighlight() {
        iconPresence.setColorFilter(ContextCompat.getColor(this, COLOR_BLUE));
        textPresence.setTextColor(ContextCompat.getColor(this, COLOR_BLUE));

        iconAccueil.setColorFilter(ContextCompat.getColor(this, COLOR_GREY));
        textAccueil.setTextColor(ContextCompat.getColor(this, COLOR_GREY));
        iconConges.setColorFilter(ContextCompat.getColor(this, COLOR_GREY));
        textConges.setTextColor(ContextCompat.getColor(this, COLOR_GREY));
        iconReunions.setColorFilter(ContextCompat.getColor(this, COLOR_GREY));
        textReunions.setTextColor(ContextCompat.getColor(this, COLOR_GREY));
        iconProfil.setColorFilter(ContextCompat.getColor(this, COLOR_GREY));
        textProfil.setTextColor(ContextCompat.getColor(this, COLOR_GREY));
    }

    private void sendPresenceMarkedNotification(String arrivalTime) {
        recordNotification(
                "presence_marked",
                "L'employé a marqué sa présence à " + arrivalTime + ".",
                "Présence Enregistrée"
        );
    }

    private void sendUnjustifiedAbsenceNotification() {
        recordNotification(
                "unjustified_absence",
                "Absence non justifiée détectée à l'ouverture de l'application.",
                "Absence Non Justifiée"
        );
        runOnUiThread(() -> {
            Toast.makeText(this, "Alerte RH: Absence non justifiée détectée.", Toast.LENGTH_LONG).show();
        });
    }

    private void recordNotification(String type, String message, String toastTitle) {
        executorService.execute(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences(NOTIFICATION_PREF_NAME, Context.MODE_PRIVATE);
                String existingHistory = prefs.getString(NOTIFICATION_KEY, "[]");

                JSONArray historyArray = new JSONArray(existingHistory);
                JSONObject newEntry = new JSONObject();
                newEntry.put("type", type);
                newEntry.put("message", message);

                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date now = new Date();

                newEntry.put("time", timeSdf.format(now));
                newEntry.put("date", dateSdf.format(now));

                JSONArray updatedArray = new JSONArray();
                updatedArray.put(newEntry);

                for (int i = 0; i < historyArray.length(); i++) {
                    updatedArray.put(historyArray.getJSONObject(i));
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(NOTIFICATION_KEY, updatedArray.toString());
                editor.apply();

                Log.d("RH_NOTIF", toastTitle + " enregistrée");
            } catch (Exception e) {
                Log.e("RH_NOTIF", "Erreur enregistrement notification JSON", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les données quand l'activité revient au premier plan
        executorService.execute(() -> {
            try {
                Thread.sleep(500); // Petit délai pour éviter les conflits
                runOnUiThread(() -> {
                    loadHistoryFromFirestoreForPreviousWeek();
                    loadMonthlyStatistics(); // Recharger aussi les stats
                });
            } catch (InterruptedException e) {
                Log.e("PresenceActivity", "Erreur délai onResume: ", e);
            }
        });
    }

    // Méthode de débogage pour vérifier les données Firestore
    private void debugFirestoreData() {
        if (userId == null || historyCollection == null) return;

        historyCollection
                .whereEqualTo("userId", userId)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("DEBUG", "=== DONNÉES FIRESTORE ===");
                        int count = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            count++;
                            Log.d("DEBUG", "Document #" + count);
                            Log.d("DEBUG", "  ID: " + doc.getId());
                            Log.d("DEBUG", "  Date: " + doc.get("date"));
                            Log.d("DEBUG", "  Status: " + doc.get("status"));
                            Log.d("DEBUG", "  UserId: " + doc.get("userId"));
                            Log.d("DEBUG", "---");
                        }
                        Log.d("DEBUG", "Total documents: " + count);
                    } else {
                        Log.e("DEBUG", "Erreur récupération données: ", task.getException());
                    }
                });
    }
}