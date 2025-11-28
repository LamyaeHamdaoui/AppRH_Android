package com.example.rhapp;

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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PresenceActivity extends AppCompatActivity {

    // --- Constantes de Persistance de l'État de Présence ---
    private static final String PREF_NAME = "PresencePrefs";
    private static final String KEY_ARRIVAL_TIME = "arrivalTime";

    private String userGender = "M";

    // --- NOUVELLE Constante de Persistance pour le suivi de l'absence non justifiée ---
    private static final String KEY_LAST_ABSENCE_NOTIFICATION_DATE = "lastAbsenceNotificationDate";

    // Définitions des couleurs pour une utilisation dans l'Adapter
    private final int COLOR_GREEN = R.color.green;
    private final int COLOR_RED = R.color.red;
    private final int COLOR_BLUE = R.color.blue;
    private final int COLOR_GREY = R.color.grey;

    // --- Constantes de Persistance des Notifications RH ---
    private static final String NOTIFICATION_PREF_NAME = "RhNotifications";
    private static final String NOTIFICATION_KEY = "justification_history";

    // Vues principales
    private LinearLayout presenceActionContainer;
    private Button btnMarquerPresence;
    private Button btnJustifierAbsence;
    private RecyclerView recyclerViewHistorique;

    // Vues du Footer (inchangées)
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
    private RelativeLayout notificationsButton;

    // Formats de date (inchangés)
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Format pour l'affichage de l'historique
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("EEEE", new Locale("fr", "FR"));
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // --- 0. Modèle de Données pour l'Historique ---
    private static class PresenceDay {
        final String dayName;
        final String date;
        final String status; // Ex: "Présent", "Absent Justifié", "Congé Payé"
        final String details; // Ex: "8h00 - 17h00", "Maladie"

        public PresenceDay(String dayName, String date, String status, String details) {
            this.dayName = dayName;
            this.date = date;
            this.status = status;
            this.details = details;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        // --- 1. Initialisation des Vues ---
        presenceActionContainer = findViewById(R.id.presence_action_container);
        btnMarquerPresence = findViewById(R.id.btn_marquer_presence);
        btnJustifierAbsence = findViewById(R.id.btn_justifier_absence);
        notificationsButton = findViewById(R.id.notificationsButton);
        recyclerViewHistorique = findViewById(R.id.recyclerViewHistorique);

        // Initialisation du Footer (inchangée)
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

        // --- 2. Vérification de l'état de présence persisté ET du contrôle d'absence ---
        // ⚠️ NOTE : Le chargement du genre de l'utilisateur doit avoir lieu AVANT setupPresenceHistory().
        userGender = loadUserGender(); // Chargement du genre de l'utilisateur

        checkAndRestorePresenceState();

        // --- 3. Configuration des Actions ---
        setupClickListeners();

        // --- 4. Configuration du Footer ---
        setupFooterHighlight();

        // --- 5. Affichage de l'Historique de la Semaine Précédente ---
        setupPresenceHistory();
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

    private void setupPresenceHistory() {
        List<PresenceDay> historyData = generatePreviousWeekHistoryData();

        // ⚠️ Passage du genre à l'Adapter
        PresenceHistoryAdapter adapter = new PresenceHistoryAdapter(historyData, userGender);
        recyclerViewHistorique.setAdapter(adapter);
        recyclerViewHistorique.setLayoutManager(new LinearLayoutManager(this));
    }

    private List<PresenceDay> generatePreviousWeekHistoryData() {
        List<PresenceDay> history = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Déplace le calendrier au lundi de la semaine précédente.
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // Ajout des jours du Lundi au Vendredi
        for (int i = 0; i < 5; i++) { // 5 jours ouvrés
            Date day = calendar.getTime();
            String dayName = displayDayFormat.format(day);
            String dateString = displayDateFormat.format(day);

            String status;
            String details;

            if (i == 0 || i == 3) { // Lundi, Jeudi
                status = "Absent Justifié";
                details = "Maladie";
            } else if (i == 1) { // Mardi
                status = "Congé Payé";
                details = "Congé annuel";
            } else { // Mercredi, Vendredi
                status = "Présent";
                details = "8h00 - 17h00";
            }

            history.add(new PresenceDay(capitalize(dayName), dateString, status, details));
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Passer au jour suivant
        }

        return history;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

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

            // Extrait la base du statut (Présent, Absent, Congé)
            String statutBase = day.status.split(" ")[0];
            // Applique l'accord grammatical ("Présent" -> "Présente")
            String statutBadgeText = getGenderSpecificStatus(statutBase, gender);

            // --- 1. Définir le jour et la date ---
            holder.tvJourDate.setText(day.dayName + " " + day.date);

            // --- 2. Définir l'icône, la couleur et la bordure ---
            int color;
            int iconRes;
            int borderBackgroundRes;

            if (day.status.contains("Présent")) {
                color = getResources().getColor(COLOR_GREEN, null);
                iconRes = R.drawable.approuve;
                borderBackgroundRes = R.drawable.approuve_border; // Bordure verte pour présent

            } else if (day.status.contains("Absent")) {
                color = getResources().getColor(COLOR_RED, null);
                iconRes = R.drawable.refuse;
                borderBackgroundRes = R.drawable.border_redlight; // Bordure rouge pour absent

            } else if (day.status.contains("Congé")) {
                color = getResources().getColor(COLOR_BLUE, null);
                iconRes = R.drawable.conge_blue;
                borderBackgroundRes = R.drawable.border_blue; // Bordure bleue pour congé

            } else {
                color = getResources().getColor(COLOR_GREY, null);
                iconRes = R.drawable.time_bleu;
                borderBackgroundRes = R.drawable.simple_border; // Bordure grise par défaut
            }

            // Appliquer la couleur et l'icône
            holder.statusIcon.setImageResource(iconRes);
            holder.statusIcon.setColorFilter(color);

            // Appliquer le texte et la couleur du badge
            holder.tvStatutPresence.setText(statutBadgeText);

            // --- MODIFICATION IMPORTANTE : Couleur du texte différente pour absent ---
            if (day.status.contains("Absent")) {
                holder.tvStatutPresence.setTextColor(getResources().getColor(COLOR_RED, null));
            } else {
                // Pour présent et congé, garder la couleur originale
                holder.tvStatutPresence.setTextColor(color);
            }

            // Appliquer la bordure à la carte entière
            holder.itemView.setBackgroundResource(borderBackgroundRes);

            // --- 3. Définir les détails ---
            // On affiche le détail (ex: 8h00 - 17h00)
            holder.tvDetailsPresence.setText(day.details);
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView statusIcon;
            final TextView tvJourDate;
            final TextView tvDetailsPresence;
            // ⚠️ AJOUT IMPORTANT : Récupération du TextView du statut
            final TextView tvStatutPresence;

            public ViewHolder(View view) {
                super(view);
                // Mappage des IDs des vues de item_card_presence.xml
                statusIcon = view.findViewById(R.id.statusIcon);
                tvJourDate = view.findViewById(R.id.tv_jour_date);
                tvDetailsPresence = view.findViewById(R.id.tv_details_presence);
                // ⚠️ Récupération du badge de statut
                tvStatutPresence = view.findViewById(R.id.tv_statut_presence);
            }
        }
    }

    private String getGenderSpecificStatus(String baseStatus, String gender) {
        if ("F".equals(gender)) {
            if (baseStatus.equals("Présent")) {
                return "Présente";
            } else if (baseStatus.equals("Absent")) {
                return "Absente";
            }
        }
        return baseStatus; // Retourne "Présent", "Absent" (pour M), "Congé", etc.
    }

    private void checkAndRestorePresenceState() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedTime = prefs.getString(KEY_ARRIVAL_TIME, null);

        Date now = new Date();
        boolean isPresenceMarked = false;

        if (savedTime != null) {
            try {
                Date arrivalDateTime = dateTimeFormat.parse(savedTime);

                if (isSameDay(arrivalDateTime, now)) {
                    // La présence est marquée aujourd'hui
                    isPresenceMarked = true;
                    Log.i("PresenceActivity", "Restoration de l'état de présence marqué.");

                    SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String displayTime = displaySdf.format(arrivalDateTime);
                    restorePresenceCard(displayTime);
                } else {
                    // Minuit est passé, réinitialisation
                    Log.i("PresenceActivity", "Minuit est passé, réinitialisation de l'état de présence.");
                    clearPresenceState();
                }
            } catch (ParseException e) {
                Log.e("PresenceActivity", "Erreur lors de l'analyse de l'heure persistante.", e);
                clearPresenceState();
            }
        }

        // --- Contrôle d'Absence Non Justifiée ---
        if (!isPresenceMarked) {
            // Vérifie si la notification d'absence non justifiée a déjà été envoyée aujourd'hui
            String lastNotifDateString = prefs.getString(KEY_LAST_ABSENCE_NOTIFICATION_DATE, "");
            String todayDateString = dateFormat.format(now);

            if (!todayDateString.equals(lastNotifDateString)) {
                // Si aucune présence n'est marquée et la notification d'absence n'a pas été envoyée aujourd'hui
                sendUnjustifiedAbsenceNotification();

                // Met à jour la date de la dernière notification d'absence pour éviter le spam
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_LAST_ABSENCE_NOTIFICATION_DATE, todayDateString);
                editor.apply();
            }
        }
    }

    private void sendPresenceMarkedNotification(String arrivalTime) {
        // Enregistrement dans le stockage RH
        recordNotification(
                "presence_marked",
                "L'employé a marqué sa présence à " + arrivalTime + ".",
                "Présence Enregistrée"
        );
    }

    private void sendUnjustifiedAbsenceNotification() {
        // Enregistrement dans le stockage RH
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

            // Ajout au début de la liste
            JSONArray updatedArray = new JSONArray();
            updatedArray.put(newEntry);

            for (int i = 0; i < historyArray.length(); i++) {
                updatedArray.put(historyArray.getJSONObject(i));
            }

            // Sauvegarde du nouvel historique
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(NOTIFICATION_KEY, updatedArray.toString());
            editor.apply();

            Log.d("RH_NOTIF", toastTitle + " enregistrée : " + newEntry.toString());

        } catch (Exception e) {
            Log.e("RH_NOTIF", "Erreur lors de l'enregistrement de la notification JSON", e);
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
        // Logique de présence/absence
        btnMarquerPresence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkIfAlreadyMarkedToday()) {
                    replacePresenceActionWithCard();
                } else {
                    Toast.makeText(PresenceActivity.this, "Votre présence est déjà enregistrée pour aujourd'hui.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnJustifierAbsence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJustifyAbsenceFragment();
            }
        });

        // Logique de navigation du Footer
        iconAccueil.setOnClickListener(v -> navigateTo(AcceuilEmployeActivity.class));
        iconConges.setOnClickListener(v -> navigateTo(CongesEmployeActivity.class));
        iconReunions.setOnClickListener(v -> navigateTo(ReunionEmployeActivity.class));
        iconProfil.setOnClickListener(v -> navigateTo(ProfileEmployeActivity.class));
        notificationsButton.setOnClickListener(v -> navigateTo(NotificationsEmployesActivity.class));
    }

    private void replacePresenceActionWithCard() {
        // 1. ENREGISTREMENT DE L'HEURE D'ARRIVÉE ET STOCKAGE PERSISTANT
        Date now = new Date();

        // Stocke la date et l'heure complètes pour la vérification de minuit
        String fullDateTime = dateTimeFormat.format(now);
        savePresenceState(fullDateTime); // <-- Stockage persistant

        // Extrait uniquement l'heure pour l'affichage
        SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String displayTime = displaySdf.format(now);
        restorePresenceCard(displayTime);
        sendPresenceMarkedNotification(displayTime); // <-- NOUVEAU: Envoie la notification de présence
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
        iconPresence.setColorFilter(getResources().getColor(COLOR_BLUE, getTheme()));
        textPresence.setTextColor(getResources().getColor(COLOR_BLUE, getTheme()));

        iconAccueil.setColorFilter(getResources().getColor(COLOR_GREY, getTheme()));
        textAccueil.setTextColor(getResources().getColor(COLOR_GREY, getTheme()));
        iconConges.setColorFilter(getResources().getColor(COLOR_GREY, getTheme()));
        textConges.setTextColor(getResources().getColor(COLOR_GREY, getTheme()));
        iconReunions.setColorFilter(getResources().getColor(COLOR_GREY, getTheme()));
        textReunions.setTextColor(getResources().getColor(COLOR_GREY, getTheme()));
        iconProfil.setColorFilter(getResources().getColor(COLOR_GREY, getTheme()));
        textProfil.setTextColor(getResources().getColor(COLOR_GREY, getTheme()));
    }
}