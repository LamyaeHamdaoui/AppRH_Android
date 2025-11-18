package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditEmployeFragment extends Fragment {

    private EditText nom, prenom, email, telephone, poste, soldeConge;
    private Spinner departement;
    private Button btnAnnuler, btnEnregistrer;

    private FirebaseFirestore db;
    private String employeId; // <--- ID du document Firestore

    public EditEmployeFragment() { }

    public static EditEmployeFragment newInstance(String employeId) {
        EditEmployeFragment fragment = new EditEmployeFragment();
        Bundle args = new Bundle();
        args.putString("employeId", employeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            employeId = getArguments().getString("employeId");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_employe, container, false);

        // Récupération des vues
        nom = v.findViewById(R.id.nom);
        prenom = v.findViewById(R.id.prenom);
        email = v.findViewById(R.id.email);
        telephone = v.findViewById(R.id.telephone);
        poste = v.findViewById(R.id.poste);
        departement = v.findViewById(R.id.departement);
        soldeConge = v.findViewById(R.id.soldeConge);

        btnAnnuler = v.findViewById(R.id.btnAnnulerEditEmploye);
        btnEnregistrer = v.findViewById(R.id.btnenregistrerEditEmploye);

        chargerEmploye();

        btnEnregistrer.setOnClickListener(view -> enregistrerModifications());
        btnAnnuler.setOnClickListener(view -> requireActivity().onBackPressed());

        return v;
    }

    private void chargerEmploye() {
        db.collection("users").document(employeId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        nom.setText(document.getString("nom"));
                        prenom.setText(document.getString("prenom"));
                        email.setText(document.getString("email"));
                        telephone.setText(document.getString("telephone"));
                        poste.setText(document.getString("poste"));
                        soldeConge.setText(String.valueOf(document.getLong("soldeConge")));

                        // Département = Spinner
                        String dep = document.getString("departement");
                        setSpinnerValue(departement, dep);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erreur chargement", Toast.LENGTH_SHORT).show());
    }

    private void enregistrerModifications() {

        DocumentReference docRef = db.collection("users").document(employeId);

        docRef.update(
                "nom", nom.getText().toString(),
                "prenom", prenom.getText().toString(),
                "email", email.getText().toString(),
                "telephone", telephone.getText().toString(),
                "poste", poste.getText().toString(),
                "departement", departement.getSelectedItem().toString(),
                "soldeConge", Integer.parseInt(soldeConge.getText().toString())
        ).addOnSuccessListener(unused ->
                Toast.makeText(getContext(), "Modifications enregistrées", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show()
        );
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}
