package com.example.rhapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.model.Employe;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmployeActivity extends AppCompatActivity {

    private LinearLayout itemsEmployeeCardsContainer, noEmployeeContainer, footer;
    private EditText rechercherEmploye;
    private Spinner departement;
    private Button btnAddEmploye;
    private ListenerRegistration registration;

    private FrameLayout fragmentContainer;
    private ScrollView mainContent;
    private RelativeLayout relative;
    private TextView actif;

    private FirebaseFirestore db;

    // ---- THREAD SECONDARY ----
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
        executor.shutdown();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employe);

        initViews();
        gererNavigationFooter();
        styliserSpinnerRecherche();
        configurerRecherche();
        configurerFiltreDepartement();
        chargerEmployes();



        btnAddEmploye.setOnClickListener(v -> ouvrirFragmentAddEmploye());

        // Gestion bouton retour
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fragmentContainer.getVisibility() == View.VISIBLE) {
                    getSupportFragmentManager().popBackStack();
                    retourDepuisFragment();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    // ---------------------------------------------------------
    //              INITIALISATION
    // ---------------------------------------------------------
    private void initViews() {
        itemsEmployeeCardsContainer = findViewById(R.id.itemsEmployeeCardsContainer);
        noEmployeeContainer = findViewById(R.id.noEmployeeContainer);
        rechercherEmploye = findViewById(R.id.rechercherEmploye);
        departement = findViewById(R.id.departement);
        btnAddEmploye = findViewById(R.id.btnAddEmploye);
        fragmentContainer = findViewById(R.id.fragment_container);
        mainContent = findViewById(R.id.main_content);
        //relative = findViewById(R.id.relative);
        footer = findViewById(R.id.footer);
        actif = findViewById(R.id.employesActifs);

        db = FirebaseFirestore.getInstance();
    }

    // ---------------------------------------------------------
    //              OUVERTURE FRAGMENTS
    // ---------------------------------------------------------
    private void ouvrirFragmentAddEmploye() {
        cacherUI();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AddEmployeFragment())
                .addToBackStack("add_employe")
                .commit();
    }

    private void ouvrirFragmentEditEmploye(String employeId) {
        cacherUI();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, EditEmployeFragment.newInstance(employeId))
                .addToBackStack("edit_employe")
                .commit();
    }

    public void retourDepuisFragment() {
        fragmentContainer.setVisibility(View.GONE);
        mainContent.setVisibility(View.VISIBLE);
        //relative.setVisibility(View.VISIBLE);
        footer.setVisibility(View.VISIBLE);
    }

    private void cacherUI() {
        mainContent.setVisibility(View.GONE);
        //relative.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------
    //              CHARGEMENT EMPLOYÉS (THREAD)
    // ---------------------------------------------------------

    private void chargerEmployes() {

        registration = db.collection("employees")
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null || querySnapshot == null) {
                        return;
                    }

                    runOnUiThread(() -> {
                        itemsEmployeeCardsContainer.removeAllViews();

                        if (querySnapshot.isEmpty()) {
                            actif.setText("0 employé actif");
                            noEmployeeContainer.setVisibility(View.VISIBLE);
                            return;
                        }

                        actif.setText(querySnapshot.size() + " employés actifs");
                        noEmployeeContainer.setVisibility(View.GONE);
                        itemsEmployeeCardsContainer.setVisibility(View.VISIBLE);

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Employe emp = doc.toObject(Employe.class);
                            emp.setId(doc.getId());

                            View card = getLayoutInflater().inflate(R.layout.item_employee_card, null);
                            setCardInfo(card, emp);
                            itemsEmployeeCardsContainer.addView(card);
                        }
                    });
                });
    }
    // ---------------------------------------------------------
    //              CARTE EMPLOYÉ
    // ---------------------------------------------------------
    private void setCardInfo(View card, Employe emp) {
        TextView np = card.findViewById(R.id.np);
        TextView nomComplet = card.findViewById(R.id.nomComplet);
        TextView poste = card.findViewById(R.id.poste);
        TextView departementTv = card.findViewById(R.id.departement);
        TextView email = card.findViewById(R.id.email);
        TextView telephone = card.findViewById(R.id.telephone);
        TextView dateEmbauche = card.findViewById(R.id.dateEmbauche);
        //TextView soldeConge = card.findViewById(R.id.soldeConge);

        np.setText(emp.getPrenom().charAt(0) + "" + emp.getNom().charAt(0));
        nomComplet.setText(emp.getNomComplet());
        poste.setText(emp.getPoste());
        departementTv.setText(emp.getDepartement());
        email.setText(emp.getEmail());
        telephone.setText(emp.getTelephone());

        if (emp.getDateEmbauche() != null) {
            String d = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(emp.getDateEmbauche().toDate());
            dateEmbauche.setText("Embauché le " + d);
        } else {
            dateEmbauche.setText("Date inconnue");
        }

        //
        // soldeConge.setText(emp.getSoldeConge() + " jours");

        card.findViewById(R.id.editEmploye).setOnClickListener(v -> ouvrirFragmentEditEmploye(emp.getId()));
        card.findViewById(R.id.deleteEmploye).setOnClickListener(v -> supprimerEmploye(emp.getId()));
    }

    // ---------------------------------------------------------
    //              SUPPRESSION EMPLOYÉ + THREAD
    // ---------------------------------------------------------
    private void supprimerEmploye(String id) {

        TextView title = new TextView(this);
        title.setText("Confirmer la suppression");
        title.setPadding(50, 40, 50, 40);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        title.setTextSize(20f);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(title);
        builder.setMessage("Voulez-vous vraiment supprimer cet employé ?");

        builder.setPositiveButton("Oui", (dialog, which) -> executor.execute(() -> {
            db.collection("employees").document(id).delete()
                    .addOnSuccessListener(unused -> runOnUiThread(() -> {
                        Toast.makeText(this, "Employé supprimé", Toast.LENGTH_SHORT).show();
                        chargerEmployes();
                    }));
        }));

        builder.setNegativeButton("Non", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // ---- STYLE BUTTONS ----
        Button btnYes = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button btnNo  = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Fond du popup blanc
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        // Bouton OUI (rouge)
        btnYes.setTextColor(Color.RED);

        // Bouton NON (bleu)
        btnNo.setTextColor(Color.BLUE);



    }

    // ---------------------------------------------------------
    //              RECHERCHE + DÉPARTEMENT (THREAD)
    // ---------------------------------------------------------
    private void configurerRecherche() {
        rechercherEmploye.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                filtrerEmployes();
            }
        });
    }

    private void configurerFiltreDepartement() {
        departement.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filtrerEmployes();
            }
        });
    }

    // ----------- FILTER THREAD -----------
    private void filtrerEmployes() {
        executor.execute(() -> db.collection("employees").get()
                .addOnSuccessListener(query -> runOnUiThread(() -> {
                    String rechercheTxt = rechercherEmploye.getText().toString().trim().toLowerCase();
                    String depSel = departement.getSelectedItem().toString();

                    // Vider la vue
                    itemsEmployeeCardsContainer.removeAllViews();

                    boolean hasMatches = false;

                    for (QueryDocumentSnapshot doc : query) {
                        Employe emp = doc.toObject(Employe.class);
                        emp.setId(doc.getId());

                        if (matchFiltre(emp, rechercheTxt, depSel)) {
                            hasMatches = true;
                            View card = getLayoutInflater().inflate(R.layout.item_employee_card, null);
                            setCardInfo(card, emp);
                            itemsEmployeeCardsContainer.addView(card);
                        }
                    }

                    // Gérer l'affichage quand aucun résultat
                    if (hasMatches) {
                        noEmployeeContainer.setVisibility(View.GONE);
                        itemsEmployeeCardsContainer.setVisibility(View.VISIBLE);
                    } else {
                        noEmployeeContainer.setVisibility(View.VISIBLE);
                        itemsEmployeeCardsContainer.setVisibility(View.GONE);
                    }
                })));
    }

    private boolean matchFiltre(Employe e, String txt, String dep) {

        boolean okTexte =
                txt.isEmpty() ||
                        e.getNomComplet().toLowerCase().contains(txt) ||
                        e.getEmail().toLowerCase().contains(txt) ||
                        e.getPoste().toLowerCase().contains(txt);

        boolean okDep =
                dep.equals("Tous les départements") ||
                        e.getDepartement().equals(dep);

        return okTexte && okDep;
    }

    private void styliserSpinnerRecherche() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.departements_recherche,
                R.layout.spinner_dropdown_item
        );
        departement.setAdapter(adapter);
    }

    private void gererNavigationFooter() {
        findViewById(R.id.reunions).setOnClickListener(v ->
                startActivity(new Intent(this, reunionActivity.class)));

        findViewById(R.id.conges).setOnClickListener(v ->
                startActivity(new Intent(this, CongesActivity.class)));

        findViewById(R.id.Acceuil).setOnClickListener(v ->
                startActivity(new Intent(this, AcceuilRhActivity.class)));

        findViewById(R.id.profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

}
