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
import java.util.Locale;

public class reunionActivity extends AppCompatActivity {

    private LinearLayout reunionPlanifieContainer, reunionPasseContainer,noReunionContainer;
    private FirebaseFirestore db;
    // ⚠️ Variable de contrôle pour savoir si on doit rafraîchir
    private boolean shouldRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reunion);

        reunionPlanifieContainer = findViewById(R.id.reunionPlanifieContainer);
        reunionPasseContainer = findViewById(R.id.reunionPasseContainer);
        noReunionContainer = findViewById(R.id.noReunionContainer);

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

                    reunionPlanifieContainer.removeAllViews();
                    reunionPasseContainer.removeAllViews();

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        noReunionContainer.setVisibility(View.VISIBLE);
                        return;
                    }

                    // ================================
                    // AJOUTER LES TITRES UNE SEULE FOIS
                    // ================================

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

                    // ================================
                    // BOUCLE D'AFFICHAGE DES RÉUNIONS
                    // ================================

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        Reunion reunion = doc.toObject(Reunion.class);
                        reunion.setId(doc.getId());

                        String dateStr = reunion.getDate();
                        String timeStr = reunion.getHeure();
                        int etatDate = compareDate(dateStr, timeStr);

                        // ------------------ RÉUNIONS À VENIR ------------------
                        if (etatDate == 1) {

                            View cardView = LayoutInflater.from(this)
                                    .inflate(R.layout.item_card_reunion_rh, reunionPlanifieContainer, false);

                            TextView titreReunion = cardView.findViewById(R.id.titreReunion);
                            TextView dateReunion = cardView.findViewById(R.id.dateReunion);
                            TextView timeReunion = cardView.findViewById(R.id.timeReunion);
                            TextView lieuReunion = cardView.findViewById(R.id.localReunion);
                            TextView departementReunion = cardView.findViewById(R.id.departementReunion);
                            TextView descriptionReunion = cardView.findViewById(R.id.reunionDescription);
                            TextView tempsRestantReunion = cardView.findViewById(R.id.tempsRestantReunion);
                            ImageView edit = cardView.findViewById(R.id.iconEdit);
                            ImageView delete = cardView.findViewById(R.id.iconDelete);

                            try {
                                dateReunion.setText(convertDate(dateStr));
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate());
                            }

                            titreReunion.setText(reunion.getTitre());
                            timeReunion.setText(reunion.getHeure());
                            lieuReunion.setText(reunion.getLieu());
                            departementReunion.setText(reunion.getDepartement());
                            descriptionReunion.setText(reunion.getDescription());
                            tempsRestantReunion.setText(NbrJourRestant(dateStr));

                            // ---- Editer ----
                            edit.setOnClickListener(v -> {
                                Intent intent = new Intent(reunionActivity.this, ModifierReunionActivity.class);
                                intent.putExtra("reunionId", reunion.getId());
                                intent.putExtra("titre", reunion.getTitre());
                                intent.putExtra("date", reunion.getDate());
                                intent.putExtra("time", reunion.getHeure());
                                intent.putExtra("lieu", reunion.getLieu());
                                intent.putExtra("departement", departementReunion.getText().toString());
                                intent.putExtra("description", reunion.getDescription());
                                startActivity(intent);
                            });

                            // ---- Supprimer ----
                            delete.setOnClickListener(v -> {
                                new AlertDialog.Builder(reunionActivity.this)
                                        .setTitle("Supprimer")
                                        .setMessage("Voulez-vous vraiment supprimer cette réunion ?")
                                        .setPositiveButton("Oui", (dialog, which) -> {
                                            db.collection("Reunions")
                                                    .document(reunion.getId())
                                                    .delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(reunionActivity.this, "Réunion supprimée", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(reunionActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                                    );
                                        })
                                        .setNegativeButton("Annuler", null)
                                        .show();
                            });

                            // Ajouter la card
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

                            try {
                                dateReunion.setText(convertDate(dateStr) + " , " + reunion.getHeure());
                            } catch (Exception e) {
                                dateReunion.setText(reunion.getDate() + " , " + reunion.getHeure());
                            }

                            titreReunion.setText(reunion.getTitre());
                            lieuReunion.setText(reunion.getLieu());
                            participantsReunion.setText(reunion.getDescription());

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
           return -1; // considérer comme passée ou ignorer
       }

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



    //**************************** fct pour remplir les statistique*********************
    private TextView nbrReunionVenir;
    private TextView nbrReunionPassees;

    private void statistique() {
        nbrReunionVenir = findViewById(R.id.nbrReunionVenir);
        nbrReunionPassees = findViewById(R.id.nbrReunionPassees);

        db.collection("Reunions")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;

                    int nbrVenirs = 0;
                    int nbrPassees = 0;

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Reunion reunion = doc.toObject(Reunion.class);
                            int etatDate = compareDate(reunion.getDate(), reunion.getHeure());
                            if (etatDate == 1 || etatDate == 0) nbrVenirs++;
                            else nbrPassees++;
                        }
                    }

                    nbrReunionVenir.setText(String.valueOf(nbrVenirs));
                    nbrReunionPassees.setText(String.valueOf(nbrPassees));

                    reunionPlanifieContainer.setVisibility(nbrVenirs == 0 ? View.GONE : View.VISIBLE);
                    reunionPasseContainer.setVisibility(nbrPassees == 0 ? View.GONE : View.VISIBLE);
                });
    }



}