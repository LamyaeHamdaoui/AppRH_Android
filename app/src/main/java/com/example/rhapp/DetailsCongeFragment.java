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
    private View backgroundOverlay;

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
        initializeViews(view);
        setupClickListeners(view);
        setupBackgroundClickListener(view);

        // Récupérer l'ID du congé et charger les données
        if (getArguments() != null) {
            congeId = getArguments().getString("congeId");
            if (congeId != null) {
                loadCongeDetails();
            } else {
                showError("ID du congé non trouvé");
            }
        } else {
            showError("Arguments manquants");
        }

        return view;
    }

    private void initializeViews(View view) {
        sousDetails = view.findViewById(R.id.sousDetails);
        typeConge = view.findViewById(R.id.typeConge);
        datesConge = view.findViewById(R.id.datesConge);
        motifConge = view.findViewById(R.id.MotifConge);
        soldeCongesEmploye = view.findViewById(R.id.soldeCongesEmploye);
        raisonRefus = view.findViewById(R.id.raisonrefusConge);
        //backgroundOverlay = view.findViewById(R.id.backgroundOverlay);

        // Initialiser avec des valeurs vides
        if (sousDetails != null) sousDetails.setText("");
        if (typeConge != null) typeConge.setText("");
        if (datesConge != null) datesConge.setText("");
        if (motifConge != null) motifConge.setText("");
        if (soldeCongesEmploye != null) soldeCongesEmploye.setText("");
        if (raisonRefus != null) raisonRefus.setText("");
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnApprouver).setOnClickListener(v -> approuverConge());
        view.findViewById(R.id.btnRefuser).setOnClickListener(v -> refuserConge());
    }

    private void setupBackgroundClickListener(View view) {
        if (backgroundOverlay != null) {
            backgroundOverlay.setOnClickListener(v -> {
                // Fermer le fragment quand on clique sur l'arrière-plan
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Empêcher la fermeture quand on clique sur la carte elle-même
        View detailsCard = view.findViewById(R.id.detailsCard);
        if (detailsCard != null) {
            detailsCard.setOnClickListener(v -> {
                // Ne rien faire - empêcher la propagation du clic
            });
        }
    }

    private void loadCongeDetails() {
        if (congeId == null || congeId.isEmpty()) {
            showError("ID du congé invalide");
            return;
        }

        db.collection("conges").document(congeId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            conge = document.toObject(Conge.class);
                            if (conge != null) {
                                // Définir l'ID du congé
                                conge.setId(document.getId());
                                updateUIWithCongeDetails();
                                loadEmployeeSolde();
                            } else {
                                showError("Données du congé invalides");
                            }
                        } else {
                            showError("Congé non trouvé dans la base de données");
                        }
                    } else {
                        showError("Erreur de connexion: " + task.getException().getMessage());
                    }
                });
    }

    private void loadEmployeeSolde() {
        if (conge == null || conge.getUserId() == null) {
            if (soldeCongesEmploye != null) {
                soldeCongesEmploye.setText("Non disponible");
            }
            return;
        }

        db.collection("employees").document(conge.getUserId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot employeeDoc = task.getResult();
                        Integer soldeInitial = employeeDoc.getLong("soldeConge") != null ?
                                employeeDoc.getLong("soldeConge").intValue() : 30;

                        calculerSoldeActuelReel(soldeInitial);
                    } else {
                        // Valeur par défaut si l'employé n'est pas trouvé
                        if (soldeCongesEmploye != null) {
                            soldeCongesEmploye.setText("30 jours");
                        }
                    }
                });
    }

    private void calculerSoldeActuelReel(int soldeInitial) {
        if (conge == null || conge.getUserId() == null) return;

        db.collection("conges")
                .whereEqualTo("userId", conge.getUserId())
                .whereEqualTo("statut", "Approuvé")
                .get()
                .addOnCompleteListener(task -> {
                    int totalJoursPris = 0;

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            // Ne pas compter le congé actuel s'il est en attente
                            if (!doc.getId().equals(conge.getId()) || !conge.getStatut().equals("En attente")) {
                                Integer duree = doc.getLong("duree") != null ? doc.getLong("duree").intValue() : 0;
                                totalJoursPris += duree;
                            }
                        }
                    }

                    int soldeActuelReel = soldeInitial - totalJoursPris;
                    if (soldeCongesEmploye != null) {
                        soldeCongesEmploye.setText(soldeActuelReel + " jours");
                    }
                })
                .addOnFailureListener(e -> {
                    if (soldeCongesEmploye != null) {
                        soldeCongesEmploye.setText(soldeInitial + " jours");
                    }
                });
    }

    private void updateUIWithCongeDetails() {
        if (getView() == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM. yyyy", Locale.FRENCH);

        // Mettre à jour l'UI avec les données réelles
        String details = (conge.getUserName() != null ? conge.getUserName() : "Nom non disponible") +
                " - " +
                (conge.getUserDepartment() != null ? conge.getUserDepartment() : "Département non disponible");

        if (sousDetails != null) sousDetails.setText(details);
        if (typeConge != null) typeConge.setText(conge.getTypeConge() != null ? conge.getTypeConge() : "Type non disponible");

        String dateDebut = conge.getDateDebut() != null ? dateFormat.format(conge.getDateDebut()) : "Date non disponible";
        String dateFin = conge.getDateFin() != null ? dateFormat.format(conge.getDateFin()) : "Date non disponible";
        String dates = dateDebut + " - " + dateFin;
        if (datesConge != null) datesConge.setText(dates);

        if (motifConge != null) motifConge.setText(conge.getMotif() != null ? conge.getMotif() : "Motif non spécifié");
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void approuverConge() {
        updateCongeStatus("Approuvé", "");
    }

    private void refuserConge() {
        String raison = "";
        if (raisonRefus != null) {
            raison = raisonRefus.getText().toString().trim();
        }
        if (raison.isEmpty()) {
            raison = "Raison non spécifiée";
        }
        updateCongeStatus("Refusé", raison);
    }

    private void updateCongeStatus(String nouveauStatut, String raison) {
        if (congeId == null || congeId.isEmpty()) {
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
                        Toast.makeText(getContext(), "Erreur lors de la mise à jour: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Erreur inconnue"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}