package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

// NOTE IMPORTANTE: La classe User est retirée car nous lisons les champs directement du DocumentSnapshot.
// Si la classe User est toujours nécessaire ailleurs, elle doit être conservée, mais son usage ici
// pour le toObject est moins fiable que de lire les champs un par un.

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    // Utilisation unique de la collection 'employees' comme source de vérité
    private static final String EMPLOYEE_REFERENCE_COLLECTION = "employees";

    // UI elements
    private TextView userNameTextView, userRoleTextView, userDepartmentTextView;
    private TextView userEmailTextView, userPosteTextView, userDepartmentDetailsTextView;
    private TextView userDateEmbaucheTextView, userInitialTextView;
    private ProgressBar progressBar;

    // Éléments du pied de page (Footer)
    private ImageView iconAccueil, iconEmployes, iconConges, iconReunions, iconProfile;
    private TextView textAccueil, textEmployes, textConges, textReunions, textProfile;

    private LinearLayout profileCardLayout, footerAccueil, footerEmployes, footerConges, footerReunions, footerProfil;
    private Button seDeconnecterButton;
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initializeViews();
        setupClickListeners();

        if (currentUser == null) {
            // Utilisateur non connecté, rediriger
            Toast.makeText(this, "Veuillez vous connecter.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
            return;
        }

        // Chargement des données consolidé
        loadUserProfileData(currentUser.getUid());
        highlightFooterIcon();
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        // Bloc Profile Card
        userNameTextView = findViewById(R.id.userName);
        userRoleTextView = findViewById(R.id.userRole);
        userDepartmentTextView = findViewById(R.id.userDepartment);
        userInitialTextView = findViewById(R.id.userInitial);

        // Bloc Détails
        userEmailTextView = findViewById(R.id.userEmailDetail);
        userPosteTextView = findViewById(R.id.userPosteDetail);
        userDepartmentDetailsTextView = findViewById(R.id.userDepartmentDetail);
        userDateEmbaucheTextView = findViewById(R.id.userDateEmbauche);

        // Layouts et autres
        profileCardLayout = findViewById(R.id.profileCard);
        progressBar = findViewById(R.id.progressBar);
        seDeconnecterButton = findViewById(R.id.seDeconnecterButton);

        // Footer Layouts (conteneurs cliquables)
        footerAccueil = findViewById(R.id.footerAccueil);
        footerEmployes = findViewById(R.id.footerEmployes);
        footerConges = findViewById(R.id.footerConges);
        footerReunions = findViewById(R.id.footerReunions);
        footerProfil = findViewById(R.id.footerProfil);

        // Récupération des icônes
        if (footerAccueil != null && footerAccueil.getChildCount() > 0) {
            iconAccueil = (ImageView) footerAccueil.getChildAt(0);
        }
        if (footerEmployes != null && footerEmployes.getChildCount() > 0) {
            iconEmployes = (ImageView) footerEmployes.getChildAt(0);
        }
        if (footerConges != null && footerConges.getChildCount() > 0) {
            iconConges = (ImageView) footerConges.getChildAt(0);
        }
        if (footerReunions != null && footerReunions.getChildCount() > 0) {
            iconReunions = (ImageView) footerReunions.getChildAt(0);
        }
        if (footerProfil != null && footerProfil.getChildCount() > 0) {
            iconProfile = (ImageView) footerProfil.getChildAt(0);
        }

        // Récupération des textes
        if (footerAccueil != null && footerAccueil.getChildCount() > 1) {
            textAccueil = (TextView) footerAccueil.getChildAt(1);
        }
        if (footerEmployes != null && footerEmployes.getChildCount() > 1) {
            textEmployes = (TextView) footerEmployes.getChildAt(1);
        }
        if (footerConges != null && footerConges.getChildCount() > 1) {
            textConges = (TextView) footerConges.getChildAt(1);
        }
        if (footerReunions != null && footerReunions.getChildCount() > 1) {
            textReunions = (TextView) footerReunions.getChildAt(1);
        }
        if (footerProfil != null && footerProfil.getChildCount() > 1) {
            textProfile = (TextView) footerProfil.getChildAt(1);
        }

        showLoading(true);
    }

    private void setupClickListeners() {
        if (seDeconnecterButton != null) {
            seDeconnecterButton.setOnClickListener(v -> logoutUser());
        }

        // TODO: Ajouter ici les listeners pour la navigation du footer
        // Exemple :
        // if (footerAccueil != null) {
        //     footerAccueil.setOnClickListener(v -> navigateTo(AccueilActivity.class));
        // }
    }

    /**
     * Met en évidence l'icône de profil.
     */
    private void highlightFooterIcon() {
        int colorBlue = ContextCompat.getColor(this, R.color.blue);
        int colorGrey = ContextCompat.getColor(this, R.color.grey);

        // Liste de toutes les vues pour les réinitialiser en gris (sauf le profil)
        ImageView[] icons = {iconAccueil, iconEmployes, iconConges, iconReunions};
        TextView[] texts = {textAccueil, textEmployes, textConges, textReunions};

        // 1. Réinitialiser toutes les icônes et textes en gris
        for (ImageView icon : icons) {
            if (icon != null) icon.setColorFilter(colorGrey);
        }
        for (TextView text : texts) {
            if (text != null) text.setTextColor(colorGrey);
        }

        // 2. Mettre en évidence l'icône et le texte de Profil
        if (iconProfile != null) {
            iconProfile.setColorFilter(colorBlue);
        }
        if (textProfile != null) {
            textProfile.setTextColor(colorBlue);
        }
    }


    /**
     * Charge toutes les données du profil depuis la collection 'employees'
     * en utilisant le Firebase UID comme ID de document.
     * @param userId L'ID de l'utilisateur Firebase.
     */
    private void loadUserProfileData(String userId) {
        showLoading(true);

        // Requête unique pour obtenir toutes les données de l'employé
        DocumentReference employeeDocRef = db.collection(EMPLOYEE_REFERENCE_COLLECTION).document(userId);

        employeeDocRef.get().addOnSuccessListener(employeeSnapshot -> {
            showLoading(false);

            if (employeeSnapshot.exists()) {
                // Extraction de tous les champs nécessaires directement du snapshot
                String nom = employeeSnapshot.getString("nom");
                String prenom = employeeSnapshot.getString("prenom");
                String role = employeeSnapshot.getString("role");
                String email = employeeSnapshot.getString("email");
                String poste = employeeSnapshot.getString("poste");
                String departement = employeeSnapshot.getString("departement");

                // IMPORTANT: Assurez-vous que le champ 'dateEmbauche' est bien de type Timestamp dans Firestore.
                Timestamp dateEmbaucheTimestamp = employeeSnapshot.getTimestamp("dateEmbauche");

                displayAllUserData(nom, prenom, role, email, poste, departement, dateEmbaucheTimestamp);
            } else {
                Toast.makeText(this, "Profil employé non trouvé dans Firestore (ID: " + userId + ").", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Log.e(TAG, "Erreur de chargement du profil: ", e);
            Toast.makeText(this, "Erreur de chargement du profil.", Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Affiche toutes les données utilisateur après les avoir récupérées du document 'employees'.
     * @param nom Nom de l'employé
     * @param prenom Prénom de l'employé
     * @param role Rôle de l'employé
     * @param email Email de l'employé
     * @param poste Poste occupé
     * @param departement Département
     * @param dateEmbaucheTimestamp Timestamp Firestore de la date d'embauche
     */
    private void displayAllUserData(String nom, String prenom, String role, String email,
                                    String poste, String departement, Timestamp dateEmbaucheTimestamp) {

        // 1. Bloc Card (Nom/Rôle/Initiales)
        String fullName = (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
        if (userNameTextView != null) userNameTextView.setText(fullName.trim());

        String roleDisplay = role;
        if (roleDisplay != null && !roleDisplay.isEmpty()) {
            // Formater le rôle (première lettre en majuscule)
            String formattedRole = roleDisplay.substring(0, 1).toUpperCase(Locale.getDefault()) + roleDisplay.substring(1);
            if (userRoleTextView != null) userRoleTextView.setText(formattedRole);
            // Afficher le rôle sous le nom
            if (userDepartmentTextView != null) userDepartmentTextView.setText(formattedRole);
        } else {
            if (userRoleTextView != null) userRoleTextView.setText("Rôle N/A");
            if (userDepartmentTextView != null) userDepartmentTextView.setText("N/A");
        }

        // Affichage de l'initiale
        String initial = "";
        if (prenom != null && !prenom.isEmpty()) {
            initial += prenom.substring(0, 1).toUpperCase(Locale.getDefault());
        }
        if (nom != null && !nom.isEmpty()) {
            initial += nom.substring(0, 1).toUpperCase(Locale.getDefault());
        }
        if (userInitialTextView != null) userInitialTextView.setText(initial);

        // 2. Bloc Détails
        if (userEmailTextView != null) userEmailTextView.setText(email != null ? email : "N/A");
        if (userPosteTextView != null) userPosteTextView.setText(poste != null ? poste : "N/A");
        if (userDepartmentDetailsTextView != null) userDepartmentDetailsTextView.setText(departement != null ? departement : "N/A");

        // 3. Affichage de la Date d'Embauche (Sécurisation contre les erreurs de format)
        if (dateEmbaucheTimestamp != null && userDateEmbaucheTextView != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
            try {
                // Tente de formater la date
                userDateEmbaucheTextView.setText(sdf.format(dateEmbaucheTimestamp.toDate()));
            } catch (Exception e) {
                // Si une erreur se produit (par exemple, si le champ n'est pas un Timestamp valide)
                Log.e(TAG, "Erreur de formatage de la date d'embauche: ", e);
                userDateEmbaucheTextView.setText("Date Invalide");
            }
        } else if (userDateEmbaucheTextView != null) {
            userDateEmbaucheTextView.setText("N/A");
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        // Rediriger vers l'écran de connexion (ou la page d'accueil après déconnexion)
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        // La visibilité du contenu principal est l'inverse de la visibilité du ProgressBar
        int visibility = show ? View.GONE : View.VISIBLE;

        // Afficher/Cacher le contenu principal
        if (profileCardLayout != null) {
            profileCardLayout.setVisibility(visibility);
        }

        // Cacher les détails aussi pour un meilleur effet de chargement
        if (userEmailTextView != null) findViewById(R.id.profileCard).setVisibility(visibility);


        // Afficher/Cacher la barre de progression
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}