package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.rhapp.model.Conge;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DetailsCongeFragment extends Fragment {

    private FirebaseFirestore db;
    private Conge conge;
    private String congeId;

    private TextView sousDetails, typeConge, datesConge, motifConge, soldeCongesEmploye;
    private EditText raisonRefus;

    public DetailsCongeFragment() {
        // Required empty public constructor
    }

    public static DetailsCongeFragment newInstance(String congeId) {
        DetailsCongeFragment fragment = new DetailsCongeFragment();
        Bundle args = new Bundle();
        args.putString("congeId", congeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details_conge, container, false);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            congeId = getArguments().getString("congeId");
            loadCongeDetails();
        }

        initializeViews(view);
        setupClickListeners(view);

        return view;
    }

    private void initializeViews(View view) {
        sousDetails = view.findViewById(R.id.sousDetails);
        typeConge = view.findViewById(R.id.typeConge);
        datesConge = view.findViewById(R.id.datesConge);
        motifConge = view.findViewById(R.id.MotifConge);
        soldeCongesEmploye = view.findViewById(R.id.soldeCongesEmploye);
        raisonRefus = view.findViewById(R.id.raisonrefusConge);
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnApprouver).setOnClickListener(v -> approuverConge());
        view.findViewById(R.id.btnRefuser).setOnClickListener(v -> refuserConge());
    }

    private void loadCongeDetails() {
        if (congeId == null) {
            Toast.makeText(getContext(), "Erreur: ID du congé non défini", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("conges").document(congeId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        conge = document.toObject(Conge.class);
                        if (conge != null) {
                            // Définir l'ID du congé
                            conge.setId(document.getId());
                            updateUIWithCongeDetails();
                        }
                    } else {
                        Toast.makeText(getContext(), "Erreur lors du chargement des détails", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUIWithCongeDetails() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH);

        String details = conge.getUserName() + " - " + conge.getUserDepartment();
        sousDetails.setText(details);

        typeConge.setText(conge.getTypeConge());

        String dates = dateFormat.format(conge.getDateDebut()) + " - " + dateFormat.format(conge.getDateFin());
        datesConge.setText(dates);

        motifConge.setText(conge.getMotif());
        soldeCongesEmploye.setText("25 jours"); // À calculer selon l'historique
    }

    private void approuverConge() {
        updateCongeStatus("Approuvé", "");
    }

    private void refuserConge() {
        String raison = raisonRefus.getText().toString().trim();
        if (raison.isEmpty()) {
            raison = "Raison non spécifiée";
        }
        updateCongeStatus("Refusé", raison);
    }

    private void updateCongeStatus(String nouveauStatut, String raison) {
        if (congeId == null) {
            Toast.makeText(getContext(), "Erreur: ID du congé non défini", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = db.collection("conges").document(congeId);

        docRef.update("statut", nouveauStatut)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String message = "Demande " + nouveauStatut.toLowerCase() + " avec succès";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } else {
                        Toast.makeText(getContext(), "Erreur lors de la mise à jour: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}