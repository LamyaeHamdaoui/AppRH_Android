package com.example.rhapp;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;
import androidx.activity.OnBackPressedCallback;
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

        db = FirebaseFirestore.getInstance();

        // --- Charger les employés ---
        chargerEmployes();

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
        fragmentContainer.setVisibility(View.VISIBLE);

        AddEmployeFragment addFragment = new AddEmployeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, addFragment)
                .addToBackStack("add_employe")
                .commit();
    }

    // --- Ouvrir EditEmployeFragment ---
    private void ouvrirFragmentEditEmploye(String employeId) {
        mainContent.setVisibility(View.GONE);
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
                        noEmployeeContainer.setVisibility(View.VISIBLE);
                        actif.setText("0 employé actif");
                        return;
                    }

                    int nombreEmployesActifs = queryDocumentSnapshots.size();
                    actif.setText(nombreEmployesActifs + " employés actifs");
                    noEmployeeContainer.setVisibility(View.GONE);

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
            db.collection("employees").document(emp.getId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Employé supprimé", Toast.LENGTH_SHORT).show();
                        chargerEmployes();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show());
        });
    }
}
