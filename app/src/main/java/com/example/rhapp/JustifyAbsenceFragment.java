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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JustifyAbsenceFragment extends Fragment {

    // Constante pour le stockage des notifications RH (Simule une base de données)
    private static final String NOTIFICATION_PREF_NAME = "RhNotifications";
    private static final String NOTIFICATION_KEY = "justification_history";

    // Vues (selon votre XML)
    private EditText justificationInput;
    private Button btnEnvoyer;
    private TextView title, subTitle; // Déclarations pour référence

    public JustifyAbsenceFragment() {
        // Constructeur public vide requis
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_justfy_absence, container, false);

        // 1. Initialisation des vues
        justificationInput = view.findViewById(R.id.justificationInput);
        btnEnvoyer = view.findViewById(R.id.btnEnvoyer);
        title = view.findViewById(R.id.title);
        subTitle = view.findViewById(R.id.subTitle);

        // 2. Configuration du Listener
        btnEnvoyer.setOnClickListener(v -> {
            String justification = justificationInput.getText().toString().trim();
            if (justification.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez entrer une justification.", Toast.LENGTH_SHORT).show();
            } else {
                sendJustificationAsNotification(justification);

                // 3. Retour à l'écran précédent (PresenceActivity)
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();

                    // 4. Facultatif: Lancer NotificationsRhActivity après l'envoi
                    // Intent intent = new Intent(getActivity(), NotificationsRhActivity.class);
                    // startActivity(intent);
                }
            }
        });

        return view;
    }

    /**
     * Enregistre la justification d'absence dans SharedPreferences pour simuler
     * l'ajout d'une nouvelle "notification" destinée aux RH.
     * @param message La justification entrée par l'utilisateur.
     */
    private void sendJustificationAsNotification(String message) {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(NOTIFICATION_PREF_NAME, Context.MODE_PRIVATE);
        String existingHistory = prefs.getString(NOTIFICATION_KEY, "[]");

        try {
            JSONArray historyArray = new JSONArray(existingHistory);

            // Création de l'objet de justification
            JSONObject newEntry = new JSONObject();
            newEntry.put("type", "absence_justification");
            newEntry.put("message", message);

            // Formatage de l'heure et de la date
            SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date now = new Date();

            newEntry.put("time", timeSdf.format(now));
            newEntry.put("date", dateSdf.format(now));

            // Ajout du nouvel enregistrement au début de l'historique (comme une notification récente)
            historyArray.put(0, newEntry);

            // Sauvegarde du nouvel historique
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(NOTIFICATION_KEY, historyArray.toString());
            editor.apply();

            Log.d("RH_NOTIF", "Justification enregistrée : " + newEntry.toString());
            Toast.makeText(getContext(), "Justification envoyée aux RH !", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("RH_NOTIF", "Erreur lors de l'enregistrement de la justification JSON", e);
            Toast.makeText(getContext(), "Erreur lors de l'envoi.", Toast.LENGTH_SHORT).show();
        }
    }
}