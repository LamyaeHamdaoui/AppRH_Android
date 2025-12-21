package com.example.rhapp;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private EditText emailBox, motDePasseBox;
    private Button connecteBtn, createAccBtn;
    private TextView forgottenPasswordBtn;
    private LinearLayout mainLayout; // On va utiliser un conteneur pour tout masquer

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        // --- ÉTAPE 1 : MASQUER L'INTERFACE IMMÉDIATEMENT ---
        if (mainLayout != null) mainLayout.setVisibility(View.GONE);

        // --- ÉTAPE 2 : VÉRIFICATION DE CONNEXION ---
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Utilisateur déjà loggé -> Vérification du rôle direct
            checkUserRoleAndRedirect(currentUser.getUid());
        } else {
            // Personne n'est connecté -> On montre enfin l'interface
            showLoginUI();
        }
    }

    private void initializeViews() {
        emailBox = findViewById(R.id.emailBox);
        motDePasseBox = findViewById(R.id.motDePasseBox);
        connecteBtn = findViewById(R.id.connecteBtn);
        createAccBtn = findViewById(R.id.createAccBtn);
        forgottenPasswordBtn = findViewById(R.id.forgottenPasswordBtn);

        // IMPORTANT : Essayez de trouver le layout parent (souvent un ScrollView ou LinearLayout)
        // Si vous n'avez pas d'ID sur le parent, utilisez le findViewById(android.R.id.content)
        // ou ajoutez android:id="@+id/main_container" dans votre XML
        mainLayout = findViewById(R.id.main_container);
    }

    private void showLoginUI() {
        if (mainLayout != null) mainLayout.setVisibility(View.VISIBLE);
        setupClickListeners();
        setupBackPressedHandler();
    }

    private void checkUserRoleAndRedirect(String userId) {
        db.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            executeRedirection(role);
                        } else {
                            // Erreur : User Auth existe mais pas dans Firestore
                            mAuth.signOut();
                            showLoginUI();
                        }
                    } else {
                        // Erreur réseau ou autre
                        mAuth.signOut();
                        showLoginUI();
                    }
                });
    }

    private void executeRedirection(String role) {
        if (role == null) {
            mAuth.signOut();
            showLoginUI();
            return;
        }

        Intent intent;
        if (role.equalsIgnoreCase("rh")) {
            intent = new Intent(MainActivity.this, AcceuilRhActivity.class);
        } else {
            intent = new Intent(MainActivity.this, AcceuilEmployeActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- LE RESTE DU CODE (SANS MODIFICATION D'IDS) ---

    private void loginUser() {
        String email = emailBox.getText().toString().trim();
        String password = motDePasseBox.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        connecteBtn.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid());
                    } else {
                        connecteBtn.setEnabled(true);
                        Toast.makeText(this, "Échec de connexion", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        connecteBtn.setOnClickListener(v -> loginUser());
        createAccBtn.setOnClickListener(v -> startActivity(new Intent(this, CreateAccActivity.class)));
        forgottenPasswordBtn.setOnClickListener(v -> startActivity(new Intent(this, ForgottenPasswordActivity.class)));
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { finishAffinity(); }
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Email invalide"); return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            motDePasseBox.setError("Mot de passe trop court"); return false;
        }
        return true;
    }
}