package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rhapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Déclaration des variables de l'interface
    private EditText emailBox, motDePasseBox;
    private Button connecteBtn, createAccBtn;
    private TextView forgottenPasswordBtn;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assurez-vous que R.layout.activity_main est le nom correct de votre fichier XML
        setContentView(R.layout.activity_main);

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialiser les vues
        initializeViews();

        // Configurer les écouteurs de clics
        setupClickListeners();

        // Gestion du back button pour quitter l'application
        setupBackPressedHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier si l'utilisateur est déjà connecté au début de l'activité
        checkCurrentUser();
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
            Log.d(TAG, "Utilisateur déjà connecté: " + currentUser.getEmail());
            // L'utilisateur est déjà connecté, on le redirige directement
            redirectToHomeActivity();
        }
    }

    private void setupClickListeners() {
        // 1. Bouton de connexion
        connecteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // 2. Bouton de création de compte (vers CreateAccActivity)
        createAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers l'activité de création de compte
                Intent intent = new Intent(MainActivity.this, CreateAccActivity.class);
                startActivity(intent);
            }
        });

        // 3. Mot de passe oublié (vers ForgottenPasswordActivity)
        forgottenPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers l'activité de mot de passe oublié
                Intent intent = new Intent(MainActivity.this, ForgottenPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Fermer l'application entièrement
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

        // Afficher un état de chargement
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

                            // Optionnel: Vérifier si l'email est vérifié
                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(MainActivity.this, "Connexion réussie!",
                                        Toast.LENGTH_SHORT).show();
                                redirectToHomeActivity();
                            } else {
                                // Si la vérification est requise, déconnecter l'utilisateur
                                Toast.makeText(MainActivity.this,
                                        "Veuillez vérifier votre email et vous reconnecter.",
                                        Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            // Échec de connexion : Utilisation des exceptions spécifiques
                            String displayMessage = "Erreur de connexion.";
                            Exception exception = task.getException();

                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                displayMessage = "Aucun compte trouvé avec cet email.";
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                displayMessage = "Email ou mot de passe incorrect.";
                            } else if (exception != null) {
                                displayMessage = "Erreur: " + exception.getMessage();
                                Log.e(TAG, "Erreur de connexion Firebase: ", exception); // Log détaillé
                            }

                            Toast.makeText(MainActivity.this, displayMessage, Toast.LENGTH_LONG).show();
                        }

                        // Réactiver le bouton
                        connecteBtn.setText("Se connecter");
                        connecteBtn.setEnabled(true);
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
        // FLAG_ACTIVITY_CLEAR_TASK s'assure qu'on ne puisse pas revenir à l'écran de connexion
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // NOTE: saveUserToDatabase a été retirée de MainActivity, car elle est uniquement utilisée
    // dans CreateAccActivity pour stocker les détails après l'enregistrement.
}