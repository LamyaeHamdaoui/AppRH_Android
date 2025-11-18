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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rhapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random; // Import nécessaire pour générer le code

public class CreateAccActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccActivity";

    // Déclaration des variables de l'interface
    private EditText nomEditText, prenomEditText, birthDateEditText;
    private EditText emailEditText, motDePasseEditText, confirmerMDPEditText;
    private RadioGroup radioGroup;
    private Button valideCreateAcc;
    private TextView connecterInterface;

    // Firebase (utilisé seulement pour l'initialisation dans cette activité)
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_acc);

        // Initialiser Firebase (même si la création est reportée, l'initialisation est bonne)
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();

        // Gestion du back button pour retourner à l'écran de connexion
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Retourne à MainActivity
                Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeViews() {
        // IDs basés sur votre XML
        nomEditText = findViewById(R.id.nom);
        prenomEditText = findViewById(R.id.prenom);
        birthDateEditText = findViewById(R.id.dateNaissance);
        emailEditText = findViewById(R.id.email);
        motDePasseEditText = findViewById(R.id.motDePasse);
        confirmerMDPEditText = findViewById(R.id.confirmerMDP);
        radioGroup = findViewById(R.id.radioGroup);
        valideCreateAcc = findViewById(R.id.valideCreateAcc);
        connecterInterface = findViewById(R.id.connecterInterface);
    }

    private void setupClickListeners() {
        // Bouton de validation (génère le code et redirige)
        valideCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateInputsAndRedirect();
            }
        });

        // Lien pour se connecter (retourne à MainActivity)
        connecterInterface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void validateInputsAndRedirect() {
        // Récupérer et valider toutes les valeurs
        String nom = nomEditText.getText().toString().trim();
        String prenom = prenomEditText.getText().toString().trim();
        String birthDate = birthDateEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = motDePasseEditText.getText().toString().trim();
        String confirmPassword = confirmerMDPEditText.getText().toString().trim();
        String sexe = getSelectedSexe();

        if (!validateInputs(nom, prenom, birthDate, email, password, confirmPassword, sexe)) {
            return;
        }

        // --- Logique de Génération de Code ---

        // 1. Générer le code à 4 chiffres
        String verificationCode = generateVerificationCode();
        Log.d(TAG, "Code de vérification généré: " + verificationCode);
        Toast.makeText(this, "Code généré: " + verificationCode + " (Vérifiez le Logcat pour simuler l'envoi)", Toast.LENGTH_LONG).show();

        // 2. Préparer les données pour l'activité suivante
        Intent intent = new Intent(CreateAccActivity.this, ValidationEmailActivity.class);

        // IMPORTANT: Nous passons toutes les données (y compris le mot de passe)
        // car l'enregistrement Firebase sera effectué dans ValidationEmailActivity.
        intent.putExtra("EXTRA_NOM", nom);
        intent.putExtra("EXTRA_PRENOM", prenom);
        intent.putExtra("EXTRA_DATE_NAISSANCE", birthDate);
        intent.putExtra("EXTRA_SEXE", sexe);
        intent.putExtra("EXTRA_EMAIL", email);
        intent.putExtra("EXTRA_PASSWORD", password);
        intent.putExtra("EXTRA_VERIFICATION_CODE", verificationCode);

        // 3. Rediriger
        startActivity(intent);
        // Ne pas appeler finish() ici, pour permettre un retour si l'utilisateur annule la validation.
    }

    private String generateVerificationCode() {
        Random random = new Random();
        // Génère un nombre entre 1000 et 9999
        int code = 1000 + random.nextInt(9000);
        return String.valueOf(code);
    }

    // Reste des méthodes (getSelectedSexe, validateInputs) inchangé
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
}