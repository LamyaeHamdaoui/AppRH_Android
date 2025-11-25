package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class SecurityActivity extends AppCompatActivity {

    private static final String TAG = "SecurityActivity";

    // --- Vues de la section Mot de passe ---
    private LinearLayout layoutChangePassword;
    private TextView tvLastPasswordChange;

    // --- Vues de la section Authentification ---
    private Switch switchTwoFactor;

    // --- Vues de la section Confidentialité ---
    private Spinner spinnerVisibility;
    private LinearLayout layoutDataExport;
    private LinearLayout layoutDeleteAccount;

    // --- Vues de la section Sessions actives ---
    private Button btnLogoutAll;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initializeViews();
        setupSpinner();
        setupClickListeners();

        // 💡 Si l'utilisateur est connecté, on peut charger ses données réelles
        if (currentUser != null) {
            loadSecurityData();
        } else {
            // Gérer le cas où l'utilisateur n'est pas connecté
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show();
            // Optionnel : rediriger vers l'écran de connexion
            // finish();
        }
    }

    private void initializeViews() {
        // Mot de passe
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        tvLastPasswordChange = findViewById(R.id.tvLastPasswordChange);

        // Authentification
        switchTwoFactor = findViewById(R.id.switchTwoFactor);

        // Confidentialité
        spinnerVisibility = findViewById(R.id.spinnerVisibility);
        layoutDataExport = findViewById(R.id.layoutDataExport);
        layoutDeleteAccount = findViewById(R.id.layoutDeleteAccount);

        // Sessions actives
        btnLogoutAll = findViewById(R.id.btnLogoutAll);
    }

    /**
     * Configure l'adaptateur et l'écouteur pour le Spinner de visibilité.
     */
    private void setupSpinner() {
        List<String> visibilityOptions = new ArrayList<>();
        visibilityOptions.add("Public");
        visibilityOptions.add("Privé (RH seulement)");
        visibilityOptions.add("Connexions seulement");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, visibilityOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerVisibility.setAdapter(adapter);

        spinnerVisibility.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVisibility = parent.getItemAtPosition(position).toString();
                // ⭐ TODO: Implémenter la logique de sauvegarde dans Firestore/Database
                Log.d(TAG, "Visibilité du profil sélectionnée: " + selectedVisibility);
                Toast.makeText(SecurityActivity.this, "Visibilité : " + selectedVisibility, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });
    }

    /**
     * Configure tous les écouteurs de clic et de changement d'état.
     */
    private void setupClickListeners() {
        // 1. Changer le mot de passe
        layoutChangePassword.setOnClickListener(v -> handleChangePassword());

        // 2. Switch Authentification à 2 facteurs (2FA)
        switchTwoFactor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // ⭐ TODO: Implémenter la logique d'activation/désactivation de la 2FA (peut nécessiter une nouvelle activité)
            if (isChecked) {
                Toast.makeText(this, "2FA activée. Configuration requise.", Toast.LENGTH_LONG).show();
                // Exemple: startActivity(new Intent(this, SetupTwoFactorActivity.class));
            } else {
                Toast.makeText(this, "2FA désactivée.", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Exporter les données
        layoutDataExport.setOnClickListener(v -> handleDataExport());

        // 4. Supprimer le compte
        layoutDeleteAccount.setOnClickListener(v -> handleDeleteAccount());

        // 5. Déconnecter toutes les sessions
        btnLogoutAll.setOnClickListener(v -> handleLogoutAllSessions());
    }

    // --- Logique d'action des clics ---

    private void handleChangePassword() {
        // ⭐ TODO: Naviguer vers l'activité de changement de mot de passe
        Toast.makeText(this, "Ouverture de l'écran de changement de mot de passe...", Toast.LENGTH_SHORT).show();
        // Exemple: startActivity(new Intent(this, ChangePasswordActivity.class));
    }

    private void handleDataExport() {
        // ⭐ TODO: Implémenter la logique d'exportation (générer un fichier CSV/JSON et l'envoyer par email ou le télécharger)
        Toast.makeText(this, "Lancement de l'exportation des données...", Toast.LENGTH_SHORT).show();
    }

    private void handleDeleteAccount() {
        // ⭐ TODO: Afficher une boîte de dialogue de confirmation et implémenter la logique de suppression du compte Firebase et Firestore
        Toast.makeText(this, "Ouverture de la boîte de dialogue de suppression de compte...", Toast.LENGTH_LONG).show();
    }

    private void handleLogoutAllSessions() {
        if (currentUser != null) {
            // ⭐ TODO: Ceci n'est pas directement supporté par Firebase Auth (signOut() ne déconnecte que l'appareil actuel).
            // Pour une vraie déconnexion de toutes les sessions, il faudrait utiliser l'API de gestion des sessions de l'Admin SDK
            // ou forcer le rafraîchissement du jeton de sécurité. Pour une simulation simple :
            mAuth.signOut();
            Toast.makeText(this, "Déconnexion de toutes les sessions (Cet appareil seulement pour le moment).", Toast.LENGTH_LONG).show();
            // Rediriger vers l'écran de connexion
            // startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // --- Logique de chargement des données (Simulée) ---

    /**
     * Charge les données de sécurité de l'utilisateur (2FA, dernière modification, etc.)
     */
    private void loadSecurityData() {
        // ⭐ TODO: Remplacer les données simulées par la récupération de données réelles depuis Firestore ou Realtime DB.

        // Simuler la dernière date de changement de mot de passe
        tvLastPasswordChange.setText("20 novembre 2025");

        // Simuler l'état du 2FA (e.g., récupérer 'isTwoFactorEnabled' de l'utilisateur)
        // switchTwoFactor.setChecked(true);

        // Simuler la visibilité actuelle
        // String currentVisibility = "Public";
        // spinnerVisibility.setSelection(adapter.getPosition(currentVisibility));
    }
}