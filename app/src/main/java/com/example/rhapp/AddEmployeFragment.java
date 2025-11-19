package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.Timestamp;

import androidx.fragment.app.Fragment;



import com.example.rhapp.model.Employe;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddEmployeFragment extends Fragment {

    private EditText nom, prenom, email, telephone, poste, soldeConge,dateEmbauche;
    private Spinner departement;
    private Button btnAjouter;
    private RadioButton radioEmploye , radioRh;
    private String role="employe";

    private FirebaseFirestore db;

    public AddEmployeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_employe, container, false);

        nom = v.findViewById(R.id.nom);
        prenom = v.findViewById(R.id.prenom); // Assure-toi que ton EditText pour prénom existe
        email = v.findViewById(R.id.email);
        telephone = v.findViewById(R.id.telephone);
        poste = v.findViewById(R.id.poste);
        departement = v.findViewById(R.id.departement);
        soldeConge = v.findViewById(R.id.soldeConge);
        btnAjouter = v.findViewById(R.id.btnAddEmploye);
        radioEmploye = v.findViewById(R.id.radioEmploye);
        radioRh = v.findViewById(R.id.radioRh);
        db = FirebaseFirestore.getInstance();
        btnAjouter.setOnClickListener(view -> ajouterEmploye());
        Button btnAnnuler = v.findViewById(R.id.btnAnnuler);
        btnAnnuler.setOnClickListener(view -> retourEcranPrincipal());
        dateEmbauche = v.findViewById(R.id.dateEmbauche);

        db = FirebaseFirestore.getInstance();
        btnAjouter.setOnClickListener(view -> ajouterEmploye());


        return v;
    }

    private void retourEcranPrincipal() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void ajouterEmploye() {
        if (nom.getText().toString().trim().isEmpty() ||
                prenom.getText().toString().trim().isEmpty() ||
                email.getText().toString().trim().isEmpty() ||
                telephone.getText().toString().trim().isEmpty() ||
                poste.getText().toString().trim().isEmpty() ||
                soldeConge.getText().toString().trim().isEmpty() ||
                dateEmbauche.getText().toString().trim().isEmpty()) {

            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Conversion date jj/MM/yyyy -> Timestamp
        Date date;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            date = sdf.parse(dateEmbauche.getText().toString().trim());
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Format de date incorrect (jj/MM/aaaa)", Toast.LENGTH_SHORT).show();
            return;
        }
        Timestamp timestampEmbauche = new Timestamp(date);

        role = radioEmploye.isChecked() ? "employe" : "rh";

        DocumentReference docRef = db.collection("users").document();
        String employeId = docRef.getId();

        Employe employe = new Employe(
                employeId,
                nom.getText().toString(),
                prenom.getText().toString(),
                email.getText().toString(),
                role,
                "", // photo
                poste.getText().toString(),
                departement.getSelectedItem().toString(),
                timestampEmbauche, // Timestamp ici
                Integer.parseInt(soldeConge.getText().toString()),
                telephone.getText().toString()
        );

        docRef.set(employe)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Employé ajouté !", Toast.LENGTH_SHORT).show();
                    retourEcranPrincipal();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erreur ajout employé", Toast.LENGTH_SHORT).show());
    }

}
