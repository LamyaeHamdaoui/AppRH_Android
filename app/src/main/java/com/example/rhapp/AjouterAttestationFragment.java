package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AjouterAttestationFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private Spinner spinnerTypeAttestation;
    private EditText motifEditText;
    private Button btnEnvoyer;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String selectedType = "";

    public AjouterAttestationFragment() {
        // Required empty public constructor
    }

    public static AjouterAttestationFragment newInstance(String param1, String param2) {
        AjouterAttestationFragment fragment = new AjouterAttestationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ajouter_attestation, container, false);

        initViews(view);
        setupSpinner();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        spinnerTypeAttestation = view.findViewById(R.id.TypeAttestation);
        motifEditText = view.findViewById(R.id.motifEditText);
        btnEnvoyer = view.findViewById(R.id.btnEnvoyerDemande);
    }

    private void setupSpinner() {
        if (spinnerTypeAttestation != null) {
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
    }

    private void setupClickListeners() {
        if (btnEnvoyer != null) {
            btnEnvoyer.setOnClickListener(v -> envoyerDemande());
        }

        if (motifEditText != null) {
            motifEditText.setOnKeyListener((v, keyCode, event) -> {
                updateButtonState();
                return false;
            });
        }
    }

    private void updateButtonState() {
        if (btnEnvoyer != null) {
            boolean isTypeSelected = !selectedType.isEmpty();
            boolean hasMotif = motifEditText != null &&
                    motifEditText.getText().toString().trim().length() > 0;

            btnEnvoyer.setEnabled(isTypeSelected && hasMotif);
            btnEnvoyer.setAlpha(isTypeSelected && hasMotif ? 1.0f : 0.5f);
        }
    }

    private void envoyerDemande() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedType.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez sélectionner un type d'attestation", Toast.LENGTH_SHORT).show();
            return;
        }

        String motif = motifEditText != null ? motifEditText.getText().toString().trim() : "";

        // Créer l'objet Attestation
        Attestation attestation = new Attestation(
                currentUser.getUid(),
                "Nom Employé", // À remplacer par les vraies données utilisateur
                "Département", // À remplacer par les vraies données utilisateur
                selectedType,
                motif
        );

        // Sauvegarder dans Firebase
        db.collection("Attestations")
                .add(attestation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Demande envoyée avec succès", Toast.LENGTH_SHORT).show();
                    resetForm();
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur lors de l'envoi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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