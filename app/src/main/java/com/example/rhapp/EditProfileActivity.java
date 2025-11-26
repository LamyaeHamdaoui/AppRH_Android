package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View; // Importation ajoutée pour View.GONE/VISIBLE
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final String EMPLOYEES_COLLECTION = "employees";

    // ⭐ Clé définie dans ProfileEmployeActivity pour recevoir l'autorisation d'édition
    private static final String IS_PROFESSIONAL_EDIT_ALLOWED = "IS_PROFESSIONAL_EDIT_ALLOWED";

    // ⭐ Variable pour stocker l'autorisation d'édition reçue
    private boolean isProfessionalEditAllowed = false;

    // Vues (Widgets)
    private Button btnCancel, btnSave;
    private EditText etFirstName, etLastName, etEmail, etPhone, etPosition, etDepartment, etHireDate;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private DocumentReference employeeRef;

    // Variables pour l'image
    private String employeeDocumentId;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter pour modifier le profil.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ⭐ ÉTAPE 1: Récupérer l'autorisation d'édition professionnelle
        if (getIntent().hasExtra(IS_PROFESSIONAL_EDIT_ALLOWED)) {
            // La valeur par défaut 'false' est une sécurité
            isProfessionalEditAllowed = getIntent().getBooleanExtra(IS_PROFESSIONAL_EDIT_ALLOWED, false);
        }

        // Initialisation du lanceur d'activité
        setupGalleryLauncher();

        initializeViews();

        // ⭐ ÉTAPE 2: Appliquer la restriction d'édition après l'initialisation des vues
        applyProfessionalEditRestriction();

        loadEmployeeData();
        setupClickListeners();
    }

    private void initializeViews() {
        // Informations personnelles
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        // Email désactivé (non modifiable)
        etEmail.setEnabled(false);

        // Informations professionnelles
        etPosition = findViewById(R.id.etPosition);
        etDepartment = findViewById(R.id.etDepartment);
        etHireDate = findViewById(R.id.etHireDate);

        // Boutons d'action
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
    }

    /**
     * ⭐ NOUVELLE MÉTHODE : Applique la restriction sur les champs professionnels
     * si l'Intent extra IS_PROFESSIONAL_EDIT_ALLOWED est false.
     */
    private void applyProfessionalEditRestriction() {
        if (!isProfessionalEditAllowed) {
            // Désactiver les champs pour l'employé ordinaire
            etPosition.setEnabled(false);
            etPosition.setFocusable(false);
            etPosition.setAlpha(0.7f); // Optionnel : griser légèrement pour l'UX

            etDepartment.setEnabled(false);
            etDepartment.setFocusable(false);
            etDepartment.setAlpha(0.7f);

            etHireDate.setEnabled(false);
            etHireDate.setFocusable(false);
            etHireDate.setAlpha(0.7f);

        }
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    // --- GESTION DE LA PHOTO DE PROFIL (Laissée pour complétude, même si les vues manquent) ---

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // ivProfilePhoto.setImageURI(selectedImageUri); // Décommenter si l'ImageView est ajoutée
                            uploadImageToFirebase(selectedImageUri);
                        }
                    } else {
                        Toast.makeText(this, "Sélection d'image annulée.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageToFirebase(Uri fileUri) {
        if (currentUser == null || employeeRef == null) {
            Toast.makeText(this, "Erreur de connexion ou de profil. Impossible de télécharger.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Téléchargement en cours...", Toast.LENGTH_LONG).show();

        String fileName = currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference profileImagesRef = storage.getReference()
                .child("profile_images/" + fileName);

        profileImagesRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profileImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateFirestoreProfilePhoto(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur de téléchargement de l'image:", e);
                    Toast.makeText(EditProfileActivity.this, "Échec du téléchargement de la photo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFirestoreProfilePhoto(String photoUrl) {
        employeeRef.update("photoUrl", photoUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Photo de profil mise à jour!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur mise à jour Firestore:", e);
                    Toast.makeText(EditProfileActivity.this, "Erreur mise à jour du profil photo dans la base de données.", Toast.LENGTH_SHORT).show();
                });
    }

    // --- LOGIQUE DE CHARGEMENT ET SAUVEGARDE ---

    private void loadEmployeeData() {
        String userEmail = currentUser.getEmail();
        if (userEmail == null) return;

        db.collection(EMPLOYEES_COLLECTION)
                .whereEqualTo("email", userEmail.toLowerCase(Locale.ROOT).trim())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        employeeDocumentId = document.getId();
                        employeeRef = document.getReference();

                        displayData(document);
                    } else {
                        Toast.makeText(this, "Profil employé non trouvé dans Firestore.", Toast.LENGTH_LONG).show();
                        displayDefaultAuthData(userEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du chargement des données:", e);
                    Toast.makeText(this, "Erreur de chargement des données.", Toast.LENGTH_SHORT).show();
                    displayDefaultAuthData(userEmail);
                });
    }

    private void displayData(DocumentSnapshot document) {
        // Informations personnelles
        etFirstName.setText(document.getString("prenom"));
        etLastName.setText(document.getString("nom"));
        etEmail.setText(document.getString("email"));
        etPhone.setText(document.getString("telephone"));

        // Informations professionnelles
        etPosition.setText(document.getString("poste"));
        etDepartment.setText(document.getString("departement"));

        // Date d'embauche (formatage)
        Timestamp dateEmbaucheTimestamp = document.getTimestamp("dateEmbauche");
        if (dateEmbaucheTimestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etHireDate.setText(sdf.format(dateEmbaucheTimestamp.toDate()));
        }
    }

    private void displayDefaultAuthData(String email) {
        if (etEmail != null) {
            etEmail.setText(email);
        }
    }

    private void saveProfileChanges() {
        if (employeeRef == null) {
            Toast.makeText(this, "Impossible d'enregistrer: Profil non chargé.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupération des données modifiables (personnelles)
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Le Prénom et le Nom ne peuvent être vides.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("prenom", firstName);
        updates.put("nom", lastName);
        updates.put("telephone", phone);

        // ⭐ LOGIQUE CLÉ : Inclure les champs professionnels UNIQUEMENT si l'édition est autorisée
        if (isProfessionalEditAllowed) {
            updates.put("poste", etPosition.getText().toString().trim());
            updates.put("departement", etDepartment.getText().toString().trim());
            // Note: La modification de la date d'embauche est complexe (Timestamp), elle est exclue ici par simplicité,
            // mais un admin pourrait vouloir la mettre à jour.
        }

        employeeRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profil mis à jour avec succès!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors de la mise à jour du profil: ", e);
                    Toast.makeText(EditProfileActivity.this, "Échec de la mise à jour.", Toast.LENGTH_SHORT).show();
                });
    }
}