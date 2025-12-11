package com.example.rhapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class JustifyAbsenceFragment extends DialogFragment {

    // Interface de Callback
    public interface JustificationListener {
        void onAbsenceJustified(String justificationDetails);
    }

    private JustificationListener listener;

    // Vues
    private EditText justificationInput;
    private Button btnEnvoyer;
    // ... (autres vues)

    public JustifyAbsenceFragment() {
        // Constructeur public vide requis
    }

    // 1. Attacher le Fragment à l'Activity pour initialiser le listener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof JustificationListener) {
            listener = (JustificationListener) context;
        } else {
            // Sécurité : assurez-vous que l'Activity implémente l'interface
            throw new RuntimeException(context.toString()
                    + " doit implémenter JustificationListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_justfy_absence, container, false);

        // Initialisation des vues
        justificationInput = view.findViewById(R.id.justificationInput);
        btnEnvoyer = view.findViewById(R.id.btnEnvoyer);

        btnEnvoyer.setOnClickListener(v -> {
            String justification = justificationInput.getText().toString().trim();

            // 1. Vérification si le champ est vide
            if (justification.isEmpty()) {
                Toast.makeText(getContext(), "Veuillez entrer une justification.", Toast.LENGTH_SHORT).show();
                return; // Arrêter l'exécution
            }

            // 2. Vérification si c'est un jour ouvré
            Calendar cal = Calendar.getInstance();
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ..., Calendar.SATURDAY = 7
            boolean isWorkingDay = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;

            if (isWorkingDay) {
                if (listener != null) {
                    // Passer la donnée à l'Activity pour la sauvegarde Firestore/RH
                    listener.onAbsenceJustified(justification);
                    dismiss();
                }

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            } else {
                // C'est Samedi ou Dimanche, on refuse l'action.
                Toast.makeText(getContext(), "La justification d'absence n'est requise que du Lundi au Vendredi.", Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}