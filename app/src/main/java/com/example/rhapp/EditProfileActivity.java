package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher; // ⭐ NOUVEAU
import androidx.activity.result.contract.ActivityResultContracts; // ⭐ NOUVEAU

import android.content.Intent;
import android.net.Uri; // ⭐ NOUVEAU
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage; // ⭐ NOUVEAU
import com.google.firebase.storage.StorageReference; // ⭐ NOUVEAU

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final String EMPLOYEES_COLLECTION = "employees";

    // Vues (Widgets)
    private CircleImageView ivProfilePhoto;
    private Button btnChangePhoto, btnCancel, btnSave;
    private EditText etFirstName, etLastName, etEmail, etPhone, etPosition, etDepartment, etHireDate;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage; // ⭐ INITIALISATION
    private FirebaseUser currentUser;
    private DocumentReference employeeRef;

    // Variables pour l'image
    private String employeeDocumentId;
    private Uri selectedImageUri; // ⭐ URI de l'image sélectionnée
    private ActivityResultLauncher<Intent> galleryLauncher; // ⭐ Lanceur pour la galerie

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(); // ⭐ Initialisation de Firebase Storage
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter pour modifier le profil.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ⭐ NOUVEAU : Initialisation du lanceur d'activité
        setupGalleryLauncher();

        initializeViews();
        loadEmployeeData();
        setupClickListeners();
    }

    private void initializeViews() {
        // Photo et Bouton
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        // Informations personnelles
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        // Champs désactivés (non modifiables par l'employé)
        etEmail.setEnabled(false);

        // Informations professionnelles
        etPosition = findViewById(R.id.etPosition);
        etDepartment = findViewById(R.id.etDepartment);
        etHireDate = findViewById(R.id.etHireDate);

        etPosition.setEnabled(false);
        etDepartment.setEnabled(false);
        etHireDate.setEnabled(false);

        // Boutons d'action
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfileChanges());

        // ⭐ Mise à jour : appelle la nouvelle fonction pour choisir la photo
        btnChangePhoto.setOnClickListener(v -> changeProfilePhoto());
    }

    // --- GESTION DE LA PHOTO DE PROFIL (NOUVEAUTÉS) ---

    /**
     * ⭐ NOUVEAU : Initialise l'ActivityResultLauncher pour gérer le résultat
     * de la sélection d'image depuis la galerie.
     */
    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // 1. Afficher l'image sélectionnée
                            ivProfilePhoto.setImageURI(selectedImageUri);

                            // 2. Déclencher l'upload vers Firebase Storage
                            uploadImageToFirebase(selectedImageUri);
                        }
                    } else {
                        Toast.makeText(this, "Sélection d'image annulée.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Lance l'intention de choisir une photo dans la galerie via le ActivityResultLauncher.
     */
    private void changeProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        // Utilisation du lanceur d'activité
        galleryLauncher.launch(intent);
    }

    /**
     * Téléverse l'image sélectionnée vers Firebase Storage et met à jour Firestore.
     */
    private void uploadImageToFirebase(Uri fileUri) {
        if (currentUser == null || employeeRef == null) {
            Toast.makeText(this, "Erreur de connexion ou de profil. Impossible de télécharger.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Téléchargement en cours...", Toast.LENGTH_LONG).show();

        // Créer la référence de stockage
        String fileName = currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference profileImagesRef = storage.getReference()
                .child("profile_images/" + fileName);

        // Téléverser le fichier
        profileImagesRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obtenir l'URL de téléchargement
                    profileImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        // Mettre à jour le champ 'photoUrl' dans Firestore
                        updateFirestoreProfilePhoto(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur de téléchargement de l'image:", e);
                    Toast.makeText(EditProfileActivity.this, "Échec du téléchargement de la photo.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Met à jour le champ 'photoUrl' dans le document Firestore de l'employé.
     */
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

    // --- LOGIQUE DE CHARGEMENT ET SAUVEGARDE EXISTANTE ---

    /**
     * Recherche et charge les données du profil de l'employé à partir de Firestore.
     */
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

    /**
     * Affiche les données de Firestore dans les champs d'édition.
     */
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

        // ⭐ TODO: Charger la photo de profil existante (avec Glide ou Picasso)
        // String photoUrl = document.getString("photoUrl");
        // if (photoUrl != null && !photoUrl.isEmpty()) {
        //     // Utiliser Glide.with(this).load(photoUrl).into(ivProfilePhoto);
        // }
    }

    /**
     * Affiche l'email de l'utilisateur authentifié si le profil Firestore est introuvable.
     */
    private void displayDefaultAuthData(String email) {
        if (etEmail != null) {
            etEmail.setText(email);
        }
    }

    /**
     * Enregistre les modifications de profil dans Firestore.
     */
    private void saveProfileChanges() {
        if (employeeRef == null) {
            Toast.makeText(this, "Impossible d'enregistrer: Profil non chargé.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Récupération des données modifiables
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