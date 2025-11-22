package com.example.rhapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.rhapp.model.Conge;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DemandeCongeFragment extends Fragment {

    private FirebaseFirestore db;

    private Spinner typeCongeSpinner;
    private EditText dateDebutEditText, dateFinEditText, motifEditText;
    private Button btnEnvoyerDemande;

    private Calendar calendarDebut, calendarFin;
    private SimpleDateFormat dateFormat;

    public DemandeCongeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_demande_conge, container, false);

        // Initialisation Firebase seulement Firestore
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupDatePickers();
        setupSpinner();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        typeCongeSpinner = view.findViewById(R.id.typeConge);
        dateDebutEditText = view.findViewById(R.id.dateDebutConge);
        dateFinEditText = view.findViewById(R.id.dateFinConge);
        motifEditText = view.findViewById(R.id.MotifConge);
        btnEnvoyerDemande = view.findViewById(R.id.btnEnvoyerDemande);

        calendarDebut = Calendar.getInstance();
        calendarFin = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

        // Définir la date de début à aujourd'hui
        dateDebutEditText.setText(dateFormat.format(calendarDebut.getTime()));
        // Définir la date de fin à aujourd'hui + 1 jour
        calendarFin.add(Calendar.DAY_OF_MONTH, 1);
        dateFinEditText.setText(dateFormat.format(calendarFin.getTime()));
    }

    private void setupSpinner() {
        if (getContext() != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    getContext(),
                    R.array.types_conge,
                    android.R.layout.simple_spinner_item
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeCongeSpinner.setAdapter(adapter);
        }
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener dateDebutListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendarDebut.set(Calendar.YEAR, year);
                calendarDebut.set(Calendar.MONTH, month);
                calendarDebut.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateEditText(dateDebutEditText, calendarDebut);
            }
        };

        DatePickerDialog.OnDateSetListener dateFinListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendarFin.set(Calendar.YEAR, year);
                calendarFin.set(Calendar.MONTH, month);
                calendarFin.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateEditText(dateFinEditText, calendarFin);
            }
        };

        dateDebutEditText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), dateDebutListener,
                    calendarDebut.get(Calendar.YEAR),
                    calendarDebut.get(Calendar.MONTH),
                    calendarDebut.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });

        dateFinEditText.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), dateFinListener,
                    calendarFin.get(Calendar.YEAR),
                    calendarFin.get(Calendar.MONTH),
                    calendarFin.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMinDate(calendarDebut.getTimeInMillis() - 1000);
            dialog.show();
        });
    }

    private void updateDateEditText(EditText editText, Calendar calendar) {
        editText.setText(dateFormat.format(calendar.getTime()));
    }

    private void setupClickListeners() {
        btnEnvoyerDemande.setOnClickListener(v -> envoyerDemandeConge());
    }

    private void envoyerDemandeConge() {
        String typeConge = typeCongeSpinner.getSelectedItem().toString();
        String dateDebutStr = dateDebutEditText.getText().toString();
        String dateFinStr = dateFinEditText.getText().toString();
        String motif = motifEditText.getText().toString();

        // Validation
        if (dateDebutStr.isEmpty() || dateFinStr.isEmpty() || motif.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (motif.length() < 5) {
            Toast.makeText(getContext(), "Veuillez donner plus de détails dans le motif", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date dateDebut = dateFormat.parse(dateDebutStr);
            Date dateFin = dateFormat.parse(dateFinStr);

            if (dateDebut == null || dateFin == null) {
                Toast.makeText(getContext(), "Format de date invalide", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier que la date de fin est après la date de début
            if (dateFin.before(dateDebut)) {
                Toast.makeText(getContext(), "La date de fin doit être après la date de début", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calcul de la durée en jours
            long difference = dateFin.getTime() - dateDebut.getTime();
            int duree = (int) (difference / (1000 * 60 * 60 * 24)) + 1;

            if (duree <= 0) {
                Toast.makeText(getContext(), "La durée doit être d'au moins 1 jour", Toast.LENGTH_SHORT).show();
                return;
            }

            // Générer un ID unique pour l'utilisateur (sans authentification)
            String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
            String userName = "Employé Test";
            String userDepartment = "Développement";

            // Création de l'objet Conge
            Conge conge = new Conge(
                    userId,
                    userName,
                    userDepartment,
                    typeConge,
                    dateDebut,
                    dateFin,
                    duree,
                    motif,
                    "En attente" // Statut par défaut
            );

            // Afficher un message de chargement
            Toast.makeText(getContext(), "Envoi en cours...", Toast.LENGTH_SHORT).show();

            // Enregistrement dans Firestore
            db.collection("conges")
                    .add(conge)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "✅ Demande envoyée avec succès !", Toast.LENGTH_LONG).show();

                            // Vider les champs
                            dateDebutEditText.setText("");
                            dateFinEditText.setText("");
                            motifEditText.setText("");

                            // Retour à l'écran précédent
                            if (getActivity() != null) {
                                getActivity().onBackPressed();
                            }
                        } else {
                            Toast.makeText(getContext(), "❌ Erreur lors de l'envoi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (ParseException e) {
            Toast.makeText(getContext(), "❌ Erreur de format de date", Toast.LENGTH_SHORT).show();
        }
    }
}