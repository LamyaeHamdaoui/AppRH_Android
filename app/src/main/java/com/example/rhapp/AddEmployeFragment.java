package com.example.rhapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.rhapp.model.Employe;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEmployeFragment extends Fragment {

    private EditText nom, prenom, email, telephone, poste, soldeConge, dateEmbauche;
    private Spinner departement;
    private RadioButton radioEmploye, radioRh;
    private FirebaseFirestore db;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AddEmployeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_add_employe, container, false);

        initViews(v);
        initListeners();
        styliserSpinner();

        db = FirebaseFirestore.getInstance();

        return v;
    }

    private void initViews(View v) {
        nom = v.findViewById(R.id.nom);
        prenom = v.findViewById(R.id.prenom);
        email = v.findViewById(R.id.email);
        telephone = v.findViewById(R.id.telephone);
        poste = v.findViewById(R.id.poste);
        soldeConge = v.findViewById(R.id.soldeConge);
        dateEmbauche = v.findViewById(R.id.dateEmbauche);
        departement = v.findViewById(R.id.departement);
        radioEmploye = v.findViewById(R.id.radioEmploye);
        radioRh = v.findViewById(R.id.radioRh);

        Button btnAjouter = v.findViewById(R.id.btnAddEmploye);
        Button btnAnnuler = v.findViewById(R.id.btnAnnuler);

        btnAjouter.setOnClickListener(view -> ajouterEmploye());
        btnAnnuler.setOnClickListener(view -> retourEcranPrincipal());
    }

    private void initListeners() {}

    private void retourEcranPrincipal() {
        if (getActivity() != null) getActivity().onBackPressed();
    }

    private void ajouterEmploye() {

        // 1 — Vérification des champs
        if (!verifierChamps()) return;

        executorService.execute(() -> {

            // 2 — Conversion de la date en dehors du thread UI
            Timestamp timestampEmbauche = parseDate(dateEmbauche.getText().toString().trim());
            if (timestampEmbauche == null) {
                afficherMessage("Format de date incorrect (jj/MM/aaaa)");
                return;
            }

            // 3 — Récupération des données
            String role = radioEmploye.isChecked() ? "employe" : "rh";

            DocumentReference docRef = db.collection("employees").document();
            String employeId = docRef.getId();

            Employe employe = new Employe(
                    employeId,
                    nom.getText().toString().trim(),
                    prenom.getText().toString().trim(),
                    email.getText().toString().trim(),
                    role,
                    "",
                    poste.getText().toString().trim(),
                    departement.getSelectedItem().toString(),
                    timestampEmbauche,
                    Integer.parseInt(soldeConge.getText().toString().trim()),
                    telephone.getText().toString().trim(),
                    null,
                    false
            );

            // 4 — Ajout Firestore (Firestore gère déjà son propre thread)
            docRef.set(employe)
                    .addOnSuccessListener(unused -> afficherMessageEtRetour("Employé ajouté !"))
                    .addOnFailureListener(e -> afficherMessage("Erreur ajout employé : " + e.getMessage()));
        });
    }

    private boolean verifierChamps() {
        if (TextUtils.isEmpty(nom.getText().toString()) ||
                TextUtils.isEmpty(prenom.getText().toString()) ||
                TextUtils.isEmpty(email.getText().toString()) ||
                TextUtils.isEmpty(telephone.getText().toString()) ||
                TextUtils.isEmpty(poste.getText().toString()) ||
                TextUtils.isEmpty(soldeConge.getText().toString()) ||
                TextUtils.isEmpty(dateEmbauche.getText().toString())) {

            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Timestamp parseDate(String dateText) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateText);
            return new Timestamp(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private void afficherMessage(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show()
        );
    }

    private void afficherMessageEtRetour(String message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            retourEcranPrincipal();
        });
    }

    private void styliserSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.departements,
                R.layout.spinner_dropdown_item
        );
        departement.setAdapter(adapter);
    }
}
