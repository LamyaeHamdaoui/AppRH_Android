package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.*;

import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private EditText emailBox, motDePasseBox;
    private Button connecteBtn, createAccBtn;
    private TextView forgottenPasswordBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();
        setupBackPressedHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // MODIFICATION: Suppression de la vérification d'email.
        // Si l'utilisateur est déjà connecté, on le redirige immédiatement.
        if (currentUser != null) {
            redirectToHomeActivity();
        }
    }

    private void initializeViews() {
        emailBox = findViewById(R.id.emailBox);
        motDePasseBox = findViewById(R.id.motDePasseBox);
        connecteBtn = findViewById(R.id.connecteBtn);
        createAccBtn = findViewById(R.id.createAccBtn);
        forgottenPasswordBtn = findViewById(R.id.forgottenPasswordBtn);
    }

    private void setupClickListeners() {

        connecteBtn.setOnClickListener(v -> loginUser());

        createAccBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateAccActivity.class));
        });

        forgottenPasswordBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ForgottenPasswordActivity.class));
        });
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Ferme l'application
                finishAffinity();
            }
        });
    }

    private void loginUser() {

        String email = emailBox.getText().toString().trim();
        String password = motDePasseBox.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        connecteBtn.setEnabled(false);
        connecteBtn.setText("Connexion...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    connecteBtn.setEnabled(true);
                    connecteBtn.setText("Se connecter");

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        // MODIFICATION: Suppression de la vérification d'email ici aussi.
                        // On considère la connexion réussie dès que l'authentification est OK.
                        if (user != null) {
                            Toast.makeText(MainActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                            redirectToHomeActivity();
                        } else {
                            // Cas très rare où l'authentification réussit mais user est null
                            Toast.makeText(MainActivity.this, "Erreur utilisateur. Réessayez.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }

                    } else {

                        String message = "Erreur de connexion.";
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthInvalidUserException) {
                            message = "Aucun utilisateur trouvé pour cet email.";
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            message = "Email ou mot de passe incorrect.";
                        } else if (e != null) {
                            message = e.getMessage();
                        }

                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            emailBox.setError("L'email est requis");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Email invalide");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            motDePasseBox.setError("Mot de passe requis");
            return false;
        }

        if (password.length() < 6) {
            motDePasseBox.setError("Minimum 6 caractères");
            return false;
        }

        return true;
    }

    private void redirectToHomeActivity() {
        Intent intent = new Intent(MainActivity.this, AcceuilRhActivity.class);
        // Utilisation des flags pour empêcher le retour à l'écran de connexion
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}