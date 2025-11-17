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
import android.widget.TextView;
import android.widget.Toast;

import com.example.rhapp.AcceuilRhActivity;
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

public class MainActivity extends AppCompatActivity {

    // Déclaration des variables
    private EditText emailBox, motDePasseBox;
    private Button connecteBtn, createAccBtn;
    private TextView forgottenPasswordBtn;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialiser les vues
        initializeViews();

        // Vérifier si l'utilisateur est déjà connecté
        checkCurrentUser();

        // Configurer les écouteurs de clics
        setupClickListeners();

        // Gestion moderne du back button
        setupBackPressedHandler();
    }

    private void initializeViews() {
        emailBox = findViewById(R.id.emailBox);
        motDePasseBox = findViewById(R.id.motDePasseBox);
        connecteBtn = findViewById(R.id.connecteBtn);
        createAccBtn = findViewById(R.id.createAccBtn);
        forgottenPasswordBtn = findViewById(R.id.forgottenPasswordBtn);
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // L'utilisateur est déjà connecté, rediriger vers l'activité principale
            redirectToHomeActivity();
        }
    }

    private void setupClickListeners() {
        // Bouton de connexion
        connecteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Bouton de création de compte
        createAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });

        // Mot de passe oublié
        forgottenPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Fermer l'application
                finishAffinity();
            }
        });
    }

    private void loginUser() {
        String email = emailBox.getText().toString().trim();
        String password = motDePasseBox.getText().toString().trim();

        // Validation des champs
        if (!validateInputs(email, password)) {
            return;
        }

        // Afficher un loading
        connecteBtn.setText("Connexion...");
        connecteBtn.setEnabled(false);

        // Connexion avec Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Connexion réussie
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Vérifier si l'email est vérifié (optionnel)
                                if (user.isEmailVerified()) {
                                    Toast.makeText(MainActivity.this, "Connexion réussie!",
                                            Toast.LENGTH_SHORT).show();
                                    redirectToHomeActivity();
                                } else {
                                    // Si vous voulez forcer la vérification d'email
                                    Toast.makeText(MainActivity.this,
                                            "Veuillez vérifier votre email",
                                            Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            // Échec de connexion
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("invalid credential") || errorMessage.contains("wrong password")) {
                                Toast.makeText(MainActivity.this,
                                        "Email ou mot de passe incorrect",
                                        Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("user not found")) {
                                Toast.makeText(MainActivity.this,
                                        "Aucun compte trouvé avec cet email",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Erreur: " + errorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        // Réactiver le bouton
                        connecteBtn.setText("Se connecter");
                        connecteBtn.setEnabled(true);
                    }
                });
    }

    private void createNewAccount() {
        // Rediriger vers l'activité de création de compte
        Intent intent = new Intent(MainActivity.this, CreateAccActivity.class);
        startActivity(intent);
    }

    private void saveUserToDatabase(String userId, String email) {
        String createdAt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        // Créer un objet utilisateur avec email et date de création
        User user = new User();
        user.setEmail(email);
        user.setCreatedAt(createdAt);
        user.setNom("");
        user.setPrenom("");
        user.setBirthDate("");
        user.setSexe("");

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

    private void resetPassword() {
        String email = emailBox.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailBox.setError("Entrez votre email");
            emailBox.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Email invalide");
            emailBox.requestFocus();
            return;
        }

        // Envoyer l'email de réinitialisation
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this,
                                    "Email de réinitialisation envoyé!",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Erreur: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        // Validation email
        if (TextUtils.isEmpty(email)) {
            emailBox.setError("L'email est requis");
            emailBox.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Format d'email invalide");
            emailBox.requestFocus();
            return false;
        }

        // Validation mot de passe
        if (TextUtils.isEmpty(password)) {
            motDePasseBox.setError("Le mot de passe est requis");
            motDePasseBox.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            motDePasseBox.setError("Le mot de passe doit contenir au moins 6 caractères");
            motDePasseBox.requestFocus();
            return false;
        }

        return true;
    }

    private void redirectToHomeActivity() {
        Intent intent = new Intent(MainActivity.this, AcceuilRhActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier à nouveau si l'utilisateur est connecté
        checkCurrentUser();
    }
}