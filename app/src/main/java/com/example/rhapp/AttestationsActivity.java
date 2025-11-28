package com.example.rhapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rhapp.model.Attestation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttestationsActivity extends AppCompatActivity {

    private Button btnAttente, btnApprouve, btnRefuse;
    private LinearLayout containerAttestations;
    private ProgressDialog progressDialog;
    private TextView tvTotal, tvAttente, tvApprouve, tvRefuse;

    // Écouteurs temps réel
    private ListenerRegistration attestationsListener;
    private ListenerRegistration statsListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String filtreActuel = "en_attente";

    // Executor pour les opérations en arrière-plan
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

        swipeRefreshLayout.setOnRefreshListener(() -> {
            rechargerDonnees();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer les écouteurs et l'executor
        if (attestationsListener != null) {
            attestationsListener.remove();
        }
        if (statsListener != null) {
            statsListener.remove();
        }
        backgroundExecutor.shutdown();
    }

    private void loadStats() {
        if (statsListener != null) {
            statsListener.remove();
        }

        statsListener = db.collection("Attestations")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showErrorOnMainThread("Erreur chargement stats: " + e.getMessage());
                        Log.e("ATTESTATIONS_RH", "Erreur stats: ", e);
                        return;
                    }

                    // Traitement des stats en arrière-plan
                    backgroundExecutor.execute(() -> {
                        try {
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
                        } catch (Exception ex) {
                            Log.e("ATTESTATIONS_RH", "Erreur calcul stats: ", ex);
                        }
                    });
                });
    }

    private void updateStatsUI(int total, int enAttente, int approuvees, int refusees) {
        mainHandler.post(() -> {
            if (tvTotal != null) tvTotal.setText(String.valueOf(total));
            if (tvAttente != null) tvAttente.setText(String.valueOf(enAttente));
            if (tvApprouve != null) tvApprouve.setText(String.valueOf(approuvees));
            if (tvRefuse != null) tvRefuse.setText(String.valueOf(refusees));
        });
    }

    private void chargerAttestations(String statut) {
        Log.d("ATTESTATIONS_RH", "Chargement TEMPS RÉEL attestations avec statut: " + statut);

        if (attestationsListener != null) {
            attestationsListener.remove();
        }

        attestationsListener = db.collection("Attestations")
                .whereEqualTo("statut", statut)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        showErrorOnMainThread("Erreur chargement: " + e.getMessage());
                        Log.e("ATTESTATIONS_RH", "Erreur Firestore: ", e);
                        return;
                    }

                    // Traitement des données en arrière-plan
                    backgroundExecutor.execute(() -> {
                        try {
                            List<Attestation> attestations = processAttestationsData(queryDocumentSnapshots);
                            afficherAttestations(attestations);
                        } catch (Exception ex) {
                            Log.e("ATTESTATIONS_RH", "Erreur traitement attestations: ", ex);
                        }
                    });
                });
    }

    private List<Attestation> processAttestationsData(Iterable<QueryDocumentSnapshot> queryDocumentSnapshots) {
        List<Attestation> attestations = new ArrayList<>();
        Log.d("ATTESTATIONS_RH", "Traitement des attestations en arrière-plan");

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            try {
                Attestation attestation = createAttestationFromDocument(document);
                if (attestation != null) {
                    attestations.add(attestation);
                }
            } catch (Exception ex) {
                Log.e("ATTESTATIONS_RH", "Erreur conversion document: ", ex);
            }
        }

        // Tri par date de demande (plus récent en premier)
        Collections.sort(attestations, new Comparator<Attestation>() {
            @Override
            public int compare(Attestation a1, Attestation a2) {
                Date date1 = a1.getDateDemande();
                Date date2 = a2.getDateDemande();

                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1;
                if (date2 == null) return -1;

                return date2.compareTo(date1);
            }
        });

        return attestations;
    }

    private Attestation createAttestationFromDocument(QueryDocumentSnapshot document) {
        Attestation attestation = new Attestation();
        attestation.setId(document.getId());

        // Données de base
        attestation.setEmployeId(document.getString("employeeId"));
        attestation.setTypeAttestation(document.getString("typeAttestation"));
        attestation.setMotif(document.getString("motif"));
        attestation.setStatut(document.getString("statut"));

        // Données employé
        String employeeNom = document.getString("employeeNom");
        String employeeDepartment = document.getString("employeeDepartment");

        attestation.setEmployeNom(employeeNom != null && !employeeNom.isEmpty() ?
                employeeNom : "Nom non disponible");
        attestation.setEmployeDepartement(employeeDepartment != null && !employeeDepartment.isEmpty() ?
                employeeDepartment : "Département non disponible");

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

        return attestation;
    }

    private void showMotifRefusDialog(Attestation attestation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Motif du refus");
        builder.setMessage("Veuillez saisir le motif du refus :");

        final EditText input = createMotifRefusInput();
        LinearLayout container = createDialogContainer(input);

        builder.setView(container);

        builder.setPositiveButton("Confirmer le refus", (dialog, which) -> {
            String motifRefus = input.getText().toString().trim();
            if (motifRefus.isEmpty()) {
                showErrorOnMainThread("Veuillez saisir un motif de refus");
            } else {
                refuserAttestation(attestation, motifRefus);
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        customizeDialogButton(dialog, AlertDialog.BUTTON_POSITIVE, Color.RED);
    }

    private EditText createMotifRefusInput() {
        EditText input = new EditText(this);
        input.setHint("Saisissez le motif du refus...");
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setMaxLines(5);
        input.setGravity(Gravity.START);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(50, 20, 50, 20);
        input.setLayoutParams(layoutParams);

        return input;
    }

    private LinearLayout createDialogContainer(EditText input) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        container.setBackgroundColor(Color.WHITE);
        return container;
    }

    private void customizeDialogButton(AlertDialog dialog, int buttonId, int color) {
        Button button = dialog.getButton(buttonId);
        if (button != null) {
            button.setTextColor(color);
        }
    }

    private void afficherAttestations(List<Attestation> attestations) {
        mainHandler.post(() -> {
            if (containerAttestations == null) return;

            containerAttestations.removeAllViews();
            Log.d("ATTESTATIONS_RH", "Affichage de " + attestations.size() + " attestations");

            if (attestations.isEmpty()) {
                showEmptyState();
                return;
            }

            for (Attestation attestation : attestations) {
                View itemView = createAttestationItemView(attestation);
                containerAttestations.addView(itemView);
            }
        });
    }

    private void showEmptyState() {
        TextView emptyText = new TextView(this);
        emptyText.setText("Aucune attestation " + getStatutText(filtreActuel));
        emptyText.setTextSize(16);
        emptyText.setPadding(50, 50, 50, 50);
        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        containerAttestations.addView(emptyText);
    }

    private View createAttestationItemView(Attestation attestation) {
        View itemView = getLayoutForStatut(attestation.getStatut());
        configureItemView(itemView, attestation);
        return itemView;
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

        // Récupérer et configurer les vues
        setupCommonViews(itemView, attestation, dateFormat);

        // Configurer selon le statut
        switch (statut) {
            case "en_attente":
                configureEnAttenteView(itemView, attestation, dateFormat);
                break;
            case "approuvee":
                configureApprouveeView(itemView, attestation, dateFormat);
                break;
            case "refusee":
                configureRefuseeView(itemView, attestation, dateFormat);
                break;
        }
    }

    private void setupCommonViews(View itemView, Attestation attestation, SimpleDateFormat dateFormat) {
        TextView nomComplet = itemView.findViewById(R.id.nomComplet);
        TextView departement = itemView.findViewById(R.id.departement);
        TextView typeAttestation = itemView.findViewById(R.id.TypeAttestation);
        TextView dateDemandee = itemView.findViewById(R.id.DateDemandee);
        TextView motif = itemView.findViewById(R.id.MotifAttestation);

        if (nomComplet != null) {
            nomComplet.setText(attestation.getEmployeNom() != null ?
                    attestation.getEmployeNom() : "Employé inconnu");
        }

        if (departement != null) {
            departement.setText(attestation.getEmployeDepartement() != null ?
                    attestation.getEmployeDepartement() : "Département inconnu");
        }

        if (typeAttestation != null) {
            typeAttestation.setText(attestation.getTypeAttestation() != null ?
                    attestation.getTypeAttestation() : "Type non spécifié");
        }

        if (dateDemandee != null && attestation.getDateDemande() != null) {
            dateDemandee.setText("Demandée le " + dateFormat.format(attestation.getDateDemande()));
        }

        if (motif != null) {
            String motifText = attestation.getMotif();
            motif.setText("Motif : " + (motifText != null ? motifText : "Aucun motif spécifié"));
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

        if (btnValidate != null) {
            btnValidate.setOnClickListener(v -> validerAttestation(attestation));
        }

        if (btnReject != null) {
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

        // MODIFICATION : Afficher "PDF non disponible" au clic
        if (btnTelecharger != null) {
            btnTelecharger.setOnClickListener(v -> {
                Toast.makeText(this, "PDF non disponible", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void configureRefuseeView(View itemView, Attestation attestation, SimpleDateFormat dateFormat) {
        TextView motifRefusView = itemView.findViewById(R.id.motifRefus);

        if (motifRefusView != null) {
            String motifRefus = attestation.getMotifRefus();
            motifRefusView.setText("Refusée - " + (motifRefus != null ? motifRefus : "Motif non spécifié"));
        }

        TextView dateTraitementView = itemView.findViewById(R.id.DateDemandee);
        if (dateTraitementView != null && attestation.getDateTraitement() != null) {
            dateTraitementView.setText("Refusée le " + dateFormat.format(attestation.getDateTraitement()));
        }
    }

    private void validerAttestation(Attestation attestation) {
        db.collection("Attestations")
                .document(attestation.getId())
                .update(
                        "statut", "approuvee",
                        "dateTraitement", new Date()
                )
                .addOnFailureListener(e -> {
                    showErrorOnMainThread("Erreur validation: " + e.getMessage());
                    Log.e("ATTESTATIONS_RH", "Erreur validation: ", e);
                });
    }

    private void refuserAttestation(Attestation attestation, String motifRefus) {
        if (motifRefus == null || motifRefus.trim().isEmpty()) {
            showErrorOnMainThread("Le motif de refus ne peut pas être vide");
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
                    showSuccessOnMainThread("Attestation refusée");
                })
                .addOnFailureListener(e -> {
                    showErrorOnMainThread("Erreur: " + e.getMessage());
                    Log.e("ATTESTATIONS_RH", "Erreur refus: ", e);
                });
    }

    private void voirDetails(Attestation attestation) {
        backgroundExecutor.execute(() -> {
            try {
                String details = buildDetailsString(attestation);
                mainHandler.post(() -> showDetailsDialog(details));
            } catch (Exception e) {
                Log.e("ATTESTATIONS_RH", "Erreur construction détails: ", e);
            }
        });
    }

    private String buildDetailsString(Attestation attestation) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);
        StringBuilder details = new StringBuilder();

        details.append("Nom: ").append(attestation.getEmployeNom()).append("\n")
                .append("Département: ").append(attestation.getEmployeDepartement()).append("\n")
                .append("Type: ").append(attestation.getTypeAttestation()).append("\n")
                .append("Motif: ").append(attestation.getMotif() != null ? attestation.getMotif() : "Aucun").append("\n")
                .append("Statut: ").append(getStatutDisplayText(attestation.getStatut())).append("\n")
                .append("Demandée le: ").append(attestation.getDateDemande() != null ?
                        dateFormat.format(attestation.getDateDemande()) : "Date inconnue");

        if (attestation.getDateTraitement() != null) {
            details.append("\nTraité le: ").append(dateFormat.format(attestation.getDateTraitement()));
        }

        if ("refusee".equals(attestation.getStatut()) && attestation.getMotifRefus() != null) {
            details.append("\nMotif du refus: ").append(attestation.getMotifRefus());
        }

        return details.toString();
    }

    private void showDetailsDialog(String details) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Détails de l'attestation");
        builder.setMessage(details);
        builder.setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private String getStatutText(String statut) {
        switch (statut) {
            case "en_attente": return "en attente";
            case "approuvee": return "approuvée";
            case "refusee": return "refusée";
            default: return "";
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
        button.setBackgroundResource(R.drawable.border_gris);
        button.setBackgroundTintList(null);
    }

    private void rechargerDonnees() {
        loadStats();
        chargerAttestations(filtreActuel);
    }

    // Méthodes utilitaires pour afficher les messages sur le thread principal
    private void showErrorOnMainThread(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showSuccessOnMainThread(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        rechargerDonnees();
    }
}