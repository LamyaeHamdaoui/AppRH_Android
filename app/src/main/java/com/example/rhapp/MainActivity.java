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
import android.widget.*;

import com.google.firebase.auth.*;
// Importation de Firestore
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// Suppression des imports de Realtime Database
// import com.google.firebase.database.DatabaseReference;
// import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText emailBox, motDePasseBox;
    private Button connecteBtn, createAccBtn;
    private TextView forgottenPasswordBtn;

    private FirebaseAuth mAuth;
    // Remplacement de Realtime Database par Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        // Initialisation de Firestore
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupBackPressedHandler();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Si l'utilisateur est déjà connecté, on le redirige immédiatement.
        if (currentUser != null) {
            // NOTE: On ne peut pas vérifier le rôle ici sans bloquer l'UI,
            // on appelle directement la méthode qui va chercher le rôle et rediriger.
            checkUserRoleAndRedirect(currentUser.getUid());
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

                        if (user != null) {

                            // 1. Vérification OBLIGATOIRE de l'email
                            if (!user.isEmailVerified()) {

                                FirebaseAuth.getInstance().signOut();

                                Toast.makeText(MainActivity.this,
                                        "Veuillez vérifier votre email avant de vous connecter.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            // 2. Email vérifié, on récupère le rôle et on redirige
                            Toast.makeText(MainActivity.this,
                                    "Connexion réussie", Toast.LENGTH_SHORT).show();

                            checkUserRoleAndRedirect(user.getUid());

                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Erreur utilisateur. Réessayez.", Toast.LENGTH_LONG).show();
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
                            Log.e(TAG, "Erreur de connexion : " + message);
                        }

                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Récupère le rôle de l'utilisateur depuis Firestore et redirige vers l'activité appropriée.
     */
    private void checkUserRoleAndRedirect(String userId) {

        // 1. Récupérer le document utilisateur dans la collection "Users"
        db.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // 2. Récupérer la valeur du champ 'role'
                            String role = document.getString("role");
                            Log.d(TAG, "Rôle de l'utilisateur récupéré : " + role);

                            // 3. Redirection basée sur le rôle
                            redirectToActivity(role);

                        } else {
                            // Cas où l'utilisateur existe dans Auth mais pas dans Firestore (rare)
                            Toast.makeText(MainActivity.this,
                                    "Données utilisateur introuvables. Contactez l'administrateur.",
                                    Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            connecteBtn.setEnabled(true);
                        }
                    } else {
                        // Erreur de lecture de la base de données
                        Log.e(TAG, "Erreur de lecture Firestore: ", task.getException());
                        Toast.makeText(MainActivity.this,
                                "Erreur de base de données. Réessayez.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        connecteBtn.setEnabled(true);
                    }
                });
    }

    /**
     * Redirige l'utilisateur vers l'écran d'accueil basé sur son rôle.
     */
    private void redirectToActivity(String role) {
        Class<?> destinationActivity;

        if (role == null) {
            // Si le rôle est null ou non défini après la connexion
            Toast.makeText(MainActivity.this, "Rôle non défini.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            return;
        }

        switch (role.toLowerCase()) { // Utilisation de toLowerCase pour une meilleure robustesse
            case "admin":
                destinationActivity = AcceuilEmployeActivity.class;
                break;
            case "rh":

                destinationActivity = AcceuilRhActivity.class;
                break;
            // Ajoutez d'autres cas si vous avez plus de rôles (ex: "Employe", etc.)
            default:
                // Redirection par défaut (si le rôle n'est pas reconnu)
                destinationActivity = AcceuilRhActivity.class;
                Toast.makeText(MainActivity.this, "Rôle utilisateur par défaut appliqué.", Toast.LENGTH_SHORT).show();
                break;
        }

        Intent intent = new Intent(MainActivity.this, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    private boolean validateInputs(String email, String password) {
        // ... (Logique de validation inchangée, elle est correcte)
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
}