package com.example.rhapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class AjouterAttestationFragment extends Fragment {

    private EditText editMotif;
    private Spinner spinnerTypeAttestation;
    private Button btnEnvoyer;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup viewGroup, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ajouter_attestation, viewGroup, false);

        // CORRECTION: Utiliser view.findViewById() dans un Fragment
        editMotif = view.findViewById(R.id.motifEditText);
        spinnerTypeAttestation = view.findViewById(R.id.TypeAttestation); // ← Correction ici
        btnEnvoyer = view.findViewById(R.id.btnEnvoyerDemande);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Activer le bouton quand un type est sélectionné
        spinnerTypeAttestation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                btnEnvoyer.setEnabled(position > 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                btnEnvoyer.setEnabled(false);
            }
        });

        btnEnvoyer.setOnClickListener(v -> envoyerDemande());
        styliserSpinner();
        return view;
    }

    private void envoyerDemande() {
        String motif = editMotif.getText().toString().trim();
        String typeAttestation = spinnerTypeAttestation.getSelectedItem().toString();

        if (spinnerTypeAttestation.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Veuillez sélectionner le type d'attestation", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        // Rechercher l'employé par email
        db.collection("employees")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot employeDoc = queryDocumentSnapshots.getDocuments().get(0);

                        String employeNom = employeDoc.getString("prenom") + " " + employeDoc.getString("nom");
                        String employeDepartement = employeDoc.getString("departement");

                        // Créer l'attestation avec la structure EXACTE de votre Firestore
                        Map<String, Object> attestationData = new HashMap<>();
                        attestationData.put("employeeId", uid); // Firebase Auth UID
                        attestationData.put("employeeNom", employeNom);
                        attestationData.put("employeeDepartment", employeDepartement); // Orthographe exacte
                        attestationData.put("employeEmail", email);
                        attestationData.put("typeAttestation", typeAttestation);
                        attestationData.put("motif", motif);
                        attestationData.put("statut", "en_attente");
                        attestationData.put("dateDemande", new Date());
                        attestationData.put("dateTraitement", null);
                        attestationData.put("motifRefus", null);
                        attestationData.put("pdfUrl", null);

                        db.collection("Attestations")
                                .add(attestationData)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getContext(), "Demande envoyée avec succès", Toast.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        getActivity().getSupportFragmentManager().popBackStack();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("AJOUT_ATTESTATION", "Erreur Firestore", e);
                                });

                    } else {
                        Toast.makeText(getContext(), "Employé non trouvé dans la base", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur recherche employé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("AJOUT_ATTESTATION", "Erreur recherche employé", e);
                });
    }

    private void styliserSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.type_attestation,
                R.layout.spinner_dropdown_item  // Utilise notre layout personnalisé
        );
        spinnerTypeAttestation.setAdapter(adapter);
    }



}