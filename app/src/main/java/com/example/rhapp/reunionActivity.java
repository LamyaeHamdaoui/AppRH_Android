package com.example.rhapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.model.Reunion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class reunionActivity extends AppCompatActivity {

    private LinearLayout reunionPlanifieContainer, reunionPasseContainer,noReunionContainer,noReunionContainerPasse , noReunionContainerVenirs;
    private FirebaseFirestore db;
    private TextView nbrParticipants;
    String Participants;

    //  Variable de contrôle pour savoir si on doit rafraîchir
    private boolean shouldRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reunion);

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        reunionPasseContainer = findViewById(R.id.reunionPasseContainer);
        noReunionContainer = findViewById(R.id.noReunionContainer);
        noReunionContainerPasse= findViewById(R.id.noReunionContainerPasse);
        noReunionContainerVenirs= findViewById(R.id.noReunionContainerVenirs);

        // ******************************* Passer d'une fenetre a l'autre ************************************
        LinearLayout accueil = findViewById(R.id.accueil);
        LinearLayout employes = findViewById(R.id.employes);
        LinearLayout conge = findViewById(R.id.conge);
        LinearLayout profil = findViewById(R.id.profil);

        accueil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, AcceuilRhActivity.class));

            }
        });
        employes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, EmployeActivity.class));

            }
        });
        conge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, CongesActivity.class));

            }
        });
        profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, ProfileActivity.class));

            }
        });

        // ********************************** Planifier un reunion ********************

        Button btnPlanifierReunion = findViewById(R.id.btnPlanifierReunion);

        btnPlanifierReunion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, PlanifierReunionActivity.class));
            }
        });



        // ************************************* Afficher les cards view des reuinons a venir **************************

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        db = FirebaseFirestore.getInstance();
        afficherReunionsVenirs();


        Intent intent = getIntent();
        Participants = intent.getStringExtra("nbrConfirm");

        statistique();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Seulement rafraîchir si tu viens d'une autre activité de modification
        if (shouldRefresh) {
            afficherReunionsVenirs();
            shouldRefresh = false;
        }
    }

    // ************************************* fonction  Afficher  des reuinons  **************************

    private void afficherReunionsVenirs() {

        // Vider les conteneurs
        reunionPlanifieContainer.removeAllViews();
        reunionPasseContainer.removeAllViews();

        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(this, "Erreur : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reunionPlanifieContainer.removeAllViews();
                    reunionPasseContainer.removeAllViews();

                    // ===========================
                    // Ajouter titres
                    // ===========================
                    TextView tvHeaderVenirs = new TextView(this);
                    tvHeaderVenirs.setText("Réunions à venir");
                    tvHeaderVenirs.setTextSize(20);
                    tvHeaderVenirs.setPadding(20, 20, 20, 20);
                    tvHeaderVenirs.setTextColor(Color.BLACK);

                    TextView tvHeaderPasses = new TextView(this);
                    tvHeaderPasses.setText("Réunions passées");
                    tvHeaderPasses.setTextSize(20);
                    tvHeaderPasses.setPadding(20, 20, 20, 20);
                    tvHeaderPasses.setTextColor(Color.BLACK);

                    reunionPlanifieContainer.addView(tvHeaderVenirs);
                    reunionPasseContainer.addView(tvHeaderPasses);

                    List<Reunion> reunionsVenir = new ArrayList<>();
                    List<Reunion> reunionsPasse = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reunion reunion = doc.toObject(Reunion.class);
                        reunion.setId(doc.getId());

                        int etatDate = compareDate(reunion.getDate(), reunion.getHeure());

                        if (etatDate >= 0) { // réunion à venir ou aujourd'hui
                            reunionsVenir.add(reunion);
                        } else { // réunion passée
                            reunionsPasse.add(reunion);
                        }
                    }

                    // ===========================
                    // Trier les réunions à venir par date + heure croissante
                    // ===========================
                    Collections.sort(reunionsVenir, (r1, r2) -> {
                        LocalDateTime dt1 = LocalDateTime.of(
                                safeParseDate(r1.getDate()),
                                LocalTime.parse(r1.getHeure(), DateTimeFormatter.ofPattern("HH:mm"))
                        );
                        LocalDateTime dt2 = LocalDateTime.of(
                                safeParseDate(r2.getDate()),
                                LocalTime.parse(r2.getHeure(), DateTimeFormatter.ofPattern("HH:mm"))
                        );
                        return dt1.compareTo(dt2); // croissant
                    });

                    // Trier les réunions passées par date décroissante (optionnel)
                    Collections.sort(reunionsPasse, (r1, r2) -> {
                        LocalDateTime dt1 = LocalDateTime.of(
                                safeParseDate(r1.getDate()),
                                LocalTime.parse(r1.getHeure(), DateTimeFormatter.ofPattern("HH:mm"))
                        );
                        LocalDateTime dt2 = LocalDateTime.of(
                                safeParseDate(r2.getDate()),
                                LocalTime.parse(r2.getHeure(), DateTimeFormatter.ofPattern("HH:mm"))
                        );
                        return dt2.compareTo(dt1); // décroissant
                    });

                    // ===========================
                    // Afficher les cards
                    // ===========================
                    for (Reunion reunion : reunionsVenir) {
                        View cardView = LayoutInflater.from(this)
                                .inflate(R.layout.item_card_reunion_rh, reunionPlanifieContainer, false);
                        setupCardView(cardView, reunion);
                        reunionPlanifieContainer.addView(cardView);
                    }

                    for (Reunion reunion : reunionsPasse) {
                        View cardView = LayoutInflater.from(this)
                                .inflate(R.layout.item_card_reunionpasse, reunionPasseContainer, false);

                        setupCardViewPasse(cardView, reunion);

                        reunionPasseContainer.addView(cardView);

                    }

                });
    }
    private void setupCardView(View cardView, Reunion reunion) {
        TextView titreReunion = cardView.findViewById(R.id.titreReunion);
        TextView dateReunion = cardView.findViewById(R.id.dateReunion);
        TextView timeReunion = cardView.findViewById(R.id.timeReunion);
        TextView lieuReunion = cardView.findViewById(R.id.localReunion);
        TextView departementReunion = cardView.findViewById(R.id.departementReunion);
        TextView descriptionReunion = cardView.findViewById(R.id.reunionDescription);
        TextView tempsRestantReunion = cardView.findViewById(R.id.tempsRestantReunion);
        ImageView edit = cardView.findViewById(R.id.iconEdit);
        ImageView delete = cardView.findViewById(R.id.iconDelete);
        TextView leaderReunion = cardView.findViewById(R.id.reunionLeader);
        TextView participants = cardView.findViewById(R.id.participants);

        titreReunion.setText(reunion.getTitre());
        lieuReunion.setText(reunion.getLieu());
        departementReunion.setText(reunion.getDepartement());
        descriptionReunion.setText(reunion.getDescription());
        participants.setText(reunion.getParticipantsCount() + " participants");

        try {
            dateReunion.setText(convertDate(reunion.getDate()));
        } catch (Exception e) {
            dateReunion.setText(reunion.getDate());
        }

        timeReunion.setText(reunion.getHeure());
        tempsRestantReunion.setText(NbrJourRestant(reunion.getDate()));
        afficherNomRH(reunion, leaderReunion);

        // Editer
        edit.setOnClickListener(v -> {
            Intent intent = new Intent(reunionActivity.this, ModifierReunionActivity.class);
            intent.putExtra("reunionId", reunion.getId());
            intent.putExtra("titre", reunion.getTitre());
            intent.putExtra("date", reunion.getDate());
            intent.putExtra("time", reunion.getHeure());
            intent.putExtra("lieu", reunion.getLieu());
            intent.putExtra("departement", reunion.getDepartement());
            intent.putExtra("description", reunion.getDescription());
            startActivity(intent);
        });

        // Supprimer
        delete.setOnClickListener(v -> {
            new AlertDialog.Builder(reunionActivity.this)
                    .setTitle("Supprimer")
                    .setMessage("Voulez-vous vraiment supprimer cette réunion ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        db.collection("Reunions")
                                .document(reunion.getId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(reunionActivity.this, "Réunion supprimée", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(reunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        });
    }

//*****************************
private void setupCardViewPasse(View cardView, Reunion reunion) {
    TextView titreReunion = cardView.findViewById(R.id.titreReunion);
    TextView dateReunion = cardView.findViewById(R.id.dateReunion);
    TextView lieuReunion = cardView.findViewById(R.id.lieuReunion);
    TextView participantsReunion = cardView.findViewById(R.id.participants);

    titreReunion.setText(reunion.getTitre());
    lieuReunion.setText(reunion.getLieu());
    participantsReunion.setText(reunion.getParticipantsCount() + " participants");

    try {
        dateReunion.setText(convertDate(reunion.getDate()) + " , " + reunion.getHeure());
    } catch (Exception e) {
        dateReunion.setText(reunion.getDate() + " , " + reunion.getHeure());
    }
}



    //***********************************
    // Conversion robuste de la date venant de Firestore
    private LocalDate safeParseDate(String date) {
        if (date == null || date.isEmpty()) return null;

        // Essayer format avec slash → dd/MM/yyyy
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        // Essayer format avec tirets → dd-MM-yyyy
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {}

        // ❌ NE PAS accepter les formats sans séparateurs
        // ddMMyyyy → doit retourner null pour lever l'exception dans compareDate()

        return null; // rien ne marche → date invalide
    }




    // ******************* fonction pour convertir la date en date complet **********
    private String convertDate(String date){
        LocalDate dateForm = safeParseDate(date);
        if (dateForm == null) return date;

        DateTimeFormatter outputFormatter =
                DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);

        return dateForm.format(outputFormatter);
    }



    // ******* fonct pour comparer une date avec date d aujourd'hui ( pour determiner les passes avec a venir ) *******
    private int compareDate(String date, String time) {
        LocalDate dateForm = safeParseDate(date);
        if (dateForm == null) return -1;

        LocalTime timeForm;
        try {
            timeForm = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return -1;
        }

        LocalDateTime xmlDateTime = LocalDateTime.of(dateForm, timeForm);
        LocalDateTime now = LocalDateTime.now();

        if (xmlDateTime.isEqual(now)) return 0;
        if (xmlDateTime.isBefore(now)) return -1;
        return 1;
    }



    // ******* fonct pour calculer la difference entre une date et la date d aujourd'hui  *******
    private String NbrJourRestant(String date){
        LocalDate dateForm = safeParseDate(date);
        if (dateForm == null) return "";

        LocalDate today = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(today, dateForm);

        if (diff == 0) return "Aujourd'hui";
        if (diff == 1) return "Demain";
        return "Dans " + diff + " jours";
    }


    private void AfficherBoiteDialogue(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer un Reunion");
        builder.setMessage("Voulez-vous supprimer ce reunion ?");
        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Action quand on clique sur Oui
            }
        });
        builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss(); // ferme la boîte de dialogue
            }
        });

        // Afficher la boîte de dialogue
        AlertDialog dialog = builder.create();
        dialog.show();

    }



    //**************************** fct pour remplir les statistique*********************
    private TextView nbrReunionVenir;
    private TextView nbrReunionPassees;


    private void statistique() {
        nbrReunionVenir = findViewById(R.id.nbrReunionVenir);
        nbrReunionPassees = findViewById(R.id.nbrReunionPassees);
        nbrParticipants = findViewById(R.id.nbrParticipants);

        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    int nbrVenirs = 0;
                    int nbrPassees = 0;
                    int nbrConfirmes = 0;

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reunion reunion = doc.toObject(Reunion.class);

                            int etatDate = compareDate(reunion.getDate(), reunion.getHeure());
                            if (etatDate == 1 || etatDate == 0) nbrVenirs++;
                            else nbrPassees++;

                            if (reunion.isConfirmed()) nbrConfirmes++;
                        }
                    }

                    // Mise à jour du texte statistique
                    nbrReunionVenir.setText(String.valueOf(nbrVenirs));
                    nbrReunionPassees.setText(String.valueOf(nbrPassees));
                    nbrParticipants.setText(String.valueOf(nbrConfirmes));

                    // Appeler la fonction qui affiche les containers
                    afficherContainers(nbrVenirs, nbrPassees);
                });
    }

    private void afficherContainers(int nbrVenirs, int nbrPassees) {

        // Masquer toutes les cards
        noReunionContainer.setVisibility(View.GONE);
        noReunionContainerVenirs.setVisibility(View.GONE);
        noReunionContainerPasse.setVisibility(View.GONE);
        reunionPlanifieContainer.setVisibility(View.GONE);
        reunionPasseContainer.setVisibility(View.GONE);

        // CAS 3 : aucune réunion nulle part
        if (nbrVenirs == 0 && nbrPassees == 0) {
            noReunionContainer.setVisibility(View.VISIBLE);
        }
        // CAS 1 : aucune réunion à venir
        else if (nbrVenirs == 0) {
            noReunionContainerVenirs.setVisibility(View.VISIBLE);
            reunionPasseContainer.setVisibility(View.VISIBLE);
        }
        // CAS 2 : aucune réunion passée
        else if (nbrPassees == 0) {
            noReunionContainerPasse.setVisibility(View.VISIBLE);
            reunionPlanifieContainer.setVisibility(View.VISIBLE);
        }
        // CAS NORMAL : il y a les deux
        else {
            reunionPlanifieContainer.setVisibility(View.VISIBLE);
            reunionPasseContainer.setVisibility(View.VISIBLE);
        }
    }


    private void afficherNomRH(Reunion reunion, TextView leaderReunion) {
        if (reunion.getPlanifiePar() != null && !reunion.getPlanifiePar().isEmpty()) {
            FirebaseFirestore.getInstance().collection("Users")
                    .document(reunion.getPlanifiePar())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nom = documentSnapshot.getString("nom");
                            String prenom = documentSnapshot.getString("prenom");

                            // --- Capitalisation ---
                            nom = capitalize(nom);
                            prenom = capitalize(prenom);

                            String fullName = nom + " " + prenom;

                            leaderReunion.setText("Par " + fullName);
                            reunion.setLeaderNomComplet(fullName);

                        } else {
                            leaderReunion.setText("RH inconnu");
                            reunion.setLeaderNomComplet("RH inconnu");
                        }
                    })
                    .addOnFailureListener(e -> {
                        leaderReunion.setText("RH inconnu");
                        reunion.setLeaderNomComplet("RH inconnu");
                    });
        } else {
            leaderReunion.setText("RH inconnu");
            reunion.setLeaderNomComplet("RH inconnu");
        }
    }

    // --- Fonction utilitaire pour capitaliser correctement ---
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }




}