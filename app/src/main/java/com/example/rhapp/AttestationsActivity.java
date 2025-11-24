package com.example.rhapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
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
        btnAttente = findViewById(R.id.btnCongeAttente);
        btnApprouve = findViewById(R.id.btnCongeApprouve);
        btnRefuse = findViewById(R.id.btnCongeRefuse);
        containerAttestations = findViewById(R.id.containerAttestations);

        tvTotal = findViewById(R.id.tvTotal);
        tvAttente = findViewById(R.id.tvAttente);
        tvApprouve = findViewById(R.id.tvApprouve);
        tvRefuse = findViewById(R.id.tvRefuse);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void loadStats() {
        db.collection("Attestations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ATTESTATIONS_RH", "Erreur stats: ", e);
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
        Log.d("ATTESTATIONS_RH", "Chargement attestations avec statut: " + statut);

        // SOLUTION : Enlever le orderBy pour éviter l'index, on triera manuellement
        db.collection("Attestations")
                .whereEqualTo("statut", statut)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attestation> attestations = new ArrayList<>();
                    Log.d("ATTESTATIONS_RH", "Nombre de documents trouvés: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Attestation attestation = new Attestation();
                            attestation.setId(document.getId());

                            // Mapping manuel pour éviter les problèmes de conversion
                            attestation.setEmployeId(document.getString("employeeId"));
                            attestation.setEmployeNom(document.getString("employeeNom"));
                            attestation.setEmployeDepartement(document.getString("employeeDepartment"));
                            attestation.setTypeAttestation(document.getString("typeAttestation"));
                            attestation.setMotif(document.getString("motif"));
                            attestation.setStatut(document.getString("statut"));

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
                            Log.d("ATTESTATIONS_RH", "Attestation chargée: " + attestation.getEmployeNom());

                        } catch (Exception e) {
                            Log.e("ATTESTATIONS_RH", "Erreur conversion document: ", e);
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ATTESTATIONS_RH", "Erreur Firestore: ", e);
                });
    }

    private void afficherAttestations(List<Attestation> attestations) {
        runOnUiThread(() -> {
            if (containerAttestations == null) return;

            containerAttestations.removeAllViews();

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
                View itemView = getLayoutInflater().inflate(
                        R.layout.item_attestations_card,
                        containerAttestations,
                        false
                );

                configureAdminItemView(itemView, attestation);
                containerAttestations.addView(itemView);
            }
        });
    }

    private String getStatutText(String statut) {
        switch (statut) {
            case "en_attente": return "en attente";
            case "approuvee": return "approuvée";
            case "refusee": return "refusée";
            default: return "";
        }
    }

    private void configureAdminItemView(View itemView, Attestation attestation) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

        // Récupérer les vues - CORRECTION DES IDS
        TextView nomComplet = itemView.findViewById(R.id.nomComplet);
        TextView departement = itemView.findViewById(R.id.departement);
        TextView typeAttestation = itemView.findViewById(R.id.TypeConge);
        TextView dateDemandee = itemView.findViewById(R.id.DateDemandee);
        TextView motif = itemView.findViewById(R.id.MotifAttestation);
        TextView statutView = itemView.findViewById(R.id.StatutAttestation);

        Button btnDetails = itemView.findViewById(R.id.btnDetails);
        Button btnValidate = itemView.findViewById(R.id.btnValidate);
        Button btnReject = itemView.findViewById(R.id.btnReject);

        // Remplir les données
        if (nomComplet != null) {
            nomComplet.setText(attestation.getEmployeNom() != null ? attestation.getEmployeNom() : "Nom non spécifié");
        }

        if (departement != null) {
            departement.setText(attestation.getEmployeDepartement() != null ? attestation.getEmployeDepartement() : "Département non spécifié");
        }

        if (typeAttestation != null) {
            typeAttestation.setText(attestation.getTypeAttestation() != null ? attestation.getTypeAttestation() : "Type non spécifié");
        }

        if (dateDemandee != null && attestation.getDateDemande() != null) {
            String dateStr = dateFormat.format(attestation.getDateDemande());
            dateDemandee.setText("Demandée le " + dateStr);
        }

        if (motif != null) {
            String motifText = attestation.getMotif();
            motif.setText(motifText != null && !motifText.isEmpty() ?
                    "Motif : " + motifText : "Aucun motif spécifié");
        }

        if (statutView != null) {
            statutView.setText(getStatutDisplayText(attestation.getStatut()));
        }

        // Configurer les boutons - seulement pour les attestations en attente
        if ("en_attente".equals(attestation.getStatut())) {
            if (btnValidate != null) {
                btnValidate.setVisibility(View.VISIBLE);
                btnValidate.setOnClickListener(v -> validerAttestation(attestation));
            }
            if (btnReject != null) {
                btnReject.setVisibility(View.VISIBLE);
                btnReject.setOnClickListener(v -> refuserAttestation(attestation));
            }
        } else {
            // Cacher les boutons pour les attestations déjà traitées
            if (btnValidate != null) btnValidate.setVisibility(View.GONE);
            if (btnReject != null) btnReject.setVisibility(View.GONE);
        }

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> voirDetails(attestation));
        }
    }

    private String getStatutDisplayText(String statut) {
        switch (statut) {
            case "en_attente": return "En attente";
            case "approuvee": return "Approuvée";
            case "refusee": return "Refusée";
            default: return statut;
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

    private void refuserAttestation(Attestation attestation) {
        // Pour l'instant, refus sans motif. Vous pouvez ajouter un dialog pour saisir le motif
        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "refusee",
                        "dateTraitement", new Date(),
                        "motifRefus", "Refusé par le service RH"
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attestation refusée", Toast.LENGTH_SHORT).show();
                    rechargerDonnees();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ATTESTATIONS_RH", "Erreur refus: ", e);
                });
    }

    private void voirDetails(Attestation attestation) {
        // Dialog avec détails complets
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
        button.setBackgroundResource(R.drawable.border_gris);
        button.setTextColor(getResources().getColor(R.color.black));
    }

    private void setActiveButtonStyle(Button button) {
        button.setBackgroundResource(R.drawable.border_button);
        button.setTextColor(getResources().getColor(R.color.white));
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