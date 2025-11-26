package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SecurityActivity extends AppCompatActivity {

    private static final String TAG = "SecurityActivity";
    private static final String EMPLOYEES_COLLECTION = "employees";

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
    private LinearLayout layoutOtherSession;
    private TextView tvLogoutOther;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String employeeDocumentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initializeViews();
        setupSpinner();
        setupClickListeners();

        if (currentUser != null) {
            loadSecurityData();
        } else {
            Toast.makeText(this, "Veuillez vous connecter pour accéder aux paramètres de sécurité.", Toast.LENGTH_LONG).show();
            finish();
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
        layoutOtherSession = findViewById(R.id.layoutOtherSession);
        tvLogoutOther = findViewById(R.id.tvLogoutOther);
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
                saveProfileVisibility(selectedVisibility);
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
            handleTwoFactorToggle(isChecked);
        });

        // 3. Exporter les données
        layoutDataExport.setOnClickListener(v -> handleDataExport());

        // 4. Supprimer le compte
        layoutDeleteAccount.setOnClickListener(v -> handleDeleteAccount());

        // 5. Déconnecter toutes les sessions
        btnLogoutAll.setOnClickListener(v -> handleLogoutAllSessions());

        // 6. Déconnecter une session spécifique
        tvLogoutOther.setOnClickListener(v -> handleLogoutOtherSession());
    }

    // --- Logique d'action des clics ---

    private void handleChangePassword() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    private void handleTwoFactorToggle(boolean isEnabled) {
        if (employeeDocumentId == null) {
            Toast.makeText(this, "Profil non chargé. Réessayez.", Toast.LENGTH_SHORT).show();
            switchTwoFactor.setChecked(!isEnabled); // Revert the switch
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("twoFactorEnabled", isEnabled);

        db.collection(EMPLOYEES_COLLECTION).document(employeeDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = isEnabled ?
                            "Authentification à 2 facteurs activée" :
                            "Authentification à 2 facteurs désactivée";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur mise à jour 2FA:", e);
                    Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                    switchTwoFactor.setChecked(!isEnabled); // Revert on failure
                });
    }

    private void handleDataExport() {
        new AlertDialog.Builder(this)
                .setTitle("Exporter les données")
                .setMessage("Voulez-vous exporter toutes vos données personnelles ? Un fichier CSV sera généré et envoyé à votre adresse email.")
                .setPositiveButton("Exporter", (dialog, which) -> {
                    exportUserData();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void handleDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le compte")
                .setMessage("Êtes-vous sûr de vouloir supprimer définitivement votre compte ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    deleteUserAccount();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void handleLogoutAllSessions() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion globale")
                .setMessage("Voulez-vous vous déconnecter de tous les appareils ?")
                .setPositiveButton("Déconnecter", (dialog, which) -> {
                    logoutAllSessions();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void handleLogoutOtherSession() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous déconnecter la session iPhone 13 ?")
                .setPositiveButton("Déconnecter", (dialog, which) -> {
                    // Simuler la déconnexion de l'autre session
                    layoutOtherSession.setVisibility(View.GONE);
                    Toast.makeText(this, "Session déconnectée", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // --- Méthodes de traitement ---

    private void saveProfileVisibility(String visibility) {
        if (employeeDocumentId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileVisibility", visibility);

        db.collection(EMPLOYEES_COLLECTION).document(employeeDocumentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Visibilité mise à jour: " + visibility);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur mise à jour visibilité:", e);
                    Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                });
    }

    private void exportUserData() {
        Toast.makeText(this, "Génération et envoi du rapport en cours...", Toast.LENGTH_LONG).show();

        // ⭐ TODO: Implémenter la logique d'exportation réelle
        // - Récupérer toutes les données de l'utilisateur depuis Firestore
        // - Générer un fichier CSV/JSON
        // - Envoyer par email ou permettre le téléchargement
    }

    private void deleteUserAccount() {
        if (currentUser == null || employeeDocumentId == null) return;

        // ⭐ ATTENTION: Cette opération est critique
        // 1. Supprimer les données Firestore
        db.collection(EMPLOYEES_COLLECTION).document(employeeDocumentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Supprimer le compte Firebase Auth
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Compte supprimé avec succès", Toast.LENGTH_SHORT).show();
                                    // Rediriger vers l'écran de connexion
                                    Intent intent = new Intent(this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Erreur lors de la suppression du compte", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur suppression données:", e);
                    Toast.makeText(this, "Erreur lors de la suppression des données", Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutAllSessions() {
        // Firebase Auth ne supporte pas directement la déconnexion de toutes les sessions
        // Cette méthode déconnecte seulement l'appareil actuel
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion effectuée", Toast.LENGTH_SHORT).show();

        // Rediriger vers l'écran de connexion
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- Logique de chargement des données ---

    /**
     * Charge les données de sécurité de l'utilisateur depuis Firestore
     */
    private void loadSecurityData() {
        String userEmail = currentUser.getEmail();
        if (userEmail == null) return;

        db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("email", userEmail.toLowerCase().trim())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        employeeDocumentId = document.getId();
                        displaySecurityData(document);
                    } else {
                        Toast.makeText(this, "Profil employé non trouvé", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement données sécurité:", e);
                    Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Affiche les données de sécurité dans l'interface
     */
    private void displaySecurityData(DocumentSnapshot document) {
        // Date dernière modification mot de passe
        Object lastPasswordChange = document.get("lastPasswordChange");
        if (lastPasswordChange instanceof com.google.firebase.Timestamp) {
            com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) lastPasswordChange;
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            tvLastPasswordChange.setText(sdf.format(timestamp.toDate()));
        } else {
            // Valeur par défaut si non disponible
            tvLastPasswordChange.setText("Non disponible");
        }

        // État 2FA
        Boolean twoFactorEnabled = document.getBoolean("twoFactorEnabled");
        if (twoFactorEnabled != null) {
            switchTwoFactor.setChecked(twoFactorEnabled);
        }

        // Visibilité du profil
        String visibility = document.getString("profileVisibility");
        if (visibility != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerVisibility.getAdapter();
            int position = adapter.getPosition(visibility);
            if (position >= 0) {
                spinnerVisibility.setSelection(position);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les données si nécessaire
        if (currentUser != null) {
            loadSecurityData();
        }
    }
}