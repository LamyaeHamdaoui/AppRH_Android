package com.example.rhapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AjouterAttestationFragment extends Fragment {

    private static final String TAG = "AjouterAttestation";

    private Spinner spinnerTypeAttestation;
    private EditText motifEditText;
    private Button btnEnvoyer;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private String selectedType = "";

    public AjouterAttestationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ajouter_attestation, container, false);

        spinnerTypeAttestation = view.findViewById(R.id.TypeAttestation);
        motifEditText = view.findViewById(R.id.motifEditText);
        btnEnvoyer = view.findViewById(R.id.btnEnvoyerDemande);

        setupSpinner();
        setupListeners();

        // Désactiver le bouton au démarrage
        btnEnvoyer.setEnabled(false);

        return view;
    }

    private void setupSpinner() {
        // Liste des types d'attestation
        String[] types = new String[]{"Sélectionnez le type", "Attestation de travail", "Attestation de salaire", "Attestation de présence"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerTypeAttestation.setAdapter(adapter);

        spinnerTypeAttestation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedType = parent.getItemAtPosition(position).toString();
                } else {
                    selectedType = "";
                }
                updateButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedType = "";
                updateButtonState();
            }
        });
    }

    private void setupListeners() {
        btnEnvoyer.setOnClickListener(v -> {
            envoyerDemandeAttestation();
        });

        // TextWatcher pour la validation dynamique
        motifEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateButtonState() {
        String motif = motifEditText.getText().toString().trim();
        boolean isValid = !selectedType.isEmpty() && motif.length() > 5;
        btnEnvoyer.setEnabled(isValid);
    }

    private void envoyerDemandeAttestation() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnvoyer.setEnabled(false);
        String userEmail = currentUser.getEmail();

        Log.d(TAG, "Récupération des données employé pour: " + userEmail);

        // L'utilisateur connecté est forcément un employé
        db.collection("employees")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var document = queryDocumentSnapshots.getDocuments().get(0);

                        String nom = document.getString("nom");
                        String prenom = document.getString("prenom");
                        String departement = document.getString("departement");
                        String nomComplet = document.getString("nomComplet");

                        // Utiliser nomComplet si disponible, sinon nom + prénom
                        String nomAffiche = (nomComplet != null && !nomComplet.isEmpty()) ?
                                nomComplet : prenom + " " + nom;

                        String dept = (departement != null && !departement.isEmpty()) ?
                                departement : "Non spécifié";

                        Log.d(TAG, "Données employé récupérées: " + nomAffiche + " - " + dept);
                        creerEtEnvoyerAttestation(nomAffiche, dept);
                    } else {
                        // Normalement ce cas ne devrait pas arriver
                        Toast.makeText(getContext(), "Profil employé non trouvé", Toast.LENGTH_LONG).show();
                        btnEnvoyer.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnEnvoyer.setEnabled(true);
                    Log.e(TAG, "Erreur récupération employé: " + e.getMessage());
                });
    }

    private void creerEtEnvoyerAttestation(String employeNom, String employeDepartement) {
        String motif = motifEditText.getText().toString().trim();

        // Créer l'objet Attestation
        Attestation attestation = new Attestation(
                currentUser.getUid(),
                employeNom,
                employeDepartement,
                selectedType,
                motif
        );

        // Sauvegarder dans Firestore
        db.collection("Attestations")
                .add(attestation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Demande envoyée avec succès", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Attestation créée avec ID: " + documentReference.getId());
                    resetForm();

                    // Revenir en arrière
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur lors de l'envoi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnEnvoyer.setEnabled(true);
                    Log.e(TAG, "Erreur création attestation: " + e.getMessage());
                });
    }

    private void resetForm() {
        if (spinnerTypeAttestation != null) {
            spinnerTypeAttestation.setSelection(0);
        }
        if (motifEditText != null) {
            motifEditText.setText("");
        }
        selectedType = "";
        updateButtonState();
    }
}