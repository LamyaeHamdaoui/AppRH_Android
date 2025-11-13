package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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

public class CreateAccActivity extends AppCompatActivity {

    // Déclaration des variables
    private EditText nomEditText, prenomEditText, birthDateEditText;
    private EditText emailEditText, motDePasseEditText, confirmerMDPEditText;
    private RadioGroup radioGroup;
    private Button valideCreateAcc;
    private TextView connecterInterface;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acc);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialiser les vues
        initializeViews();

        // Configurer les écouteurs de clics
        setupClickListeners();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Retour à MainActivity
                Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeViews() {
        nomEditText = findViewById(R.id.nom);
        prenomEditText = findViewById(R.id.prenom);
        birthDateEditText = findViewById(R.id.birthDate);
        emailEditText = findViewById(R.id.email);
        motDePasseEditText = findViewById(R.id.motDePasse);
        confirmerMDPEditText = findViewById(R.id.confirmerMDP);
        radioGroup = findViewById(R.id.radioGroup);
        valideCreateAcc = findViewById(R.id.valideCreateAcc);
        connecterInterface = findViewById(R.id.connecterInterface);
    }

    private void setupClickListeners() {
        // Bouton de validation
        valideCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });

        // Lien pour se connecter
        connecterInterface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retour à l'écran de connexion
                Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void createNewAccount() {
        // Récupérer les valeurs
        String nom = nomEditText.getText().toString().trim();
        String prenom = prenomEditText.getText().toString().trim();
        String birthDate = birthDateEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = motDePasseEditText.getText().toString().trim();
        String confirmPassword = confirmerMDPEditText.getText().toString().trim();

        // Récupérer le sexe
        String sexe = getSelectedSexe();

        // Validation des champs
        if (!validateInputs(nom, prenom, birthDate, email, password, confirmPassword, sexe)) {
            return;
        }

        // Afficher un loading
        valideCreateAcc.setText("Création en cours...");
        valideCreateAcc.setEnabled(false);

        // Créer un nouvel utilisateur avec Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Création réussie
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Sauvegarder les informations supplémentaires dans la base de données
                                saveUserToDatabase(user.getUid(), nom, prenom, birthDate, sexe, email);

                                // Envoyer l'email de vérification
                                sendEmailVerification(user);

                                Toast.makeText(CreateAccActivity.this,
                                        "Compte créé avec succès!",
                                        Toast.LENGTH_SHORT).show();

                                // Rediriger vers l'écran principal
                                redirectToHomeActivity();
                            }
                        } else {
                            // Échec de création
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("email address is already in use")) {
                                Toast.makeText(CreateAccActivity.this,
                                        "Cette adresse email est déjà utilisée",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(CreateAccActivity.this,
                                        "Erreur: " + errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        // Réactiver le bouton
                        valideCreateAcc.setText("Valider");
                        valideCreateAcc.setEnabled(true);
                    }
                });
    }

    private void saveUserToDatabase(String userId, String nom, String prenom, String birthDate,
                                    String sexe, String email) {
        // Obtenir la date actuelle
        String createdAt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        // Créer un objet utilisateur avec toutes les informations
        User user = new User(nom, prenom, birthDate, sexe, email, createdAt);

        // Sauvegarder dans la base de données
        mDatabase.child("users").child(userId).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            System.out.println("Utilisateur sauvegardé dans la base de données");
                        } else {
                            System.out.println("Erreur sauvegarde: " + task.getException());
                        }
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(CreateAccActivity.this,
                                    "Email de vérification envoyé!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    private String getSelectedSexe() {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton radioButton = findViewById(selectedId);
            return radioButton.getText().toString();
        }
        return "";
    }

    private boolean validateInputs(String nom, String prenom, String birthDate, String email,
                                   String password, String confirmPassword, String sexe) {
        // Validation nom
        if (TextUtils.isEmpty(nom)) {
            nomEditText.setError("Le nom est requis");
            nomEditText.requestFocus();
            return false;
        }

        // Validation prénom
        if (TextUtils.isEmpty(prenom)) {
            prenomEditText.setError("Le prénom est requis");
            prenomEditText.requestFocus();
            return false;
        }

        // Validation date de naissance
        if (TextUtils.isEmpty(birthDate)) {
            birthDateEditText.setError("La date de naissance est requise");
            birthDateEditText.requestFocus();
            return false;
        }

        // Validation sexe
        if (TextUtils.isEmpty(sexe)) {
            Toast.makeText(this, "Veuillez sélectionner votre sexe", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validation email
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("L'email est requis");
            emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Format d'email invalide");
            emailEditText.requestFocus();
            return false;
        }

        // Validation mot de passe
        if (TextUtils.isEmpty(password)) {
            motDePasseEditText.setError("Le mot de passe est requis");
            motDePasseEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            motDePasseEditText.setError("Le mot de passe doit contenir au moins 6 caractères");
            motDePasseEditText.requestFocus();
            return false;
        }

        // Validation confirmation mot de passe
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmerMDPEditText.setError("Veuillez confirmer votre mot de passe");
            confirmerMDPEditText.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmerMDPEditText.setError("Les mots de passe ne correspondent pas");
            confirmerMDPEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void redirectToHomeActivity() {
    Intent intent = new Intent(CreateAccActivity.this, AcceuilRhActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}