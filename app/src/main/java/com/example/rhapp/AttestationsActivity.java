package com.example.rhapp;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttestationsActivity extends AppCompatActivity {

    private Button btnAttente, btnApprouve, btnRefuse;
    private LinearLayout containerAttestations;
    private TextView tvTotal, tvAttente, tvApprouve, tvRefuse;

    // AJOUT: Écouteurs temps réel
    private ListenerRegistration attestationsListener;
    private ListenerRegistration statsListener;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseFirestore db;
    private String filtreActuel = "en_attente";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attestations);

        initViews();
        setupFirebase();
        loadStats();
        chargerAttestations(filtreActuel);
        setupClickListeners();
        updateButtonStyles();
    }

    private void initViews() {
        btnAttente = findViewById(R.id.btnAttente);
        btnApprouve = findViewById(R.id.btnApprouve);
        btnRefuse = findViewById(R.id.btnRefuse);
        containerAttestations = findViewById(R.id.containerAttestations);

        tvTotal = findViewById(R.id.tvTotal);
        tvAttente = findViewById(R.id.tvAttente);
        tvApprouve = findViewById(R.id.tvApprouve);
        tvRefuse = findViewById(R.id.tvRefuse);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

// Configurer le swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            rechargerDonnees();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les écouteurs
        if (attestationsListener != null) {
            attestationsListener.remove();
        }
        if (statsListener != null) {
            statsListener.remove();
        }
    }



    private void loadStats() {
        // Supprimer l'écouteur précédent s'il existe
        if (statsListener != null) {
            statsListener.remove();
        }

        statsListener = db.collection("Attestations")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Erreur chargement stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ATTESTATIONS_RH", "Erreur stats: ", e);
                        return;
                    }

                    int total = 0, enAttente = 0, approuvees = 0, refusees = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        total++;
                        String statut = document.getString("statut");

                        if (statut != null) {
                            switch (statut) {
                                case "en_attente":
                                    enAttente++;
                                    break;
                                case "approuvee":
                                    approuvees++;
                                    break;
                                case "refusee":
                                    refusees++;
                                    break;
                            }
                        }
                    }

                    updateStatsUI(total, enAttente, approuvees, refusees);
                });
    }

    private void updateStatsUI(int total, int enAttente, int approuvees, int refusees) {
        runOnUiThread(() -> {
            if (tvTotal != null) tvTotal.setText(String.valueOf(total));
            if (tvAttente != null) tvAttente.setText(String.valueOf(enAttente));
            if (tvApprouve != null) tvApprouve.setText(String.valueOf(approuvees));
            if (tvRefuse != null) tvRefuse.setText(String.valueOf(refusees));
        });
    }
    private void chargerAttestations(String statut) {
        Log.d("ATTESTATIONS_RH", "Chargement TEMPS RÉEL attestations avec statut: " + statut);

        // Supprimer l'écouteur précédent s'il existe
        if (attestationsListener != null) {
            attestationsListener.remove();
        }

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("statut", statut)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ATTESTATIONS_RH", "Erreur Firestore: ", e);
                        return;
                    }

                    List<Attestation> attestations = new ArrayList<>();
                    Log.d("ATTESTATIONS_RH", "Nombre de documents trouvés (temps réel): " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // RÉCUPÉRATION CORRECTE DES DONNÉES DEPUIS FIRESTORE
                            Attestation attestation = new Attestation();
                            attestation.setId(document.getId());

                            // Données de base de l'attestation
                            attestation.setEmployeId(document.getString("employeeId"));
                            attestation.setTypeAttestation(document.getString("typeAttestation"));
                            attestation.setMotif(document.getString("motif"));
                            attestation.setStatut(document.getString("statut"));

                            // Récupération des données de l'employé
                            String employeeNom = document.getString("employeeNom");
                            String employeeDepartment = document.getString("employeeDepartment");

                            if (employeeNom != null && !employeeNom.isEmpty()) {
                                attestation.setEmployeNom(employeeNom);
                            } else {
                                attestation.setEmployeNom("Nom non disponible");
                            }

                            if (employeeDepartment != null && !employeeDepartment.isEmpty()) {
                                attestation.setEmployeDepartement(employeeDepartment);
                            } else {
                                attestation.setEmployeDepartement("Département non disponible");
                            }

                            // Gestion des dates
                            Timestamp dateDemande = document.getTimestamp("dateDemande");
                            if (dateDemande != null) {
                                attestation.setDateDemande(dateDemande.toDate());
                            }

                            Timestamp dateTraitement = document.getTimestamp("dateTraitement");
                            if (dateTraitement != null) {
                                attestation.setDateTraitement(dateTraitement.toDate());
                            }

                            attestation.setPdfUrl(document.getString("pdfUrl"));
                            attestation.setMotifRefus(document.getString("motifRefus"));

                            attestations.add(attestation);

                        } catch (Exception ex) {
                            Log.e("ATTESTATIONS_RH", "Erreur conversion document: ", ex);
                        }
                    }

                    // TRI MANUEL par date de demande (plus récent en premier)
                    Collections.sort(attestations, new Comparator<Attestation>() {
                        @Override
                        public int compare(Attestation a1, Attestation a2) {
                            Date date1 = a1.getDateDemande();
                            Date date2 = a2.getDateDemande();

                            if (date1 == null && date2 == null) return 0;
                            if (date1 == null) return 1;
                            if (date2 == null) return -1;

                            return date2.compareTo(date1); // Ordre décroissant
                        }
                    });

                    afficherAttestations(attestations);
                });
    }

    private void showMotifRefusDialog(Attestation attestation) {
        // Créer un dialog personnalisé
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Motif du refus");
        builder.setMessage("Veuillez saisir le motif du refus :");

        // Créer l'input
        final EditText input = new EditText(this);
        input.setHint("Saisissez le motif du refus...");
        input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setMaxLines(5);
        input.setGravity(Gravity.START);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(50, 20, 50, 20);
        input.setLayoutParams(layoutParams);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);

        builder.setView(container);

        // Boutons du dialog
        builder.setPositiveButton("Confirmer le refus", (dialog, which) -> {
            String motifRefus = input.getText().toString().trim();
            if (motifRefus.isEmpty()) {
                Toast.makeText(this, "Veuillez saisir un motif de refus", Toast.LENGTH_SHORT).show();
            } else {
                refuserAttestation(attestation, motifRefus);
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> {
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Personnaliser le bouton positif
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.RED);
        }
    }
    private void afficherAttestations(List<Attestation> attestations) {
        runOnUiThread(() -> {
            if (containerAttestations == null) return;

            containerAttestations.removeAllViews();

            Log.d("ATTESTATIONS_RH", "Nombre d'attestations à afficher: " + attestations.size());

            if (attestations.isEmpty()) {
                TextView emptyText = new TextView(this);
                emptyText.setText("Aucune attestation " + getStatutText(filtreActuel));
                emptyText.setTextSize(16);
                emptyText.setPadding(50, 50, 50, 50);
                emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                containerAttestations.addView(emptyText);
                return;
            }

            for (Attestation attestation : attestations) {
                Log.d("ATTESTATIONS_RH", "Création vue pour: " + attestation.getEmployeNom() + " - " + attestation.getStatut());
                View itemView = getLayoutForStatut(attestation.getStatut());
                configureItemView(itemView, attestation);
                containerAttestations.addView(itemView);
            }
        });
    }

    private View getLayoutForStatut(String statut) {
        int layoutRes;
        switch (statut) {
            case "approuvee":
                layoutRes = R.layout.item_attestation_approuvee;
                break;
            case "refusee":
                layoutRes = R.layout.item_attestation_refuse;
                break;
            case "en_attente":
            default:
                layoutRes = R.layout.item_attestations_card;
                break;
        }
        return getLayoutInflater().inflate(layoutRes, containerAttestations, false);
    }

    private void configureItemView(View itemView, Attestation attestation) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
        String statut = attestation.getStatut();

        // Récupérer les vues communes avec différents IDs possibles
        TextView nomComplet = itemView.findViewById(R.id.nomComplet);
        TextView departement = itemView.findViewById(R.id.departement);

        // IDs différents selon les layouts
        TextView typeAttestation = itemView.findViewById(
                itemView.findViewById(R.id.TypeAttestation) != null ?
                        R.id.TypeAttestation : R.id.TypeAttestation
        );

        // CORRECTION : Utiliser le même ID pour tous les layouts
        TextView dateDemandee = itemView.findViewById(R.id.DateDemandee);
        TextView motif = itemView.findViewById(R.id.MotifAttestation);

        // Remplir les données communes
        if (nomComplet != null) {
            String nomEmploye = attestation.getEmployeNom();
            if (nomEmploye != null && !nomEmploye.isEmpty()) {
                nomComplet.setText(nomEmploye);
            } else {
                nomComplet.setText("Employé inconnu");
            }
        }

        if (departement != null) {
            String deptEmploye = attestation.getEmployeDepartement();
            if (deptEmploye != null && !deptEmploye.isEmpty()) {
                departement.setText(deptEmploye);
            } else {
                departement.setText("Département inconnu");
            }
        }

        if (typeAttestation != null) {
            String type = attestation.getTypeAttestation();
            if (type != null && !type.isEmpty()) {
                typeAttestation.setText(type);
            } else {
                typeAttestation.setText("Type non spécifié");
            }
        }

        // CORRECTION : Afficher la date pour TOUS les statuts
        if (dateDemandee != null) {
            if (attestation.getDateDemande() != null) {
                String dateStr = dateFormat.format(attestation.getDateDemande());
                dateDemandee.setText("Demandée le " + dateStr);
            } else {
                dateDemandee.setText("Date inconnue");
            }
        }

        if (motif != null) {
            String motifText = attestation.getMotif();
            if (motifText != null && !motifText.isEmpty()) {
                motif.setText("Motif : " + motifText);
            } else {
                motif.setText("Aucun motif spécifié");
            }
        }

        // Configurer selon le statut
        if ("en_attente".equals(statut)) {
            configureEnAttenteView(itemView, attestation, dateFormat);
        } else if ("approuvee".equals(statut)) {
            configureApprouveeView(itemView, attestation, dateFormat);
        } else if ("refusee".equals(statut)) {
            configureRefuseeView(itemView, attestation, dateFormat);
        }
    }
    private void configureEnAttenteView(View itemView, Attestation attestation, SimpleDateFormat dateFormat) {
        TextView statutView = itemView.findViewById(R.id.StatutAttestation);

        Button btnDetails = itemView.findViewById(R.id.btnDetails);
        Button btnValidate = itemView.findViewById(R.id.btnValidate);
        Button btnReject = itemView.findViewById(R.id.btnReject);

        if (statutView != null) {
            statutView.setText("En attente");
        }

        // Configurer les boutons pour les attestations en attente
        if (btnValidate != null) {
            btnValidate.setOnClickListener(v -> validerAttestation(attestation));
        }

        if (btnReject != null) {
            // MODIFICATION ICI : Ouvrir le dialog pour saisir le motif
            btnReject.setOnClickListener(v -> showMotifRefusDialog(attestation));
        }

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> voirDetails(attestation));
        }
    }
    private void configureApprouveeView(View itemView, Attestation attestation, SimpleDateFormat dateFormat) {
        TextView dateApprouvee = itemView.findViewById(R.id.DateApprouvé);

        if (dateApprouvee != null && attestation.getDateTraitement() != null) {
            String dateStr = dateFormat.format(attestation.getDateTraitement());
            dateApprouvee.setText("Approuvée le " + dateStr);
        }

        Button btnTelecharger = itemView.findViewById(R.id.btnTelecharger);
        if (btnTelecharger != null) {
            btnTelecharger.setOnClickListener(v -> telechargerAttestation(attestation));
        }
    }

    private void configureRefuseeView(View itemView, Attestation attestation, SimpleDateFormat dateFormat) {
        TextView motifRefusView = itemView.findViewById(R.id.motifRefus);

        if (motifRefusView != null) {
            String motifRefus = attestation.getMotifRefus();
            if (motifRefus != null && !motifRefus.isEmpty()) {
                motifRefusView.setText("Refusée - " + motifRefus);
            } else {
                motifRefusView.setText("Refusée - Motif non spécifié");
            }
        }

        // Afficher aussi la date de traitement si disponible
        TextView dateTraitementView = itemView.findViewById(R.id.DateDemandee);
        if (dateTraitementView != null && attestation.getDateTraitement() != null) {
            String dateStr = dateFormat.format(attestation.getDateTraitement());
            dateTraitementView.setText("Refusée le " + dateStr);
        }
    }
    private void telechargerAttestation(Attestation attestation) {
        if (attestation.getPdfUrl() != null && !attestation.getPdfUrl().isEmpty()) {
            Toast.makeText(this, "Téléchargement du PDF pour " + attestation.getEmployeNom(), Toast.LENGTH_SHORT).show();
            // Implémenter le téléchargement ici
        } else {
            Toast.makeText(this, "PDF non disponible pour " + attestation.getEmployeNom(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getStatutText(String statut) {
        switch (statut) {
            case "en_attente": return "en attente";
            case "approuvee": return "approuvée";
            case "refusee": return "refusée";
            default: return "";
        }
    }

    private void validerAttestation(Attestation attestation) {
        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "approuvee",
                        "dateTraitement", new Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attestation approuvée avec succès", Toast.LENGTH_SHORT).show();
                    rechargerDonnees();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ATTESTATIONS_RH", "Erreur validation: ", e);
                });
    }

    private void refuserAttestation(Attestation attestation, String motifRefus) {
        // Vérifier que le motif n'est pas vide
        if (motifRefus == null || motifRefus.trim().isEmpty()) {
            Toast.makeText(this, "Le motif de refus ne peut pas être vide", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "refusee",
                        "dateTraitement", new Date(),
                        "motifRefus", motifRefus.trim()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attestation refusée", Toast.LENGTH_SHORT).show();
                    // Les données se mettront à jour automatiquement grâce à l'écouteur temps réel
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ATTESTATIONS_RH", "Erreur refus: ", e);
                });
    }
    private void voirDetails(Attestation attestation) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);

        String details = "Nom: " + attestation.getEmployeNom() + "\n" +
                "Département: " + attestation.getEmployeDepartement() + "\n" +
                "Type: " + attestation.getTypeAttestation() + "\n" +
                "Motif: " + (attestation.getMotif() != null ? attestation.getMotif() : "Aucun") + "\n" +
                "Statut: " + getStatutDisplayText(attestation.getStatut()) + "\n" +
                "Demandée le: " + (attestation.getDateDemande() != null ?
                dateFormat.format(attestation.getDateDemande()) : "Date inconnue");

        if (attestation.getDateTraitement() != null) {
            details += "\nTraité le: " + dateFormat.format(attestation.getDateTraitement());
        }

        if ("refusee".equals(attestation.getStatut()) && attestation.getMotifRefus() != null) {
            details += "\nMotif du refus: " + attestation.getMotifRefus();
        }

        Toast.makeText(this, details, Toast.LENGTH_LONG).show();
    }

    private String getStatutDisplayText(String statut) {
        switch (statut) {
            case "en_attente": return "En attente";
            case "approuvee": return "Approuvée";
            case "refusee": return "Refusée";
            default: return statut;
        }
    }

    private void setupClickListeners() {
        btnAttente.setOnClickListener(v -> {
            filtreActuel = "en_attente";
            chargerAttestations(filtreActuel);
            updateButtonStyles();
        });

        btnApprouve.setOnClickListener(v -> {
            filtreActuel = "approuvee";
            chargerAttestations(filtreActuel);
            updateButtonStyles();
        });

        btnRefuse.setOnClickListener(v -> {
            filtreActuel = "refusee";
            chargerAttestations(filtreActuel);
            updateButtonStyles();
        });
    }

    private void updateButtonStyles() {
        resetButtonStyle(btnAttente);
        resetButtonStyle(btnApprouve);
        resetButtonStyle(btnRefuse);

        switch (filtreActuel) {
            case "en_attente":
                setActiveButtonStyle(btnAttente);
                break;
            case "approuvee":
                setActiveButtonStyle(btnApprouve);
                break;
            case "refusee":
                setActiveButtonStyle(btnRefuse);
                break;
        }
    }

   private void resetButtonStyle(Button button) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DEDEDE")));
    }

    private void setActiveButtonStyle(Button button) {
        button.setBackgroundResource(R.drawable.border_gris);  // même fond que XML
        button.setBackgroundTintList(null);
    }

    private void rechargerDonnees() {
        loadStats();
        chargerAttestations(filtreActuel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rechargerDonnees();
    }
}