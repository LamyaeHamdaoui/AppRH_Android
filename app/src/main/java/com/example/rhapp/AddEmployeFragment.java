package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.rhapp.model.Employe;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddEmployeFragment extends Fragment {

    private EditText nom, prenom, email, telephone, poste, soldeConge;
    private Spinner departement;
    private Button btnAjouter;

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

        db = FirebaseFirestore.getInstance();

        btnAjouter.setOnClickListener(view -> ajouterEmploye());

        return v;
    }

    private void ajouterEmploye() {
        // 1️⃣ Créer un document Firestore avec ID automatique
        DocumentReference docRef = db.collection("users").document();
        String employeId = docRef.getId(); //

        // 2️⃣ Créer l'objet Employe
        Employe employe = new Employe(
                employeId,
                nom.getText().toString(),
                prenom.getText().toString(),
                email.getText().toString(),
                "", // motDePasse vide pour l'instant
                "employe", // role
                "", // photo
                poste.getText().toString(),
                departement.getSelectedItem().toString(),
                "", // dateEmbauche
                Integer.parseInt(soldeConge.getText().toString()),
                telephone.getText().toString()
        );

        // 3️⃣ Ajouter dans Firestore
        docRef.set(employe)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Employé ajouté !", Toast.LENGTH_SHORT).show();

                   /*// --- Ouvrir EditEmployeFragment pour modification immédiate si besoin ---
                    EditEmployeFragment editFragment = EditEmployeFragment.newInstance(employeId);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, editFragment)
                            .addToBackStack(null)
                            .commit(); */
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erreur ajout employé", Toast.LENGTH_SHORT).show()
                );
    }
}
