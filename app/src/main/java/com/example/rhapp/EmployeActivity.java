package com.example.rhapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.model.Employe;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EmployeActivity extends AppCompatActivity {

    private LinearLayout itemsEmployeeCardsContainer;
    private LinearLayout noEmployeeContainer;
    private EditText rechercherEmploye;

    private FirebaseFirestore db;
    private List<Employe> employesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employe);

        // Récupération des vues
        itemsEmployeeCardsContainer = findViewById(R.id.itemsEmployeeCardsContainer);
        noEmployeeContainer = findViewById(R.id.noEmployeeContainer);
        rechercherEmploye = findViewById(R.id.rechercherEmploye);

        db = FirebaseFirestore.getInstance();

        // Charger les employés depuis Firebase
        chargerEmployes();

        // Filtrer les employés en tapant dans l'EditText
        rechercherEmploye.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) {
                filtrerEmployes(editable.toString());
            }
        });
    }

    private void chargerEmployes() {
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    employesList.clear();
                    itemsEmployeeCardsContainer.removeAllViews();

                    if (!querySnapshot.isEmpty()) {
                        noEmployeeContainer.setVisibility(View.GONE);

                        for (DocumentSnapshot doc : querySnapshot) {
                            Employe employe = doc.toObject(Employe.class);
                            if (employe != null) {
                                employesList.add(employe);
                                ajouterCarteEmploye(employe);
                            }
                        }
                    } else {
                        noEmployeeContainer.setVisibility(View.VISIBLE);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(EmployeActivity.this, "Erreur chargement employés", Toast.LENGTH_SHORT).show()
                );
    }

    private void filtrerEmployes(String texteRecherche) {
        itemsEmployeeCardsContainer.removeAllViews();
        boolean trouve = false;

        for (Employe e : employesList) {
            String nomComplet = (e.getPrenom() + " " + e.getNom()).toLowerCase();
            if (nomComplet.contains(texteRecherche.toLowerCase())) {
                ajouterCarteEmploye(e);
                trouve = true;
            }
        }

        noEmployeeContainer.setVisibility(trouve ? View.GONE : View.VISIBLE);
    }

    private void ajouterCarteEmploye(Employe e) {
        View carte = LayoutInflater.from(this).inflate(R.layout.item_employee_card, itemsEmployeeCardsContainer, false);

        TextView nomComplet = carte.findViewById(R.id.nomComplet);
        TextView poste = carte.findViewById(R.id.poste);
        TextView departement = carte.findViewById(R.id.departement);
        TextView email = carte.findViewById(R.id.email);
        TextView telephone = carte.findViewById(R.id.telephone);
        TextView dateEmbauche = carte.findViewById(R.id.dateEmbauche);
        TextView soldeConge = carte.findViewById(R.id.soldeConge);
        TextView np = carte.findViewById(R.id.np);

        nomComplet.setText(e.getNomComplet());
        poste.setText(e.getPoste());
        departement.setText(e.getDepartement());
        email.setText(e.getEmail());
        telephone.setText(e.getTelephone());
        dateEmbauche.setText("embauché le " + e.getDateEmbauche());
        soldeConge.setText(e.getSoldeConge() + " jours");

        // Initiales
        String initials = "";
        if (e.getPrenom().length() > 0) initials += e.getPrenom().charAt(0);
        if (e.getNom().length() > 0) initials += e.getNom().charAt(0);
        np.setText(initials.toUpperCase());

        // Ajouter la carte dans le conteneur
        itemsEmployeeCardsContainer.addView(carte);
    }
}
