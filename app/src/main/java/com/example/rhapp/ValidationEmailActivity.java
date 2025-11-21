package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.*;

import com.example.rhapp.model.User;
import com.google.firebase.auth.*;
// Remplacement des imports de Realtime Database par Firestore
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ValidationEmailActivity extends AppCompatActivity {

    private static final String TAG = "ValidationActivity";

    private EditText code1, code2, code3, code4;
    private Button suivantBtn;

    private String expectedCode;
    private String userNom, userPrenom, userSexe, userRole, userEmail, userPassword;
    private Date userBirthDate; // java.util.Date

    private FirebaseAuth mAuth;
    // Remplacement de Realtime Database par Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        mAuth = FirebaseAuth.getInstance();
        // Initialisation de Firestore
        db = FirebaseFirestore.getInstance();

        retrieveData();

        if (!isDataValid()) {
            Toast.makeText(this, "Erreur : données manquantes. Retour à la connexion.", Toast.LENGTH_LONG).show();
            // Délai de redirection pour que le Toast soit visible
            new android.os.Handler().postDelayed(this::finish, 3000);
            return;
        }

        initializeViews();
        setupCodeInputSystem();

        // Affichage du code pour les tests, à retirer en production!
        Log.d(TAG, "Code attendu: " + expectedCode);
        Toast.makeText(this, "Code : " + expectedCode, Toast.LENGTH_LONG).show();

        suivantBtn.setOnClickListener(v -> validateCode());
    }

    private void retrieveData() {
        Intent i = getIntent();
        userNom = i.getStringExtra("NOM");
        userPrenom = i.getStringExtra("PRENOM");

        // CORRECTION : Assignation directe à la variable de classe (membre)
        userBirthDate = (Date) i.getSerializableExtra("NAISS");

        userSexe = i.getStringExtra("SEXE");
        userRole = i.getStringExtra("ROLE");
        userEmail = i.getStringExtra("EMAIL");
        userPassword = i.getStringExtra("PASSWORD");
        expectedCode = i.getStringExtra("CODE");
    }

    private boolean isDataValid() {
        return userNom != null && userPrenom != null && userBirthDate != null &&
                userSexe != null && userRole != null &&
                userEmail != null && userPassword != null &&
                expectedCode != null;
    }

    private void initializeViews() {
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        suivantBtn = findViewById(R.id.suivant2);
    }

    private void setupCodeInputSystem() {
        EditText[] fields = {code1, code2, code3, code4};

        for (int i = 0; i < fields.length; i++) {
            int index = i;

            fields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < 3) {
                        fields[index + 1].requestFocus();
                    }
                }
            });

            fields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        fields[index].getText().length() == 0 &&
                        index > 0) {

                    // Efface le champ précédent et déplace le focus
                    fields[index - 1].setText("");
                    fields[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private void validateCode() {
        String entered =
                code1.getText().toString() +
                        code2.getText().toString() +
                        code3.getText().toString() +
                        code4.getText().toString();

        if (entered.length() != 4) {
            Toast.makeText(this, "Code incomplet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!entered.equals(expectedCode)) {
            Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        createFirebaseAccount();
    }

    private void createFirebaseAccount() {
        suivantBtn.setEnabled(false);
        suivantBtn.setText("Création...");

        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        if (fbUser != null) {
                            saveUserToDatabase(fbUser.getUid());
                        } else {
                            // Cas étrange où Auth réussit mais getCurrentUser est null
                            handleAuthError("Création de compte réussie mais utilisateur introuvable.");
                        }
                    }
                    else {
                        handleAuthError(task.getException());
                    }
                });
    }

    private void handleAuthError(Exception e) {
        suivantBtn.setEnabled(true);
        suivantBtn.setText("Suivant");

        String message = "Erreur de création du compte.";

        if (e instanceof FirebaseAuthUserCollisionException) {
            message = "Cet email est déjà utilisé. Veuillez vous connecter.";
        } else if (e != null) {
            message = e.getMessage();
            Log.e(TAG, "Erreur Auth: ", e);
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleAuthError(String msg) {
        suivantBtn.setEnabled(true);
        suivantBtn.setText("Suivant");
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Erreur Auth: " + msg);
    }


    /**
     * Sauvegarde les informations utilisateur dans la collection Firestore "Users".
     */
    private void saveUserToDatabase(String uid) {

        // CORRECTION 1 : Création du Timestamp Firebase
        Date now = new Date();
        Timestamp createdAt = new Timestamp(now);

        User user = new User(
                userNom,
                userPrenom,
                userBirthDate,
                userSexe,
                userRole,
                userEmail,
                createdAt // Passage du Timestamp
        );

        // CORRECTION 2 : Utilisation de Firestore (db) et de la collection "Users"
        db.collection("Users").document(uid).set(user)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Compte créé avec succès", Toast.LENGTH_LONG).show();
                    } else {
                        // Si l'écriture DB échoue, on doit idéalement supprimer l'utilisateur Auth
                        Log.e(TAG, "Erreur lors de la sauvegarde Firestore: ", task.getException());
                        Toast.makeText(this, "Erreur lors de la sauvegarde des données utilisateur. Réessayez.", Toast.LENGTH_LONG).show();
                    }

                    // Déconnexion de l'utilisateur après l'inscription
                    mAuth.signOut();

                    // Redirection vers l'activité principale de connexion
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
    }
}