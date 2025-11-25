package com.example.rhapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
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

        db.collection("Attestations")
                .whereEqualTo("statut", statut)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attestation> attestations = new ArrayList<>();
                    Log.d("ATTESTATIONS_RH", "Nombre de documents trouvés: " + queryDocumentSnapshots.size());

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

                            // Récupération des données de l'employé depuis les champs de l'attestation
                            String employeeNom = document.getString("employeeNom");
                            String employeeDepartment = document.getString("employeeDepartment");

                            // Si les champs employeeNom et employeeDepartment existent, les utiliser
                            if (employeeNom != null && !employeeNom.isEmpty()) {
                                attestation.setEmployeNom(employeeNom);
                            } else {
                                // Sinon, essayer de récupérer depuis la collection employees
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

                            // DEBUG: Afficher les données récupérées
                            Log.d("ATTESTATIONS_RH", "=== DONNÉES RÉCUPÉRÉES ===");
                            Log.d("ATTESTATIONS_RH", "ID: " + document.getId());
                            Log.d("ATTESTATIONS_RH", "employeeNom: " + employeeNom);
                            Log.d("ATTESTATIONS_RH", "employeeDepartment: " + employeeDepartment);
                            Log.d("ATTESTATIONS_RH", "typeAttestation: " + attestation.getTypeAttestation());
                            Log.d("ATTESTATIONS_RH", "motif: " + attestation.getMotif());
                            Log.d("ATTESTATIONS_RH", "statut: " + attestation.getStatut());

                            attestations.add(attestation);

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

        // DEBUG: Vérifier les données avant affichage
        Log.d("ATTESTATIONS_RH", "=== CONFIGURATION CARD ===");
        Log.d("ATTESTATIONS_RH", "Nom: " + attestation.getEmployeNom());
        Log.d("ATTESTATIONS_RH", "Département: " + attestation.getEmployeDepartement());
        Log.d("ATTESTATIONS_RH", "Type: " + attestation.getTypeAttestation());
        Log.d("ATTESTATIONS_RH", "Motif: " + attestation.getMotif());

        // Récupérer les vues communes avec différents IDs possibles
        TextView nomComplet = itemView.findViewById(R.id.nomComplet);
        TextView departement = itemView.findViewById(R.id.departement);

        // IDs différents selon les layouts
        TextView typeAttestation = itemView.findViewById(
                itemView.findViewById(R.id.TypeAttestation) != null ?
                        R.id.TypeAttestation : R.id.TypeConge
        );

        TextView dateDemandee = itemView.findViewById(R.id.DateDemandee);
        TextView motif = itemView.findViewById(R.id.MotifAttestation);

        // Remplir les données communes
        if (nomComplet != null) {
            String nomEmploye = attestation.getEmployeNom();
            if (nomEmploye != null && !nomEmploye.isEmpty()) {
                nomComplet.setText(nomEmploye);
            } else {
                nomComplet.setText("Employé inconnu");
                Log.w("ATTESTATIONS_RH", "Nom employé manquant pour l'attestation: " + attestation.getId());
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

        if (dateDemandee != null) {
            if (attestation.getDateDemande() != null) {
                String dateStr = dateFormat.format(attestation.getDateDemande());
                if ("en_attente".equals(statut)) {
                    dateDemandee.setText("Demandée le " + dateStr);
                }
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
            btnReject.setOnClickListener(v -> refuserAttestation(attestation));
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

    private void refuserAttestation(Attestation attestation) {
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