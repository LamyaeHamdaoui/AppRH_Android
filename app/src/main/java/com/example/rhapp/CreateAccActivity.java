package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.*;

import com.example.rhapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import nécessaire
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;

public class CreateAccActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccActivity";

    // Références aux vues
    private EditText nomEditText, prenomEditText, birthDateEditText;
    private EditText emailEditText, motDePasseEditText, confirmerMDPEditText;
    private RadioGroup radioGroupSexe;
    private Button valideCreateAcc;
    private TextView connecterInterface;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Constantes
    // Suppression de DEFAULT_ROLE car le rôle sera récupéré de Firestore
    // private static final String DEFAULT_ROLE = "employe";
    private static final String EMPLOYEE_REFERENCE_COLLECTION = "employees";
    private static final String USERS_COLLECTION = "Users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acc);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupBackPressed();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        nomEditText = findViewById(R.id.nom);
        prenomEditText = findViewById(R.id.prenom);
        birthDateEditText = findViewById(R.id.dateNaissance);
        emailEditText = findViewById(R.id.email);
        motDePasseEditText = findViewById(R.id.motDePasse);
        confirmerMDPEditText = findViewById(R.id.confirmerMDP);
        radioGroupSexe = findViewById(R.id.radioGroup);
        valideCreateAcc = findViewById(R.id.valideCreateAcc);
        connecterInterface = findViewById(R.id.connecterInterface);
        progressBar = findViewById(R.id.progressBar);

        // Masquer la progressBar initialement
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.GONE);
        }
    }

    private void setupClickListeners() {
        valideCreateAcc.setOnClickListener(v -> validateInputsAndCheckExistence());
        connecterInterface.setOnClickListener(v -> navigateToMainActivity());
    }

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMainActivity();
            }
        });
    }

    private void validateInputsAndCheckExistence() {
        // Récupération des valeurs
        final String nom = nomEditText.getText().toString().trim();
        final String prenom = prenomEditText.getText().toString().trim();
        final String birthDateString = birthDateEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim().toLowerCase();
        final String password = motDePasseEditText.getText().toString().trim();
        final String confirmPassword = confirmerMDPEditText.getText().toString().trim();
        final String sexe = getSelectedSexe();

        // Validation des champs
        if (!validateFields(nom, prenom, birthDateString, sexe, email, password, confirmPassword)) {
            return;
        }

        // Conversion de la date
        final Date birthDate = parseBirthDate(birthDateString);
        if (birthDate == null) {
            return;
        }

        // Vérification de l'âge (optionnel)
        if (!isValidAge(birthDate)) {
            birthDateEditText.setError("Vous devez avoir au moins 16 ans");
            birthDateEditText.requestFocus();
            return;
        }

        // Démarrer le processus de création
        startAccountCreationProcess(nom, prenom, birthDate, sexe, email, password);
    }

    private boolean validateFields(String nom, String prenom, String birthDateString,
                                   String sexe, String email, String password, String confirmPassword) {

        // Validation du nom
        if (nom.isEmpty()) {
            nomEditText.setError("Le nom est obligatoire");
            nomEditText.requestFocus();
            return false;
        }

        // Validation du prénom
        if (prenom.isEmpty()) {
            prenomEditText.setError("Le prénom est obligatoire");
            prenomEditText.requestFocus();
            return false;
        }

        // Validation de la date de naissance
        if (birthDateString.isEmpty()) {
            birthDateEditText.setError("La date de naissance est obligatoire");
            birthDateEditText.requestFocus();
            return false;
        }

        // Validation du sexe
        if (sexe.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner votre sexe", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validation de l'email
        if (email.isEmpty()) {
            emailEditText.setError("L'email est obligatoire");
            emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Format d'email invalide");
            emailEditText.requestFocus();
            return false;
        }

        // Validation du mot de passe
        if (password.isEmpty()) {
            motDePasseEditText.setError("Le mot de passe est obligatoire");
            motDePasseEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            motDePasseEditText.setError("Le mot de passe doit contenir au moins 6 caractères");
            motDePasseEditText.requestFocus();
            return false;
        }

        // Validation de la confirmation du mot de passe
        if (!password.equals(confirmPassword)) {
            confirmerMDPEditText.setError("Les mots de passe ne correspondent pas");
            confirmerMDPEditText.requestFocus();
            return false;
        }

        return true;
    }

    private Date parseBirthDate(String birthDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);

        try {
            return sdf.parse(birthDateString);
        } catch (ParseException e) {
            Log.e(TAG, "Erreur de parsing de la date: ", e);
            birthDateEditText.setError("Format invalide. Utilisez JJ/MM/AAAA");
            birthDateEditText.requestFocus();
            return null;
        }
    }

    private boolean isValidAge(Date birthDate) {
        Date currentDate = new Date();
        long ageInMillis = currentDate.getTime() - birthDate.getTime();
        long ageInYears = ageInMillis / (1000L * 60 * 60 * 24 * 365);
        return ageInYears >= 16;
    }

    /**
     * Vérifie si l'email existe dans la collection des employés et récupère le rôle.
     */
    private void startAccountCreationProcess(String nom, String prenom, Date birthDate,
                                             String sexe, String email, String password) {

        showLoading(true);

        // 1. Vérifier si l'email existe dans la collection des employés ET RÉCUPÉRER LE RÔLE
        db.collection(EMPLOYEE_REFERENCE_COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Email non trouvé dans la base des employés
                            showLoading(false);
                            showError("Email non autorisé. Contactez l'administration.");
                            Log.w(TAG, "Tentative d'inscription avec email non autorisé: " + email);
                        } else {
                            // Email trouvé. Récupération du rôle
                            DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                            String role = doc.getString("role"); // Assurez-vous que le champ "role" existe dans 'employees'

                            if (role == null || role.isEmpty()) {
                                showLoading(false);
                                showError("Rôle non défini dans la base de référence (employees). Contactez l'administration.");
                                Log.e(TAG, "Rôle manquant pour l'email: " + email);
                                return;
                            }

                            // Email et Rôle récupérés, on procède à la création du compte
                            Log.d(TAG, "Email autorisé et Rôle récupéré (" + role + "). Création du compte...");
                            createFirebaseAccount(nom, prenom, birthDate, sexe, email, password, role);
                        }
                    } else {
                        // Erreur de requête
                        showLoading(false);
                        showError("Erreur de vérification. Réessayez.");
                        Log.e(TAG, "Erreur Firestore: ", task.getException());
                    }
                });
    }

    /**
     * Crée l'utilisateur dans Firebase Auth.
     * Ajout du paramètre 'role'.
     */
    private void createFirebaseAccount(String nom, String prenom, Date birthDate,
                                       String sexe, String email, String password, final String role) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Passer le rôle au moment de la sauvegarde
                            saveUserToFirestore(firebaseUser.getUid(), nom, prenom, birthDate, sexe, email, role);
                        }
                    } else {
                        handleAuthError(task.getException());
                    }
                });
    }

    /**
     * Sauvegarde l'utilisateur dans la collection finale 'Users' avec le rôle correct.
     * Ajout du paramètre 'role'.
     */
    private void saveUserToFirestore(String userId, String nom, String prenom, Date birthDate,
                                     String sexe, String email, String role) { // Ajout du paramètre 'role'

        Timestamp createdAt = new Timestamp(new Date());
        // Utilisation du rôle récupéré au lieu de DEFAULT_ROLE
        User user = new User(nom, prenom, birthDate, sexe, role, email, createdAt);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Utilisateur sauvegardé dans Firestore avec rôle " + role + ": " + userId);
                        // IMPORTANT : Déconnexion après la création pour forcer le login
                        mAuth.signOut();
                        showSuccessAndRedirect();
                    } else {
                        Log.e(TAG, "Erreur sauvegarde Firestore: ", task.getException());
                        showError("Erreur lors de la sauvegarde des données");
                    }
                });
    }

    private void handleAuthError(Exception exception) {
        showLoading(false);

        if (exception instanceof FirebaseAuthUserCollisionException) {
            showError("Cet email est déjà utilisé. Connectez-vous.");
            navigateToMainActivity();
        } else {
            String errorMessage = exception != null ? exception.getMessage() : "Erreur inconnue";
            showError("Erreur de création: " + errorMessage);
            Log.e(TAG, "Erreur Auth: ", exception);
        }
    }

    private String getSelectedSexe() {
        int selectedId = radioGroupSexe.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            return selectedRadioButton.getText().toString();
        }
        return "";
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        valideCreateAcc.setEnabled(!show);
        valideCreateAcc.setText(show ? "Création en cours..." : "Valider");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccessAndRedirect() {
        // Le message est mis à jour pour indiquer de se connecter, car l'utilisateur est déconnecté juste avant
        Toast.makeText(this, "Compte créé avec succès! Veuillez vous connecter.", Toast.LENGTH_LONG).show();
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}