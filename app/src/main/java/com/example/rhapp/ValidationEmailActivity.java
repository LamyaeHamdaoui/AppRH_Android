package com.example.rhapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.rhapp.model.User; // Assurez-vous que cette classe existe pour le modèle utilisateur
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ValidationEmailActivity extends AppCompatActivity {

    private static final String TAG = "ValidationEmailActivity";

    // Déclaration des champs basés sur les IDs du XML (qui doivent être code1, code2, etc.)
    private EditText code1, code2, code3, code4;
    private Button suivantBtn; // Correspond à l'ID 'suivant2' dans votre XML

    // Données reçues de l'activité précédente (inscription)
    private String expectedCode;
    private String userEmail;
    private String userPassword;
    private String userNom, userPrenom, userBirthDate, userSexe;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assurez-vous que votre fichier XML est correctement nommé ici
        setContentView(R.layout.activity_verification_code);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        retrieveDataFromIntent();
        initializeViews();
        setupInputListeners();

        // Afficher le code pour les tests (simule l'envoi d'email, à retirer en production)
        Toast.makeText(this, "Code à utiliser: " + expectedCode, Toast.LENGTH_LONG).show();

        // Bouton Suivant (Valider)
        suivantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateCodeAndCreateAccount();
            }
        });
    }

    /**
     * Récupère les données utilisateur et le code de vérification transmis par Intent.
     */
    private void retrieveDataFromIntent() {
        Intent intent = getIntent();
        userNom = intent.getStringExtra("EXTRA_NOM");
        userPrenom = intent.getStringExtra("EXTRA_PRENOM");
        userBirthDate = intent.getStringExtra("EXTRA_DATE_NAISSANCE");
        userSexe = intent.getStringExtra("EXTRA_SEXE");
        userEmail = intent.getStringExtra("EXTRA_EMAIL");
        userPassword = intent.getStringExtra("EXTRA_PASSWORD");
        expectedCode = intent.getStringExtra("EXTRA_VERIFICATION_CODE");

        if (userEmail == null || userPassword == null || expectedCode == null) {
            Log.e(TAG, "Données de vérification manquantes. Retour à l'inscription.");
            Toast.makeText(this, "Erreur de données. Veuillez réessayer l'inscription.", Toast.LENGTH_LONG).show();
            // Rediriger ou fermer si les données sont incomplètes
            finish();
        }
    }

    /**
     * Lie les vues du layout aux variables Java.
     */
    private void initializeViews() {
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        suivantBtn = findViewById(R.id.suivant2); // ID du bouton 'Suivant'
    }

    /**
     * Gère le passage automatique au champ suivant et le retour arrière.
     */
    private void setupInputListeners() {
        EditText[] codeFields = new EditText[]{code1, code2, code3, code4};

        for (int i = 0; i < codeFields.length; i++) {
            final int index = i;
            // 1. Gère le passage automatique au champ suivant
            codeFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < codeFields.length - 1) {
                        codeFields[index + 1].requestFocus();
                    }
                }
            });

            // 2. Gère le retour au champ précédent (backspace sur un champ vide)
            codeFields[i].setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (codeFields[index].getText().length() == 0 && index > 0) {
                            codeFields[index - 1].requestFocus();
                            // Efface l'entrée précédente
                            codeFields[index - 1].setText("");
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Valide le code entré par l'utilisateur et, si valide, crée le compte Firebase.
     */
    private void validateCodeAndCreateAccount() {
        // Concaténer les 4 chiffres
        String enteredCode = code1.getText().toString() +
                code2.getText().toString() +
                code3.getText().toString() +
                code4.getText().toString();

        if (enteredCode.length() != 4) {
            Toast.makeText(this, "Veuillez entrer le code complet à 4 chiffres.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredCode.equals(expectedCode)) {
            // Code valide : Procéder à la création du compte Firebase
            createFirebaseAccount(userEmail, userPassword);
        } else {
            // Code invalide
            Toast.makeText(this, "Code invalide. Veuillez réessayer.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Crée l'utilisateur dans Firebase Authentication et sauvegarde ses données.
     */
    private void createFirebaseAccount(String email, String password) {
        suivantBtn.setText("Création du compte...");
        suivantBtn.setEnabled(false);

        // 1. Création du compte Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // 2. Sauvegarder les informations supplémentaires
                                saveUserToDatabase(user.getUid(), userNom, userPrenom, userBirthDate, userSexe, email);

                                Toast.makeText(ValidationEmailActivity.this,
                                        "Compte créé avec succès!",
                                        Toast.LENGTH_SHORT).show();

                                // 3. Rediriger vers l'écran de connexion (MainActivity)
                                redirectToMainActivity();
                            }
                        } else {
                            // Échec de création (ex: email déjà utilisé)
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                            Toast.makeText(ValidationEmailActivity.this, "Erreur de création de compte: " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur de création Firebase: " + errorMessage);

                            suivantBtn.setText("Suivant");
                            suivantBtn.setEnabled(true);
                        }
                    }
                });
    }

    /**
     * Sauvegarde les informations de l'utilisateur dans la base de données Realtime.
     */
    private void saveUserToDatabase(String userId, String nom, String prenom, String birthDate,
                                    String sexe, String email) {
        String createdAt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        // Assurez-vous que la classe 'User' a un constructeur correspondant
        User user = new User(nom, prenom, birthDate, sexe, email, createdAt);

        // Sauvegarder dans la base de données Realtime Database
        mDatabase.child("users").child(userId).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Utilisateur sauvegardé dans la base de données (ID: " + userId + ")");
                        } else {
                            Log.e(TAG, "Erreur sauvegarde dans Firebase DB: ", task.getException());
                        }
                    }
                });
    }

    /**
     * Redirige vers MainActivity et vide la pile d'activités.
     */
    private void redirectToMainActivity() {
        Intent intent = new Intent(ValidationEmailActivity.this, MainActivity.class);
        // Ces drapeaux empêchent le retour à l'écran d'inscription/validation
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }
}