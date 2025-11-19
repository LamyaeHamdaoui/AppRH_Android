package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull; // Import manquant ajouté
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View; // Import manquant ajouté
import android.widget.*;

// Imports Firebase ajoutés
import com.example.rhapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat; // Import manquant ajouté
import java.util.Date; // Import manquant ajouté
import java.util.Locale; // Import manquant ajouté
import java.util.Random;

public class CreateAccActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccActivity";

    private EditText nomEditText, prenomEditText, birthDateEditText;
    private EditText emailEditText, motDePasseEditText, confirmerMDPEditText;
    private RadioGroup radioGroupSexe, radioGroupRole;
    private Button valideCreateAcc;
    private TextView connecterInterface;

    // Champs Firebase ajoutés
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acc);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(CreateAccActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void initializeViews() {
        nomEditText = findViewById(R.id.nom);
        prenomEditText = findViewById(R.id.prenom);
        birthDateEditText = findViewById(R.id.dateNaissance);
        emailEditText = findViewById(R.id.email);
        motDePasseEditText = findViewById(R.id.motDePasse);
        confirmerMDPEditText = findViewById(R.id.confirmerMDP);

        radioGroupSexe = findViewById(R.id.radioGroup);
        radioGroupRole = findViewById(R.id.radioGroupRole);

        valideCreateAcc = findViewById(R.id.valideCreateAcc);
        connecterInterface = findViewById(R.id.connecterInterface);
    }

    private void setupClickListeners() {
        // Le bouton de validation appelle maintenant directement la fonction de création de compte
        valideCreateAcc.setOnClickListener(v -> validateInputsAndCreateAccount());
        connecterInterface.setOnClickListener(v -> {
            startActivity(new Intent(CreateAccActivity.this, MainActivity.class));
            finish();
        });
    }

    /**
     * Valide les entrées et, si tout est correct, crée le compte Firebase.
     */
    private void validateInputsAndCreateAccount() {

        String nom = nomEditText.getText().toString().trim();
        String prenom = prenomEditText.getText().toString().trim();
        String birthDate = birthDateEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = motDePasseEditText.getText().toString().trim();
        String confirmPassword = confirmerMDPEditText.getText().toString().trim();

        String sexe = getSelected(radioGroupSexe);
        String role = getSelected(radioGroupRole);

        Log.d(TAG, "Tentative de création de compte: Email=" + email + ", Rôle=" + role);

        // Validation
        if (!validate(nom, prenom, birthDate, sexe, role, email, password, confirmPassword)) {
            Log.e(TAG, "Validation locale échouée. Arrêt de la création.");
            return;
        }

        // --- Validation locale réussie, on passe à la vérification de l'email dans la DB ---
        validateEmployeeEmailAndCreateAccount(nom, prenom, birthDate, sexe, role, email, password);

    }

    /**
     * Crée l'utilisateur dans Firebase Authentication et sauvegarde ses données dans Realtime Database.
     */
    private void createFirebaseAccount(String nom, String prenom, String birthDate, String sexe,
                                       String role, String email, String password) {

        valideCreateAcc.setText("Création du compte...");
        valideCreateAcc.setEnabled(false);

        // 1. Création du compte Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            /// ///////////////////////
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {

                                                Toast.makeText(CreateAccActivity.this,
                                                        "Email de vérification envoyé ! Veuillez vérifier votre boîte.",
                                                        Toast.LENGTH_LONG).show();

                                                // Sauvegarde dans la DB
                                                saveUserToDatabase(user.getUid(), nom, prenom, birthDate, sexe, role, email);

                                                // Déconnexion obligatoire
                                                FirebaseAuth.getInstance().signOut();

                                                // Redirection vers la page de connexion
                                                redirectToMainActivity();
                                            }
                                        });
                            }

                            /// ///////////////////
                            if (user != null) {
                                // 2. Sauvegarder les informations supplémentaires, incluant le RÔLE
                                saveUserToDatabase(user.getUid(), nom, prenom, birthDate, sexe, role, email);

                                Toast.makeText(CreateAccActivity.this,
                                        "Compte créé avec succès!",
                                        Toast.LENGTH_SHORT).show();

                                // 3. Rediriger vers l'écran de connexion (MainActivity)
                                redirectToMainActivity();
                            }
                        } else {
                            // Échec de création (ex: email déjà utilisé)
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                            Toast.makeText(CreateAccActivity.this,
                                    "Erreur de création de compte: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur de création Firebase: " + errorMessage);

                            // Réactiver le bouton
                            valideCreateAcc.setText("Suivant");
                            valideCreateAcc.setEnabled(true);
                        }
                    }
                });
    }


    private void validateEmployeeEmailAndCreateAccount(String nom, String prenom, String birthDate,
                                                       String sexe, String role, String email, String password) {
        mDatabase.child("employees").orderByChild("email").equalTo(email)
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Erreur lors de la vérification de l'employé", Toast.LENGTH_LONG).show();
                    } else {
                        if (task.getResult().exists()) {
                            // L'email existe dans la base des employés → on peut créer le compte
                            createFirebaseAccount(nom, prenom, birthDate, sexe, role, email, password);
                        } else {
                            // L'email n'existe pas → on bloque la création
                            emailEditText.setError("Cet email n'est pas associé à un employé.");
                            emailEditText.requestFocus();
                            Toast.makeText(this, "Vous ne pouvez pas créer de compte avec cet email.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Sauvegarde les informations de l'utilisateur (incluant le rôle) dans la base de données Realtime.
     */
    private void saveUserToDatabase(String userId, String nom, String prenom, String birthDate,
                                    String sexe, String role, String email) {
        String createdAt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        // Créer l'objet User avec le rôle
        User user = new User(nom, prenom, birthDate, sexe, role, email, createdAt);

        // Sauvegarder dans la base de données Realtime Database sous la branche "users"
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
        Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
        // Ces drapeaux empêchent le retour à l'écran d'inscription
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }


    /**
     * Récupère la valeur texte du RadioButton sélectionné dans un RadioGroup.
     * Renvoie une chaîne vide si rien n'est sélectionné.
     */
    private String getSelected(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        // Vérifie si un bouton est sélectionné (-1 sinon)
        if (id != -1) {
            RadioButton selectedRadioButton = findViewById(id);
            return selectedRadioButton.getText().toString();
        } else {
            return "";
        }
    }

    /**
     * Contient toute la logique de validation des champs.
     */
    private boolean validate(String nom, String prenom, String birthDate, String sexe, String role,
                             String email, String password, String confirmPassword) {

        if (nom.isEmpty()) { nomEditText.setError("Champ obligatoire"); nomEditText.requestFocus(); return false; }
        if (prenom.isEmpty()) { prenomEditText.setError("Champ obligatoire"); prenomEditText.requestFocus(); return false; }
        // Ajout d'une simple vérification pour le format de date (non vide)
        if (birthDate.isEmpty()) { birthDateEditText.setError("Champ obligatoire (JJ/MM/AAAA)"); birthDateEditText.requestFocus(); return false; }

        if (sexe.isEmpty()) {
            Toast.makeText(this, "Sélectionnez le sexe", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (role.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un rôle", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Email invalide ou manquant");
            emailEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            motDePasseEditText.setError("Minimum 6 caractères");
            motDePasseEditText.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmerMDPEditText.setError("Ne correspond pas au mot de passe");
            confirmerMDPEditText.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Cette méthode n'est plus utilisée car la vérification par email est désactivée.
     * Elle est conservée uniquement pour éviter les erreurs de compilation si elle est référencée ailleurs.
     */
    private String generateCode() {
        // Génère un nombre entre 1000 et 9999
        return String.valueOf(1000 + new Random().nextInt(9000));
    }
}