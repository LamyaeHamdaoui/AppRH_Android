package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class PresenceActivity extends AppCompatActivity {

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

    // Vues des statistiques mensuelles (NOUVEAU)
    private TextView nbrePresences;
    private TextView nbreAbsences;
    private TextView nbreTaux;

    // Vues du Footer
    private ImageView iconAccueil;
    private TextView textAccueil;
    private ImageView iconPresence;
    private TextView textPresence;
    private ImageView iconConges;
    private TextView textConges;
    private ImageView iconReunions;
    private TextView textReunions, nbrePresencesTextView,  nbreAbsencesTextView, nbreTauxTextView;
    private ImageView iconProfil;
    private TextView textProfil;
    private RelativeLayout notificationsButton;

    // Formats de date
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("EEEE", new Locale("fr", "FR"));
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Firestore
    private FirebaseFirestore db;
    private CollectionReference historyCollection;

    // Adapter data
    private PresenceHistoryAdapter adapter;
    private final List<PresenceDay> presenceDays = new ArrayList<>();

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

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        // Initialisation Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            historyCollection = db.collection("Presence").document(userId).collection("history");
        } else {
            Log.w("PresenceActivity", "Utilisateur non authentifié.");
            finish();
            return;
        }

        // --- 1. Initialisation des Vues ---
        presenceActionContainer = findViewById(R.id.presence_action_container);
        btnMarquerPresence = findViewById(R.id.btn_marquer_presence);
        btnJustifierAbsence = findViewById(R.id.btn_justifier_absence);
        notificationsButton = findViewById(R.id.notificationsButton);
        recyclerViewHistorique = findViewById(R.id.recyclerViewHistorique);
        nbrePresencesTextView = findViewById(R.id.nbrePresences);
        nbreAbsencesTextView = findViewById(R.id.nbreAbsences);
        nbreTauxTextView = findViewById(R.id.nbreTaux);
        // NOUVEAU: Initialisation des vues des statistiques
        nbrePresences = findViewById(R.id.nbrePresences);
        nbreAbsences = findViewById(R.id.nbreAbsences);
        nbreTaux = findViewById(R.id.nbreTaux);

        iconAccueil = findViewById(R.id.accueil);
        textAccueil = findViewById(R.id.textView3);
        iconPresence = findViewById(R.id.employes);
        textPresence = findViewById(R.id.textemployee);
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

        // NOUVEAU: Charger et afficher les statistiques du mois actuel
        loadMonthlyStatistics();
    }

    private String loadUserGender() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        return prefs.getString("gender", "M");
    }

    private void saveUserGender(String gender) {
        SharedPreferences.Editor editor = getSharedPreferences("UserProfile", Context.MODE_PRIVATE).edit();
        editor.putString("gender", gender);
        editor.apply();
    }

    // ---------- MÉTHODES DE STATISTIQUES MENSUELLES (NOUVEAU) ----------

    /**
     * Calcule et affiche les statistiques (Présences, Absences, Taux) pour le mois actuel.
     * Utilise le champ 'timestamp' dans Firestore pour interroger la plage de dates.
     */
    private void loadMonthlyStatistics() {
        // Vérification de sécurité
        if (userId == null || historyCollection == null) {
            Log.e("STATS", "UserId ou collection non initialisés.");
            return;
        }

        // 1. Définir la plage de dates pour le mois actuel
        Calendar calStart = Calendar.getInstance();
        // Début du mois (1er jour à 00:00:00.000)
        calStart.set(Calendar.DAY_OF_MONTH, 1);
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);

        Calendar calEnd = Calendar.getInstance();
        // Fin du mois (dernier jour à 23:59:59.999)
        calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);

        // ⚠️ IMPORTANT : Assurez-vous que l'importation de Timestamp est correcte
        // com.google.firebase.Timestamp (ou ajustez la conversion si vous n'utilisez pas l'objet Timestamp)

        // Convertir les dates pour la requête Firestore
        com.google.firebase.Timestamp startOfMonth = new com.google.firebase.Timestamp(calStart.getTime());
        com.google.firebase.Timestamp endOfMonth = new com.google.firebase.Timestamp(calEnd.getTime());

        Log.d("STATS_CHECK", "Début de la recherche : " + calStart.getTime().toString());
        Log.d("STATS_CHECK", "Fin de la recherche : " + calEnd.getTime().toString());

        // 2. Requête Firestore sur le champ 'timestamp'
        historyCollection
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .whereLessThanOrEqualTo("timestamp", endOfMonth)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalPresenceDays = 0;
                        int totalAbsenceDays = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");

                            if (status != null) {
                                // On ne compte que les enregistrements qui représentent un jour entier
                                if (status.equals("present")) {
                                    totalPresenceDays++;
                                } else if (status.contains("absent") || status.equals("conge")) {
                                    // On inclut 'absent', 'absent_justifie' et 'conge'
                                    totalAbsenceDays++;
                                }
                            }
                        }

                        // 3. Affichage des résultats
                        updateStatisticsUI(totalPresenceDays, totalAbsenceDays);

                    } else {
                        Log.e("PresenceActivity", "Erreur lors du chargement des statistiques: " + task.getException());
                        updateStatisticsUI(0, 0); // Afficher 0 en cas d'erreur
                    }
                });
    }

    /**
     * Met à jour les TextView de statistiques avec les données calculées.
     */
    private void updateStatisticsUI(int presences, int absences) {
        int totalDays = presences + absences;
        double taux = 0.0;

        if (totalDays > 0) {
            // Formule pour le Taux de Présence : (Présences / Jours enregistrés) * 100
            taux = ((double) presences / totalDays) * 100;
        }

        // Mise à jour des TextViews avec les résultats
        if (nbrePresencesTextView != null) {
            nbrePresencesTextView.setText(String.valueOf(presences));
        }
        if (nbreAbsencesTextView != null) {
            nbreAbsencesTextView.setText(String.valueOf(absences));
        }
        if (nbreTauxTextView != null) {
            // Affiche le taux avec 2 décimales et le symbole %
            nbreTauxTextView.setText(String.format(Locale.getDefault(), "%.2f%%", taux));
        }
    }

    // ---------- FIN DES MÉTHODES DE STATISTIQUES MENSUELLES ----------

    // ---------- ENREGISTRER DANS FIRESTORE ----------
    private void savePresenceToFirebase(String status, String details, String justification) {
        if (userId == null || historyCollection == null) {
            Toast.makeText(this, "Erreur: ID utilisateur ou collection non définie.", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Ajout du champ 'timestamp' pour faciliter les requêtes de plage de dates (statistiques)
        Timestamp nowTimestamp = Timestamp.now();

        // Créer un Map si le modèle History ne gère pas le Timestamp, sinon utiliser toObject()
        // Puisque votre modèle History n'a pas de champ Timestamp, utilisons un Map pour l'ajouter lors de la sauvegarde:
        Map<String, Object> data = new HashMap<>();
        data.put("date", rawDate);
        data.put("status", status);
        data.put("details", details);
        data.put("time", time);
        data.put("justification", justification);
        data.put("timestamp", nowTimestamp); // C'est essentiel pour loadMonthlyStatistics

        historyCollection
                .document(rawDate)
                .set(data) // Utilisation du Map pour inclure Timestamp
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Présence enregistrée dans l'historique.", Toast.LENGTH_SHORT).show();
                    loadHistoryFromFirestoreForPreviousWeek();
                    loadMonthlyStatistics(); // Mettre à jour les statistiques après l'enregistrement
                })
                .addOnFailureListener(e -> {
                    Log.e("PresenceActivity", "Erreur de sauvegarde Firestore: " + e.getMessage());
                    Toast.makeText(this, "Erreur de sauvegarde : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ---------- CHARGER L'HISTORIQUE (Semaine précédente) ----------
    // ---------- CHARGER L'HISTORIQUE (5 jours ouvrables précédents) ----------
    private void loadHistoryFromFirestoreForPreviousWeek() {
        if (userId == null || historyCollection == null) return;

        // Vider et peupler l'historique local
        presenceDays.clear();
        adapter.notifyDataSetChanged();

        List<String> daysRawToFetch = new ArrayList<>();

        // Commence à partir d'aujourd'hui, puis recule jour par jour
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Commencer hier (pour ne pas inclure aujourd'hui)

        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int daysCount = 0;

        // Boucler en arrière pour trouver 5 jours ouvrés
        while (daysCount < 5) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            // Vérifier si c'est Lundi (2) à Vendredi (6)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {

                String rawDate = rawFormat.format(cal.getTime());
                daysRawToFetch.add(rawDate);
                daysCount++;

                // Démarrer immédiatement la requête Firestore pour le jour
                fetchHistoryForDay(rawDate);
            }

            // Reculer d'un jour pour la prochaine itération
            cal.add(Calendar.DAY_OF_YEAR, -1);

            // Sécurité pour éviter une boucle infinie (max 10 jours vérifiés)
            if (daysRawToFetch.size() > 10) break;
        }
    }

    /**
     * Nouvelle méthode pour encapsuler la requête Firestore pour un jour unique.
     */
    private void fetchHistoryForDay(String rawDate) {
        if (userId == null || historyCollection == null) return;

        historyCollection
                .document(rawDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Traitement de l'historique pour le jour
                    handleDayHistory(documentSnapshot, rawDate);
                })
                .addOnFailureListener(e -> {
                    Log.e("PresenceActivity", "Erreur lecture Firestore pour date " + rawDate + ": " + e.getMessage());
                    addAbsentEntry(rawDate, "Erreur de lecture Firestore");
                });
    }

    /**
     * Traite la DocumentSnapshot Firestore pour créer ou mettre à jour une entrée PresenceDay.
     */
    private void handleDayHistory(DocumentSnapshot doc, String rawDate) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(rawDate);
            String displayDay = capitalize(displayDayFormat.format(d));
            String displayDate = displayDateFormat.format(d);

            PresenceDay pd;

            if (doc.exists()) {
                History history = doc.toObject(History.class);

                if (history != null) {
                    pd = new PresenceDay(
                            displayDay,
                            displayDate,
                            rawDate,
                            history.getStatus() != null ? history.getStatus() : "absent",
                            history.getDetails() != null ? history.getDetails() : (history.getStatus().equals("conge") ? "Jour de congé" : "Non spécifié"),
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
            Log.e("PresenceActivity", "Erreur mapping doc ou parse date", e);
            addAbsentEntry(rawDate, "Erreur de traitement");
        }
    }

    /**
     * Ajoute une entrée "Absent" à l'historique local.
     */
    private void addAbsentEntry(String rawDate, String details) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(rawDate);
            String displayDay = capitalize(displayDayFormat.format(d));
            String displayDate = displayDateFormat.format(d);

            PresenceDay pd = new PresenceDay(displayDay, displayDate, rawDate, "absent", details, null);
            updatePresenceDaysList(pd);
        } catch (ParseException e) {
            // Ne devrait pas arriver
        }
    }

    /**
     * Ajoute ou remplace l'entrée PresenceDay dans la liste, puis trie et notifie.
     */
    private void updatePresenceDaysList(PresenceDay pd) {
        boolean replaced = false;
        for (int i = 0; i < presenceDays.size(); i++) {
            if (presenceDays.get(i).rawDate.equals(pd.rawDate)) {
                presenceDays.set(i, pd);
                replaced = true;
                break;
            }
        }
        if (!replaced) presenceDays.add(pd);

        // FIX DU TRI : Tri par rawDate desc (Plus récent -> Plus ancien)
        // b.rawDate est comparé à a.rawDate pour inverser l'ordre
        presenceDays.sort((a, b) -> b.rawDate.compareTo(a.rawDate));

        adapter.notifyDataSetChanged();
    }

    // ---------- Utilities ----------
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    // ---------- Adapter (CORRIGÉ) ----------
    private class PresenceHistoryAdapter extends RecyclerView.Adapter<PresenceHistoryAdapter.ViewHolder> {

        private final List<PresenceDay> dataSet;
        private final String gender;

        public PresenceHistoryAdapter(List<PresenceDay> dataSet, String gender) {
            this.dataSet = dataSet;
            this.gender = gender;
        }

        @NonNull
        @Override
        public PresenceHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card_presence, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PresenceHistoryAdapter.ViewHolder holder, int position) {
            PresenceDay day = dataSet.get(position);

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

    /**
     * Traduit la clé de statut (stockée) en libellé lisible, accordé au genre.
     */
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

    // Reste des méthodes de gestion d'état, de navigation et de notification.

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
                    Log.i("PresenceActivity", "Restoration de l'état de présence marqué.");

                    SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String displayTime = displaySdf.format(arrivalDateTime);
                    restorePresenceCard(displayTime);
                } else {
                    Log.i("PresenceActivity", "Minuit est passé, réinitialisation de l'état de présence.");
                    clearPresenceState();
                }
            } catch (ParseException e) {
                Log.e("PresenceActivity", "Erreur lors de l'analyse de l'heure persistante.", e);
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
        LayoutInflater inflater = LayoutInflater.from(this);
        View presenceCardView = inflater.inflate(R.layout.item_card_marquer_present, presenceActionContainer, false);

        TextView tvArrivee = presenceCardView.findViewById(R.id.tv_heure_arrivee);
        if (tvArrivee != null) {
            tvArrivee.setText("Arrivée : " + displayTime + " (Enregistrée)");
        }

        presenceActionContainer.removeAllViews();
        presenceActionContainer.addView(presenceCardView);
    }

    private boolean checkIfAlreadyMarkedToday() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedTime = prefs.getString(KEY_ARRIVAL_TIME, null);

        if (savedTime == null) return false;

        try {
            Date arrivalDateTime = dateTimeFormat.parse(savedTime);
            return isSameDay(arrivalDateTime, new Date());
        } catch (ParseException e) {
            Log.e("PresenceActivity", "Erreur de parsing dans la vérification de l'état.", e);
            return false;
        }
    }

    private void setupClickListeners() {
        btnMarquerPresence.setOnClickListener(v -> {
            if (!checkIfAlreadyMarkedToday()) {
                replacePresenceActionWithCard();
            } else {
                Toast.makeText(PresenceActivity.this, "Votre présence est déjà enregistrée pour aujourd'hui.", Toast.LENGTH_SHORT).show();
            }
        });

        btnJustifierAbsence.setOnClickListener(v -> showJustifyAbsenceFragment());

        iconAccueil.setOnClickListener(v -> navigateTo(AcceuilEmployeActivity.class));
        iconConges.setOnClickListener(v -> navigateTo(CongesEmployeActivity.class));
        iconReunions.setOnClickListener(v -> navigateTo(ReunionEmployeActivity.class));
        iconProfil.setOnClickListener(v -> navigateTo(ProfileEmployeActivity.class));
        notificationsButton.setOnClickListener(v -> navigateTo(NotificationsEmployesActivity.class));
    }

    private void replacePresenceActionWithCard() {
        Date now = new Date();

        String fullDateTime = dateTimeFormat.format(now);
        savePresenceState(fullDateTime);

        SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String displayTime = displaySdf.format(now);
        restorePresenceCard(displayTime);

        sendPresenceMarkedNotification(displayTime);
        // Utilisation de la méthode savePresenceToFirebase mise à jour
        savePresenceToFirebase("present", "8h00 - 17h00", null);

        Toast.makeText(this, "Présence marquée. Les RH ont été notifiés.", Toast.LENGTH_LONG).show();
    }

    private void showJustifyAbsenceFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        JustifyAbsenceFragment fragment = new JustifyAbsenceFragment();

        fragmentTransaction.replace(R.id.main, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(PresenceActivity.this, destinationClass);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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
                "Absence non justifiée détectée à l'ouverture de l'application (pas de présence ni de justification le jour-même).",
                "Absence Non Justifiée"
        );
        Toast.makeText(this, "Alerte RH: Absence non justifiée détectée.", Toast.LENGTH_LONG).show();
    }

    private void recordNotification(String type, String message, String toastTitle) {
        SharedPreferences prefs = getSharedPreferences(NOTIFICATION_PREF_NAME, Context.MODE_PRIVATE);
        String existingHistory = prefs.getString(NOTIFICATION_KEY, "[]");

        try {
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

            Log.d("RH_NOTIF", toastTitle + " enregistrée : " + newEntry.toString());

        } catch (Exception e) {
            Log.e("RH_NOTIF", "Erreur lors de l'enregistrement de la notification JSON", e);
        }
    }
}