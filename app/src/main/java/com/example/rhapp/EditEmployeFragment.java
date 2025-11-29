package com.example.rhapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditEmployeFragment extends Fragment {

    private EditText nom, prenom, email, telephone, poste, soldeConge;
    private Spinner departement;
    private RadioButton radioEmploye, radioRh;

    private FirebaseFirestore db;
    private String employeId;
    private String role = "employe";

    // Thread secondaire
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public EditEmployeFragment() {}

    public static EditEmployeFragment newInstance(String employeId) {
        EditEmployeFragment fragment = new EditEmployeFragment();
        Bundle args = new Bundle();
        args.putString("employeId", employeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            employeId = getArguments().getString("employeId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_employe, container, false);

        initViews(v);
        styliserSpinner();
        chargerEmploye();

        return v;
    }

    private void initViews(View v) {
        nom = v.findViewById(R.id.nom);
        prenom = v.findViewById(R.id.prenom);
        email = v.findViewById(R.id.email);
        telephone = v.findViewById(R.id.telephone);
        poste = v.findViewById(R.id.poste);
        departement = v.findViewById(R.id.departement);
        soldeConge = v.findViewById(R.id.soldeConge);
        radioEmploye = v.findViewById(R.id.radioEmploye);
        radioRh = v.findViewById(R.id.radioRh);

        Button btnAnnuler = v.findViewById(R.id.btnAnnulerEditEmploye);
        Button btnEnregistrer = v.findViewById(R.id.btnenregistrerEditEmploye);

        btnAnnuler.setOnClickListener(view -> retourEcranPrincipal());
        btnEnregistrer.setOnClickListener(view -> enregistrerModifications());
    }

    private void retourEcranPrincipal() {
        if (getActivity() != null) getActivity().onBackPressed();
    }

    // --------------------------
    //  CHARGEMENT EMPLOYÉ
    // --------------------------
    private void chargerEmploye() {
        db.collection("employees").document(employeId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        nom.setText(doc.getString("nom"));
                        prenom.setText(doc.getString("prenom"));
                        email.setText(doc.getString("email"));
                        telephone.setText(doc.getString("telephone"));
                        poste.setText(doc.getString("poste"));
                        soldeConge.setText(String.valueOf(doc.getLong("soldeConge")));

                        role = doc.getString("role") != null ? doc.getString("role") : "employe";
                        if ("rh".equals(role)) radioRh.setChecked(true);
                        else radioEmploye.setChecked(true);

                        setSpinnerValue(departement, doc.getString("departement"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erreur chargement", Toast.LENGTH_SHORT).show());
    }

    // --------------------------
    //  ENREGISTRER MODIFICATIONS
    // --------------------------
    private void enregistrerModifications() {

        if (!verifierChamps()) return;

        executor.execute(() -> {

            role = radioEmploye.isChecked() ? "employe" : "rh";

            String nomStr = nom.getText().toString().trim();
            String prenomStr = prenom.getText().toString().trim();
            String emailStr = email.getText().toString().trim();
            String telStr = telephone.getText().toString().trim();
            String posteStr = poste.getText().toString().trim();
            String depStr = departement.getSelectedItem().toString();
            int solde = Integer.parseInt(soldeConge.getText().toString().trim());

            DocumentReference docRef = db.collection("employees").document(employeId);

            docRef.update(
                            "nom", nomStr,
                            "prenom", prenomStr,
                            "email", emailStr,
                            "telephone", telStr,
                            "poste", posteStr,
                            "departement", depStr,
                            "soldeConge", solde,
                            "role", role
                    ).addOnSuccessListener(unused -> afficherMessageEtRetour("Modifications enregistrées"))
                    .addOnFailureListener(e -> afficherMessage("Erreur lors de l'enregistrement"));
        });
    }

    // --------------------------
    //   UTILITAIRES
    // --------------------------

    private boolean verifierChamps() {
        if (TextUtils.isEmpty(nom.getText()) ||
                TextUtils.isEmpty(prenom.getText()) ||
                TextUtils.isEmpty(email.getText()) ||
                TextUtils.isEmpty(telephone.getText()) ||
                TextUtils.isEmpty(poste.getText()) ||
                TextUtils.isEmpty(soldeConge.getText())) {

            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void afficherMessage(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show()
        );
    }

    private void afficherMessageEtRetour(String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            retourEcranPrincipal();
        });
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void styliserSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.departements,
                R.layout.spinner_dropdown_item
        );
        departement.setAdapter(adapter);
    }
}
