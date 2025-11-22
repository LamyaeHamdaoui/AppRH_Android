package com.example.rhapp;

import android.content.DialogInterface;
import android.content.Intent;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class reunionActivity extends AppCompatActivity {

    private LinearLayout reunionPlanifieContainer, reunionPasseContainer,noReunionContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reunion);

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        reunionPasseContainer = findViewById(R.id.reunionPasseContainer);

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

        // ********************************** Notifications ********************

        RelativeLayout notifications = findViewById(R.id.notifications);

        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(reunionActivity.this, NotificationsRhActivity.class));
            }
        });

        // ************************************* Afficher les cards view des reuinons a venir **************************

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        db = FirebaseFirestore.getInstance();
        afficherReunionsVenirs();


    }

    @Override
    protected void onResume() {
        super.onResume();
        afficherReunionsVenirs(); // recharge toutes les réunions
    }

    // ************************************* fonction  Afficher  des reuinons  **************************

    private void afficherReunionsVenirs() {
        // Vider les conteneurs avant de reconstruire les cards
        reunionPlanifieContainer.removeAllViews();
        reunionPasseContainer.removeAllViews();


        db.collection("Reunions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        noReunionContainer.setVisibility(View.VISIBLE);
                        return;
                    }


                    // Pour chaque réunion dans Firestore
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reunion reunion = doc.toObject(Reunion.class);
                        reunion.setId(doc.getId());

                        // concerant les reunions venirs
                        String dateStr = reunion.getDate();
                        String timeStr = reunion.getHeure();
                        int etatDate = compareDate(dateStr,timeStr);

                        //si la date est a venir on affiche cette card
                        if(etatDate==1) {

                            //  On "gonfle" la carte depuis le XML
                            View cardView = LayoutInflater.from(this)
                                    .inflate(R.layout.item_card_reunion_rh, reunionPlanifieContainer, false);

                            // On remplit les données
                            TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                            TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                            TextView timeReunion = cardView.findViewById(R.id.timeReunion);
                            TextView lieuReunion = cardView.findViewById(R.id.localReunion);
                            TextView departementReunion = cardView.findViewById(R.id.departementReunion);
                            TextView descriptionReunion = cardView.findViewById(R.id.reunionDescription);
                            TextView tempsRestantReunion = cardView.findViewById(R.id.tempsRestantReunion);
                            ImageView edit =  cardView.findViewById(R.id.iconEdit);
                            ImageView delete=  cardView.findViewById(R.id.iconDelete);


                            //************ convertir la date ********************
                            try {
                                dateReunion.setText(convertDate(dateStr));
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate()); // fallback
                            }

                            // *********** remplir les elements dans la card ***********
                            titreReunion.setText(reunion.getTitre());
                            timeReunion.setText(reunion.getHeure());
                            lieuReunion.setText(reunion.getLieu());
                            departementReunion.setText(reunion.getDepartement());
                            descriptionReunion.setText(reunion.getDescription());
                            tempsRestantReunion.setText(NbrJourRestant(dateStr));

                            //************* editer un reunion **************
                            edit.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(reunionActivity.this, ModifierReunionActivity.class);
                                    intent.putExtra("reunionId",reunion.getId());
                                    intent.putExtra("titre",reunion.getTitre());
                                    intent.putExtra("date",reunion.getDate());
                                    intent.putExtra("time",reunion.getHeure());
                                    intent.putExtra("lieu",reunion.getLieu());
                                    intent.putExtra("departement", departementReunion.getText().toString());
                                    intent.putExtra("description",reunion.getDescription());
                                    startActivity(intent);





                                }
                            });

                            //************* delete un reunion **************
                            delete.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AfficherBoiteDialogue();

                                }
                            });



                            //  On ajoute la carte dans le conteneur
                            reunionPlanifieContainer.addView(cardView);

                        } else if (etatDate==-1) {

                            //  On "gonfle" la carte depuis le XML
                            View cardView = LayoutInflater.from(this)
                                    .inflate(R.layout.item_card_reunionpasse,reunionPasseContainer, false);

                            // On remplit les données
                            TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                            TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                            //TextView timeReunion = cardView.findViewById(R.id.timeReunion);
                            TextView lieuReunion = cardView.findViewById(R.id.lieuReunion);
                            //TextView departementReunion = cardView.findViewById(R.id.departementReunion);
                            TextView participantsReunion = cardView.findViewById(R.id.participants);

                            //************ convertir la date ********************
                            try {
                                dateReunion.setText(convertDate(dateStr)+" , "+reunion.getHeure());
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate()+" , "+reunion.getHeure()); // fallback
                            }


                            // remplir les elements dans la card
                            titreReunion.setText(reunion.getTitre());
                            //dateReunion.setText(formattedDate);
                            //dateReunion.setText(reunion.getDate());
                            // timeReunion.setText(reunion.getHeure());
                            lieuReunion.setText(reunion.getLieu());
                            participantsReunion.setText(reunion.getDescription());

                            //  On ajoute la carte dans le conteneur
                            reunionPasseContainer.addView(cardView);

                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(reunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

       // Conversion de la date en LocalDate
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");// Format correspondant dans le XML
       LocalDate dateForm = LocalDate.parse(date, formatter);// Conversion en LocalDate

       // Conversion de l'heure en LocalTime
       DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
       LocalTime timeForm = LocalTime.parse(time, timeFormatter);

       // Date actuelle
       LocalDate today = LocalDate.now();
       LocalDateTime now = LocalDateTime.now();


       // Combinaison en LocalDateTime
       LocalDateTime xmlDateTime = LocalDateTime.of(dateForm, timeForm);

       int result;
       if (xmlDateTime.isEqual(now)) {
           result = 0; //maintenant
       } else if (xmlDateTime.isBefore(now)) {
           result = -1; //passe
       } else {
           result = 1; //venir
       }
       return result;
   }


    // ******* fonct pour calculer la difference entre une date et la date d aujourd'hui  *******
    private String NbrJourRestant(String date){
        //****** Determiner la date restant **********
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");// Format correspondant dans le XML
        LocalDate dateForm = LocalDate.parse(date, formatter);// Conversion en LocalDate

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

<<<<<<< HEAD

    //**************************** fct pour remplir les statistique*********************
    private TextView nbrReunionVenir;
    private TextView nbrReunionPassees;

    private void statistique() {

        nbrReunionVenir = findViewById(R.id.nbrReunionVenir);
        nbrReunionPassees = findViewById(R.id.nbrReunionPassees);


        db.collection("Reunions")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int nbrVenirs = 0;
                    int nbrPassees = 0;

                    if (querySnapshot.isEmpty()) {
                        nbrReunionVenir.setText("0");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Reunion reunion = doc.toObject(Reunion.class);
                        String dateStr = reunion.getDate();
                        String timeStr = reunion.getHeure();


                        // concerant les reunions venirs
                        int etatDate = compareDate(dateStr,timeStr);

                        //si la date est a venir on affiche cette card
                        if(etatDate==1 || etatDate==0) {
                            nbrVenirs++;
                        }
                        else {
                            nbrPassees++;
                        }


                        //les cardes devient invisibles si nbr == 0
                        if(nbrVenirs==0){
                            reunionPlanifieContainer.setVisibility(View.GONE);
                        }
                        if(nbrPassees==0){
                            reunionPasseContainer.setVisibility(View.GONE);
                        }

                        nbrReunionVenir.setText(String.valueOf(nbrVenirs));
                        nbrReunionPassees.setText(String.valueOf(nbrPassees));

                    }


                });
    }

=======
>>>>>>> 89334ab936d6613d08b07cf4834b8483f31438a5
}