package com.example.rhapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class AddEmployeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_employe, container, false);

        EditText nom = view.findViewById(R.id.nom);
        EditText prenom = view.findViewById(R.id.prenom);
        EditText email = view.findViewById(R.id.email);
        EditText telephone = view.findViewById(R.id.telephone);
        EditText poste = view.findViewById(R.id.poste);
        Spinner departement = view.findViewById(R.id.departement);
        EditText dateEmbauche = view.findViewById(R.id.dateEmbauche);
        EditText soldeConge = view.findViewById(R.id.soldeConge);
        Button btnAdd = view.findViewById(R.id.btnAddEmploye);

        btnAdd.setOnClickListener(v -> {

            String nomTxt = nom.getText().toString().trim();
            String prenomTxt = prenom.getText().toString().trim();
            String mail = email.getText().toString().trim();
            String tel = telephone.getText().toString().trim();
            String p = poste.getText().toString().trim();
            String dep = departement.getSelectedItem().toString();
            String date = dateEmbauche.getText().toString().trim();
            int solde = Integer.parseInt(soldeConge.getText().toString().trim());

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> user = new HashMap<>();
            user.put("nom", nomTxt);
            user.put("prenom", prenomTxt);
            user.put("email", mail);
            user.put("telephone", tel);
            user.put("poste", p);
            user.put("departement", dep);
            user.put("dateEmbauche", date);
            user.put("soldeConge", solde);
            user.put("role", "employe");
            user.put("photo", "");
            user.put("createdAt", new Date());

            db.collection("users")
                    .add(user)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(getContext(), "Employé ajouté dans Firestore", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }


}