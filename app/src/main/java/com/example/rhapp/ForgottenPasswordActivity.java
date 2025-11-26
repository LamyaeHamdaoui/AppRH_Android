package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class ForgottenPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgottenPassword";

    // Vues
    private EditText emailEditText;
    private Button suivantButton;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotten_password); // Assurez-vous que le nom du fichier est correct

        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupClickListener();
    }

    private void initializeViews() {
        // L'EditText n'avait pas d'ID, je suppose que l'ID est "emailInput" pour le retrouver
        // Vous DEVEZ ajouter l'ID à votre EditText dans le XML si ce n'est pas déjà fait.
        emailEditText = findViewById(R.id.emailInput);
        suivantButton = findViewById(R.id.suivant1);
    }

    private void setupClickListener() {
        suivantButton.setOnClickListener(v -> attemptResetPassword());
    }

    private void attemptResetPassword() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Veuillez entrer votre adresse email.");
            return;
        }

        // Simuler le chargement (désactiver le bouton)
        suivantButton.setEnabled(false);

        // --- Logique Firebase : Envoi de l'e-mail de réinitialisation ---
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    suivantButton.setEnabled(true); // Réactiver le bouton

                    if (task.isSuccessful()) {
                        // L'e-mail a été envoyé (ou le sera, Firebase confirme juste la requête)
                        Toast.makeText(ForgottenPasswordActivity.this,
                                "Un e-mail de réinitialisation a été envoyé à " + email,
                                Toast.LENGTH_LONG).show();

                        // Récupérer l'e-mail et passer à l'activité de vérification/confirmation
                        navigateToVerificationCode(email);
                    } else {
                        // Gérer les erreurs (ex: email non trouvé, mauvaise format)
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Échec de l'envoi de l'e-mail.";

                        Toast.makeText(ForgottenPasswordActivity.this,
                                "Erreur: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToVerificationCode(String email) {
        Intent intent = new Intent(ForgottenPasswordActivity.this, MainActivity.class);

        // Passer l'email à l'activité suivante pour référence
        intent.putExtra("USER_EMAIL", email);

        startActivity(intent);
        finish(); // Optionnel: Fermer cette activité pour éviter le retour arrière
    }
}