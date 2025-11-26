package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

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
import com.google.firebase.firestore.FieldValue; // Importation nécessaire pour serverTimestamp()

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
    private LinearLayout layoutDeleteAccount;


    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String employeeDocumentId; // Stocke l'ID du document Firestore

    // Code de requête pour identifier le retour de ChangePasswordActivity
    private static final int REQUEST_CODE_CHANGE_PASSWORD = 1001;

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
        layoutDeleteAccount = findViewById(R.id.layoutDeleteAccount);

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

        // L'écouteur est laissé tel quel pour ne pas interférer avec le chargement initial
        spinnerVisibility.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // On s'assure que le document ID est disponible avant de sauvegarder
                if (employeeDocumentId != null) {
                    String selectedVisibility = parent.getItemAtPosition(position).toString();
                    saveProfileVisibility(selectedVisibility);
                }
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


        // 4. Supprimer le compte
        layoutDeleteAccount.setOnClickListener(v -> handleDeleteAccount());

    }

    // --- Logique d'action des clics ---

    private void handleChangePassword() {
        if (employeeDocumentId == null) {
            Toast.makeText(this, "Veuillez attendre le chargement du profil.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChangePasswordActivity.class);
        intent.putExtra("EMPLOYEE_DOC_ID", employeeDocumentId);
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


    // --- Méthodes de traitement ---

    /**
     * Logique à inclure dans ChangePasswordActivity après un changement réussi.
     * Cette méthode utilise FieldValue.serverTimestamp() pour une précision maximale.
     * @param documentId L'ID du document de l'employé à mettre à jour.
     */
    public static void updateLastPasswordChangeTimestampInFirestore(String documentId) {
        if (documentId == null) {
            Log.e(TAG, "Impossible de mettre à jour la date: documentId est null.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        // Utilisez FieldValue.serverTimestamp() pour enregistrer l'heure exacte du serveur.
        updates.put("lastPasswordChange", FieldValue.serverTimestamp());

        db.collection(EMPLOYEES_COLLECTION).document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Date de modification du mot de passe mise à jour dans Firestore.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la mise à jour de la date:", e);
                });
    }

    /**
     * Déclenche le processus d'exportation des données.
     * Cette logique doit appeler un service backend (comme Firebase Cloud Functions)
     * pour effectuer le traitement de données et l'envoi d'email.
     */
    private void exportUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non authentifié.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ----------------------------------------------------------------------------------
        // ⭐ ÉTAPE CLÉ MANQUANTE : Appel à Firebase Cloud Functions
        // ----------------------------------------------------------------------------------

        // C'est ici que vous devriez appeler votre fonction cloud (par exemple,
        // "exportDataAndSendEmail") en lui passant l'UID de l'utilisateur.
        // Exemple (nécessite l'initialisation de FirebaseFunctions):
        // FirebaseFunctions.getInstance()
        //      .getHttpsCallable("exportDataAndSendEmail")
        //      .call(currentUser.getUid())
        //      .addOnSuccessListener(...)
        //      .addOnFailureListener(...)

        // Simulation : Affichage d'un message pour l'utilisateur
        Toast.makeText(this,
                "Génération des données lancée. Veuillez vérifier votre boîte email (" + currentUser.getEmail() + ") dans les minutes qui suivent.",
                Toast.LENGTH_LONG).show();

        Log.i(TAG, "Processus d'exportation déclenché pour l'utilisateur: " + currentUser.getUid());

        // ----------------------------------------------------------------------------------
    }

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
                        employeeDocumentId = document.getId(); // Stocke l'ID
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
            // Format d'affichage corrigé pour inclure les secondes (HH:mm:ss)
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy 'à' HH:mm:ss", Locale.getDefault());
            tvLastPasswordChange.setText(sdf.format(timestamp.toDate()));
        } else {
            tvLastPasswordChange.setText("Jamais modifié ou non enregistré");
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
        // C'est ce onResume() qui est crucial : il recharge les données après le retour de
        // ChangePasswordActivity, affichant ainsi la date de modification mise à jour.
        if (currentUser != null) {
            loadSecurityData();
        }
    }
}