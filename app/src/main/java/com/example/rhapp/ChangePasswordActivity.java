package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";
    private static final String EMPLOYEES_COLLECTION = "employees";

    // Vues
    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnCancel;
    private Button btnSave;
    private TextView tvForgotPassword;

    // Indicateurs de force du mot de passe
    private ProgressBar passwordStrengthBar;
    private TextView tvPasswordStrength;
    private TextView tvLengthRequirement;
    private TextView tvUppercaseRequirement;
    private TextView tvNumberRequirement;
    private TextView tvSpecialRequirement;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        // Champs de saisie
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Boutons
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Indicateurs de force
        passwordStrengthBar = findViewById(R.id.passwordStrengthBar);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvLengthRequirement = findViewById(R.id.tvLengthRequirement);
        tvUppercaseRequirement = findViewById(R.id.tvUppercaseRequirement);
        tvNumberRequirement = findViewById(R.id.tvNumberRequirement);
        tvSpecialRequirement = findViewById(R.id.tvSpecialRequirement);

        // Désactiver le bouton Sauvegarder par défaut
        btnSave.setEnabled(false);
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> attemptPasswordChange());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Écouteur pour la validation et la force du mot de passe
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordStrength(s.toString());
                checkValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Écouteur pour la confirmation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Écouteur pour le mot de passe actuel (pour réactiver la validation si l'actuel est saisi)
        etCurrentPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Vérifie la force du nouveau mot de passe et met à jour l'interface.
     */
    private void checkPasswordStrength(String password) {
        int score = 0;

        // 1. Longueur (min 8)
        boolean hasMinLength = password.length() >= 8;
        if (hasMinLength) score += 25;
        updateRequirement(tvLengthRequirement, hasMinLength, "Au moins 8 caractères");

        // 2. Majuscule
        boolean hasUppercase = Pattern.compile("[A-Z]").matcher(password).find();
        if (hasUppercase) score += 25;
        updateRequirement(tvUppercaseRequirement, hasUppercase, "Une lettre majuscule");

        // 3. Chiffre
        boolean hasNumber = Pattern.compile("[0-9]").matcher(password).find();
        if (hasNumber) score += 25;
        updateRequirement(tvNumberRequirement, hasNumber, "Un chiffre");

        // 4. Caractère spécial
        boolean hasSpecial = Pattern.compile("[^a-zA-Z0-9]").matcher(password).find();
        if (hasSpecial) score += 25;
        updateRequirement(tvSpecialRequirement, hasSpecial, "Un caractère spécial (optionnel)", true);

        // --- Mise à jour de la barre de progression ---
        passwordStrengthBar.setProgress(score);
        String strengthText;
        int color;

        if (password.isEmpty()) {
            strengthText = "Faible";
            color = ContextCompat.getColor(this, R.color.red); // Assumer que R.color.red existe
            passwordStrengthBar.setProgress(0);
        } else if (score < 50) {
            strengthText = "Faible";
            color = ContextCompat.getColor(this, R.color.red);
            passwordStrengthBar.setProgressTintList(ContextCompat.getColorStateList(this, R.color.red));
        } else if (score < 75) {
            strengthText = "Moyen";
            color = ContextCompat.getColor(this, R.color.orange); // Assumer que R.color.orange existe
            passwordStrengthBar.setProgressTintList(ContextCompat.getColorStateList(this, R.color.orange));
        } else {
            strengthText = "Fort";
            color = ContextCompat.getColor(this, R.color.green); // Assumer que R.color.green existe
            passwordStrengthBar.setProgressTintList(ContextCompat.getColorStateList(this, R.color.green));
        }

        tvPasswordStrength.setText(strengthText);
        tvPasswordStrength.setTextColor(color);
    }

    /**
     * Met à jour le TextView pour indiquer si l'exigence est satisfaite ou non.
     */
    private void updateRequirement(TextView textView, boolean isMet, String baseText) {
        updateRequirement(textView, isMet, baseText, false);
    }

    private void updateRequirement(TextView textView, boolean isMet, String baseText, boolean isOptional) {
        int color;
        String prefix;

        if (isMet) {
            prefix = "✓ ";
            // Si c'est un requis, on met la couleur verte, si c'est optionnel on la garde aussi.
            color = ContextCompat.getColor(this, R.color.green); // Assumer R.color.green
        } else {
            prefix = isOptional ? "✓ " : "✗ "; // L'optionnel est "coché" même s'il n'est pas rempli (pas bloquant)
            color = ContextCompat.getColor(this, isOptional ? R.color.green : R.color.red); // Assumer R.color.red et R.color.green
        }

        textView.setText(prefix + baseText);
        textView.setTextColor(color);
    }

    /**
     * Vérifie si toutes les conditions sont réunies pour activer le bouton "Enregistrer".
     */
    private void checkValidation() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Conditions minimales (doivent correspondre aux exigences non optionnelles de checkPasswordStrength)
        boolean hasMinLength = newPassword.length() >= 8;
        boolean hasUppercase = Pattern.compile("[A-Z]").matcher(newPassword).find();
        boolean hasNumber = Pattern.compile("[0-9]").matcher(newPassword).find();

        boolean isNewPasswordValid = hasMinLength && hasUppercase && hasNumber;

        boolean passwordsMatch = newPassword.equals(confirmPassword) && !newPassword.isEmpty();
        boolean currentPasswordEntered = !currentPassword.isEmpty();

        // Activer le bouton si toutes les validations passent
        btnSave.setEnabled(isNewPasswordValid && passwordsMatch && currentPasswordEntered);
    }


    // --- Logique Firebase ---

    private void attemptPasswordChange() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        // Les validations de base (champs vides, correspondance) sont gérées par checkValidation()

        if (newPassword.equals(currentPassword)) {
            Toast.makeText(this, "Le nouveau mot de passe doit être différent de l'actuel.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Démarrer le chargement
        showLoading(true);

        // 1. Ré-authentification (nécessaire pour la sécurité)
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Ré-authentification réussie. Tentative de mise à jour...");

                        // 2. Mise à jour du mot de passe
                        updatePassword(newPassword);

                    } else {
                        showLoading(false);
                        Toast.makeText(ChangePasswordActivity.this,
                                "Mot de passe actuel incorrect.",
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Ré-authentification échouée", task.getException());
                    }
                });
    }

    private void updatePassword(String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(ChangePasswordActivity.this, "Mot de passe changé avec succès!", Toast.LENGTH_LONG).show();

                        // 3. Mise à jour du timestamp de la dernière modification dans Firestore
                        updateLastPasswordChangeTimestamp();

                        finish(); // Fermer l'activité
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "Erreur lors du changement du mot de passe.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur mise à jour mot de passe", task.getException());
                    }
                });
    }

    private void updateLastPasswordChangeTimestamp() {
        if (user.getEmail() == null) return;

        String userEmail = user.getEmail().toLowerCase().trim();

        // Recherche du document de l'employé par email
        FirebaseFirestore.getInstance().collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String employeeDocumentId = querySnapshot.getDocuments().get(0).getId();

                        // Mise à jour du timestamp
                        FirebaseFirestore.getInstance().collection(EMPLOYEES_COLLECTION).document(employeeDocumentId)
                                .update("lastPasswordChange", Timestamp.now())
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Timestamp mis à jour."))
                                .addOnFailureListener(e -> Log.e(TAG, "Erreur mise à jour timestamp", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erreur recherche document employé pour timestamp", e));
    }

    /**
     * Gère l'action "Mot de passe oublié" en initiant la réinitialisation par email.
     */
    private void handleForgotPassword() {
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Impossible d'envoyer le lien, veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(user.getEmail())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Un lien de réinitialisation a été envoyé à " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                        finish(); // Fermer cette activité car l'utilisateur doit utiliser l'email
                    } else {
                        Toast.makeText(this, "Échec de l'envoi du lien de réinitialisation.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erreur envoi réinitialisation", task.getException());
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        // En l'absence d'une ProgressBar globale dans ce XML, nous désactivons les boutons
        btnSave.setEnabled(!isLoading);
        btnCancel.setEnabled(!isLoading);
        etCurrentPassword.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);

        // Si vous aviez ajouté une ProgressBar visible, vous la mettriez à jour ici.
        // Exemple: progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}