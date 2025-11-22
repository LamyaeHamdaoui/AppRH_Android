package com.example.rhapp;


import androidx.activity.EdgeToEdge;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;

import com.example.rhapp.model.Employe;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class EmployeActivity extends AppCompatActivity {

    private LinearLayout itemsEmployeeCardsContainer, noEmployeeContainer;
    private EditText rechercherEmploye;
    private Spinner departement;
    private Button btnAddEmploye;
    private FrameLayout fragmentContainer;
    private ScrollView mainContent;
    private RelativeLayout relative;
    private LinearLayout footer;
    private FirebaseFirestore db;
    private TextView actif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employe);

        // --- Initialisation des vues ---
        itemsEmployeeCardsContainer = findViewById(R.id.itemsEmployeeCardsContainer);
        noEmployeeContainer = findViewById(R.id.noEmployeeContainer);
        rechercherEmploye = findViewById(R.id.rechercherEmploye);
        departement = findViewById(R.id.departement);
        btnAddEmploye = findViewById(R.id.btnAddEmploye);
        actif = findViewById(R.id.employesActifs);
        fragmentContainer = findViewById(R.id.fragment_container);
        mainContent = findViewById(R.id.main_content);
        relative= findViewById(R.id.relative);
        footer= findViewById(R.id.footer);

        db = FirebaseFirestore.getInstance();

        styliserSpinnerRecherche();

        // --- Configuration de la recherche ---
        configurerRecherche();

// --- Configuration du filtre par département ---
        configurerFiltreDepartement();

// --- Charger les employés ---
        chargerEmployes();

        EdgeToEdge.enable(this);
        LinearLayout reunions = findViewById(R.id.reunions);
        LinearLayout conges = findViewById(R.id.conges);
        LinearLayout Acceuil = findViewById(R.id.Acceuil);
        LinearLayout profile = findViewById(R.id.profile);


        reunions.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(EmployeActivity.this, reunionActivity.class);
                startActivity(intent);
            }});
        conges.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(EmployeActivity.this, CongesActivity.class);
                startActivity(intent);
            }
        });
        Acceuil.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(EmployeActivity.this, AcceuilRhActivity.class);
                startActivity(intent);
            }
        });
        profile.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(EmployeActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });


        // --- Bouton Ajouter Employé ---
        btnAddEmploye.setOnClickListener(v -> ouvrirFragmentAddEmploye());

        // --- Gestion moderne du bouton back ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fragmentContainer.getVisibility() == View.VISIBLE) {
                    getSupportFragmentManager().popBackStack();
                    fragmentContainer.setVisibility(View.GONE);
                    mainContent.setVisibility(View.VISIBLE);
                } else {
                    setEnabled(false); // pour appeler le back par défaut
                    onBackPressed();
                }
            }
        });
    }

    // --- Ouvrir AddEmployeFragment ---
    private void ouvrirFragmentAddEmploye() {
        mainContent.setVisibility(View.GONE);
        relative.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);

        fragmentContainer.setVisibility(View.VISIBLE);

        AddEmployeFragment addFragment = new AddEmployeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, addFragment)
                .addToBackStack("add_employe")
                .commit(); }

    // --- Ouvrir EditEmployeFragment ---
    private void ouvrirFragmentEditEmploye(String employeId) {
        mainContent.setVisibility(View.GONE);
        relative.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        EditEmployeFragment editFragment = EditEmployeFragment.newInstance(employeId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack("edit_employe")
                .commit();
    }

    // --- Charger les employés depuis Firestore ---
    private void chargerEmployes() {
        itemsEmployeeCardsContainer.removeAllViews();

        db.collection("employees").get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        actif.setText("0 employé actif");
                        return;
                    }

                    int nombreEmployesActifs = queryDocumentSnapshots.size();
                    actif.setText(nombreEmployesActifs + " employés actifs");


                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Employe emp = document.toObject(Employe.class);
                        emp.setId(document.getId());

                        View card = getLayoutInflater().inflate(R.layout.item_employee_card, null);
                        setCardInfo(card, emp);
                        itemsEmployeeCardsContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement employés", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Erreur: ", e);
                });
    }

    // --- Remplir une carte employé ---
    private void setCardInfo(View card, Employe emp) {
        TextView np = card.findViewById(R.id.np);
        TextView nomComplet = card.findViewById(R.id.nomComplet);
        TextView poste = card.findViewById(R.id.poste);
        TextView departementTv = card.findViewById(R.id.departement);
        TextView email = card.findViewById(R.id.email);
        TextView telephone = card.findViewById(R.id.telephone);
        TextView dateEmbauche = card.findViewById(R.id.dateEmbauche);
        TextView soldeConge = card.findViewById(R.id.soldeConge);

        np.setText(emp.getPrenom().substring(0, 1) + emp.getNom().substring(0, 1));
        nomComplet.setText(emp.getNomComplet());
        poste.setText(emp.getPoste());
        departementTv.setText(emp.getDepartement());
        email.setText(emp.getEmail());
        telephone.setText(emp.getTelephone());
        Timestamp ts = emp.getDateEmbauche();
        if (ts != null) {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate());
            dateEmbauche.setText("Embauché le " + dateStr);
        } else {
            dateEmbauche.setText("Date inconnue");
        }
        soldeConge.setText(emp.getSoldeConge() + " jours");

        // --- Bouton Edit ---
        card.findViewById(R.id.editEmploye).setOnClickListener(v -> ouvrirFragmentEditEmploye(emp.getId()));


        // --- Bouton Delete ---
        card.findViewById(R.id.deleteEmploye).setOnClickListener(v -> {
            // Créer un TextView personnalisé pour le titre
            TextView customTitle = new TextView(this);
            customTitle.setText("Confirmer la suppression");
            customTitle.setPadding(50, 40, 50, 40); // padding autour du texte
            customTitle.setGravity(Gravity.CENTER); // centrer le titre
            customTitle.setTextColor(Color.BLACK); // couleur du texte du titre
            customTitle.setTextSize(20f); // taille du titre

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCustomTitle(customTitle);
            builder.setMessage("Voulez-vous vraiment supprimer cet employé ?");

            // Message en noir
            builder.setMessage("Voulez-vous vraiment supprimer cet employé ?");

            builder.setPositiveButton("Oui", (dialog, which) -> {
                db.collection("employees").document(emp.getId())
                        .delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Employé supprimé", Toast.LENGTH_SHORT).show();
                            chargerEmployes();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show());
            });
            builder.setNegativeButton("Non", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();

            // --- Personnalisation du background et du message ---
            if(dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE)); // arrière-plan blanc
            }

            // Message en noir
            TextView messageText = dialog.findViewById(android.R.id.message);
            if(messageText != null) {
                messageText.setTextColor(Color.BLACK);
            }

            // Boutons : optionnel
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
        });



    }

    // --- Configuration de la recherche par texte ---
    private void configurerRecherche() {
        rechercherEmploye.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrerEmployes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- Configuration du filtre par département ---
    private void configurerFiltreDepartement() {
        departement.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtrerEmployes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // --- Filtrer les employés selon les critères ---
    private void filtrerEmployes() {
        String rechercheTexte = rechercherEmploye.getText().toString().toLowerCase().trim();
        String departementSelectionne = departement.getSelectedItem().toString();

        db.collection("employees").get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    itemsEmployeeCardsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        actif.setText("0 employé actif");
                        noEmployeeContainer.setVisibility(View.VISIBLE);
                        return;
                    }

                    int totalEmployes = queryDocumentSnapshots.size(); // Total des employés dans la base
                    int employesFiltres = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Employe emp = document.toObject(Employe.class);
                        emp.setId(document.getId());

                        // Vérifier si l'employé correspond aux critères de recherche
                        if (correspondRecherche(emp, rechercheTexte, departementSelectionne)) {
                            View card = getLayoutInflater().inflate(R.layout.item_employee_card, null);
                            setCardInfo(card, emp);
                            itemsEmployeeCardsContainer.addView(card);
                            employesFiltres++;
                        }
                    }

                    // Afficher le nombre TOTAL d'employés
                    actif.setText(totalEmployes + " employé(s) actif(s)");

                    if (employesFiltres == 0) {
                        noEmployeeContainer.setVisibility(View.VISIBLE);
                    } else {
                        noEmployeeContainer.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur chargement employés", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Erreur: ", e);
                });
    }

    // --- Vérifier si un employé correspond aux critères de recherche ---
    private boolean correspondRecherche(Employe emp, String rechercheTexte, String departementSelectionne) {
        boolean correspondTexte = true;
        boolean correspondDepartement = true;

        // Filtre par texte (nom, prénom, email, poste)
        if (!rechercheTexte.isEmpty()) {
            correspondTexte = emp.getNom().toLowerCase().contains(rechercheTexte) ||
                    emp.getPrenom().toLowerCase().contains(rechercheTexte) ||
                    emp.getNomComplet().toLowerCase().contains(rechercheTexte) ||
                    emp.getEmail().toLowerCase().contains(rechercheTexte) ||
                    emp.getPoste().toLowerCase().contains(rechercheTexte);
        }

        // Filtre par département (sauf si "Tous les départements" est sélectionné)
        if (!departementSelectionne.equals("Tous les départements")) {
            correspondDepartement = emp.getDepartement().equals(departementSelectionne);
        }

        return correspondTexte && correspondDepartement;
    }

    private void styliserSpinnerRecherche() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.departements_recherche,
                R.layout.spinner_dropdown_item  // Utilise notre layout personnalisé
        );
        departement.setAdapter(adapter);
    }

}
