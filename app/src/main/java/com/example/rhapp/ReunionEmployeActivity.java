package com.example.rhapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class ReunionEmployeActivity extends AppCompatActivity {
    private LinearLayout reunionPlanifieContainer, reunionPasseContainer,noReunionContainer,noReunionContainerPasse , noReunionContainerVenirs;
    private int nbrConfirm = 0;
    private TextView nbrReunionConfirmer;


    private FirebaseFirestore db;
    // ⚠️ Variable de contrôle pour savoir si on doit rafraîchir
    private boolean shouldRefresh = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reunion_employe);


        noReunionContainerPasse= findViewById(R.id.noReunionContainerPasse);
        noReunionContainerVenirs= findViewById(R.id.noReunionContainerVenirs);
        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        reunionPasseContainer = findViewById(R.id.reunionPasseContainer);
        noReunionContainer = findViewById(R.id.noReunionContainer);
        nbrReunionConfirmer = findViewById(R.id.nbrReunionConfirmer);
        nbrReunionVenir = findViewById(R.id.nbrReunionVenir);
        nbrReunionsMois = findViewById(R.id.nbrReunionsMois);


        // ******************************* Passer d'une fenetre a l'autre ************************************

        LinearLayout accueil = findViewById(R.id.accueil);
        LinearLayout presence = findViewById(R.id.presence);
        LinearLayout conge = findViewById(R.id.conge);
        LinearLayout profil = findViewById(R.id.profil);

        accueil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReunionEmployeActivity.this, AcceuilEmployeActivity.class));

            }
        });
        presence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReunionEmployeActivity.this, PresenceActivity.class));

            }
        });
        conge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReunionEmployeActivity.this, CongesEmployeActivity.class));

            }
        });
        profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReunionEmployeActivity.this, ProfileActivity.class));

            }
        });


        // ************************************* Afficher les cards view des reuinons a venir **************************

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        db = FirebaseFirestore.getInstance();
        afficherReunionsVenirs();


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

        // Vider les conteneurs avant d'afficher
        reunionPlanifieContainer.removeAllViews();
        reunionPasseContainer.removeAllViews();

        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Erreur : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Réinitialiser le compteur avant de compter
                    nbrConfirm = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reunion reunion = doc.toObject(Reunion.class);
                        if (reunion.isConfirmed()) nbrConfirm++;
                    }
                    nbrReunionConfirmer.setText(String.valueOf(nbrConfirm));


                    // Revide encore une fois pour éviter accumulation
                    reunionPlanifieContainer.removeAllViews();
                    reunionPasseContainer.removeAllViews();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        noReunionContainer.setVisibility(View.VISIBLE);
                        return;
                    }

                    // ---------- AJOUTER LES TITRES UNE SEULE FOIS ----------
                    TextView titreVenirs = new TextView(this);
                    titreVenirs.setText("Réunions à venir");
                    titreVenirs.setTextSize(20);
                    titreVenirs.setPadding(20, 20, 20, 20);
                    titreVenirs.setTextColor(Color.BLACK);

                    TextView titrePasses = new TextView(this);
                    titrePasses.setText("Réunions Passées");
                    titrePasses.setTextSize(20);
                    titrePasses.setPadding(20, 20, 20, 20);
                    titrePasses.setTextColor(Color.BLACK);

                    // Ajouter les titres UNE SEULE FOIS
                    reunionPlanifieContainer.addView(titreVenirs);
                    reunionPasseContainer.addView(titrePasses);

                    // -------------------------------------------------------

                    // Parcourir les réunions
                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        Reunion reunion = doc.toObject(Reunion.class);
                        reunion.setId(doc.getId());

                        String dateStr = reunion.getDate();
                        String timeStr = reunion.getHeure();
                        int etatDate = compareDate(dateStr, timeStr);


                        // ------------------ RÉUNIONS À VENIR ------------------
                        if (etatDate == 1) {

                            View cardView = LayoutInflater.from(this)
                                    .inflate(R.layout.item_card_reunionemploye_pasconfirmer, reunionPlanifieContainer, false);

                            TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                            TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                            TextView timeReunion = cardView.findViewById(R.id.timeReunion);
                            TextView lieuReunion = cardView.findViewById(R.id.localReunion);
                            TextView tempsRestantReunion = cardView.findViewById(R.id.tempsRestantReunion);
                            Button btnConfirmerReunion = cardView.findViewById(R.id.btnConfirmerReunion);
                            ImageView valid = cardView.findViewById(R.id.iconValid);

                            //afficher le nbr de participatns
                            int nbr = reunion.getParticipantsCount();
                            TextView Participants = cardView.findViewById(R.id.participants);
                            Participants.setText(reunion.getParticipantsCount() + " participants");

                            //afficher le nom de leader
                            TextView reunionLeader = cardView.findViewById(R.id.reunionLeader);
                            String leaderName = reunion.getLeaderNomComplet();

                            if (leaderName != null) {
                                leaderName = capitalizeWords(leaderName);
                                reunionLeader.setText("Organisé par " + leaderName);
                            } else {
                                reunionLeader.setText("RH inconnu");
                            }



                            try {
                                dateReunion.setText(convertDate(dateStr));
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate());
                            }

                            titreReunion.setText(reunion.getTitre());
                            timeReunion.setText(reunion.getHeure());
                            lieuReunion.setText(reunion.getLieu());
                            tempsRestantReunion.setText(NbrJourRestant(dateStr));



                            btnConfirmerReunion.setOnClickListener(v -> {
                                // Masquer le bouton et afficher l'icône valid
                                btnConfirmerReunion.setVisibility(View.GONE);
                                valid.setVisibility(View.VISIBLE);

                                // Incrémenter le compteur local
                                nbrConfirm++;  // compteur local
                                nbrReunionConfirmer.setText(String.valueOf(nbrConfirm)); // mettre à jour l'affichage

                                // Mettre à jour Firestore pour que la confirmation soit persistante
                                db.collection("Reunions").document(reunion.getId())
                                        .update(
                                                "confirmed", true,
                                                "participantsCount", FieldValue.increment(1)
                                        )
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Confirmation enregistrée", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            btnConfirmerReunion.setVisibility(View.VISIBLE);
                                            valid.setVisibility(View.GONE);
                                            nbrConfirm--;
                                            nbrReunionConfirmer.setText(String.valueOf(nbrConfirm));
                                        });



                            });


                            if (reunion.isConfirmed()) {
                                btnConfirmerReunion.setVisibility(View.GONE);
                                valid.setVisibility(View.VISIBLE);
                            } else {
                                btnConfirmerReunion.setVisibility(View.VISIBLE);
                                valid.setVisibility(View.GONE);
                            }





                            reunionPlanifieContainer.addView(cardView);
                        }

                        // ------------------ RÉUNIONS PASSÉES ------------------
                        else if (etatDate == -1) {

                            View cardView = LayoutInflater.from(this)
                                    .inflate(R.layout.item_card_reunionpasse, reunionPasseContainer, false);

                            TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                            TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                            TextView lieuReunion = cardView.findViewById(R.id.lieuReunion);
                            TextView participantsReunion = cardView.findViewById(R.id.participants);

                            int nbr = reunion.getParticipantsCount();
                            TextView Participants = cardView.findViewById(R.id.participants);
                            participantsReunion.setText(reunion.getParticipantsCount() + " participants");

                            try {
                                dateReunion.setText(convertDate(dateStr) + " , " + reunion.getHeure());
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate() + " , " + reunion.getHeure());
                            }

                            titreReunion.setText(reunion.getTitre());
                            lieuReunion.setText(reunion.getLieu());
                            //participantsReunion.setText(reunion.getParticipants());

                            reunionPasseContainer.addView(cardView);
                        }
                    }
                });
    }


    // ******************* fonction pour convertir la date en date complet **********
    private String convertDate(String date){
        // 1. Définir le format source
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // 2. Parser vers LocalDate ( convertir la date entrer au date forme )
        LocalDate dateForm = LocalDate.parse(date, inputFormatter);
        // 3. Définir le format souhaité avec la locale française
        DateTimeFormatter outputFormatter =
                DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        // 4. Formatter en string et afficher
        String formattedDate = dateForm.format(outputFormatter);

        return formattedDate;

    }


    // ******* fonct pour comparer une date avec date d aujourd'hui ( pour determiner les passes avec a venir ) *******
    private int compareDate(String date, String time) {

        if (date == null || date.isEmpty() || time == null || time.isEmpty()) {
            return -1;
        }

        // Correction : on parse la date même si elle est au format "12122004"
        LocalDate dateForm = parseFlexibleDate(date);
        if (dateForm == null) {
            return -1; // date non valide → on classe en passée
        }

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
        //****** Determiner la date restant **********
        LocalDate today = LocalDate.now();
        LocalDate dateForm = parseFlexibleDate(date);
        if (dateForm == null) return "Date invalide";

        long JourResrant = ChronoUnit.DAYS.between(today, dateForm);//calcul la difference entre les dates
        String rest ;

        if(JourResrant==0){
            rest = "Aujourd'hui ";
        } else if (JourResrant==1) {
            rest = "Demain ";
        }
        else {
            rest = "Dans -"+JourResrant+ " Jour ";
        }
        return rest ;
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
    private TextView nbrReunionPassees ,nbrReunionsMois  ;



    private void statistique() {
        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    int nbrVenirs = 0;
                    int nbrPassees = 0;
                    int nbrMois = 0;

                    if (querySnapshot != null) {
                        LocalDate today = LocalDate.now();
                        int currentMonth = today.getMonthValue();
                        int currentYear = today.getYear();

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // adapter si ton format est différent

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reunion reunion = doc.toObject(Reunion.class);
                            int etatDate = compareDate(reunion.getDate(), reunion.getHeure());

                            if (etatDate == 1 || etatDate == 0) nbrVenirs++;
                            else nbrPassees++;

                            // Parser le String en LocalDate
                            LocalDate reunionDate = parseFlexibleDate(reunion.getDate());
                            if (reunionDate == null) continue;


                            if (reunionDate.getMonthValue() == currentMonth && reunionDate.getYear() == currentYear) {
                                nbrMois++;
                            }
                        }
                    }

                    nbrReunionVenir.setText(String.valueOf(nbrVenirs));
                    nbrReunionsMois.setText(String.valueOf(nbrMois));

//                    reunionPlanifieContainer.setVisibility(nbrVenirs == 0 ? View.GONE : View.VISIBLE);
//                    reunionPasseContainer.setVisibility(nbrPassees == 0 ? View.GONE : View.VISIBLE);


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

    private LocalDate parseFlexibleDate(String date) {

        // 1. Format "dd/MM/yyyy"
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        // 2. Format "dd-MM-yyyy"
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {}

        return null;
    }


    //Pour afficher les premier lettre em Maj
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;

        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();

        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }



}



