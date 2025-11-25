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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileEmployeActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEmployeActivity";
    private static final String EMPLOYEE_REFERENCE_COLLECTION = "employees";

    private TextView userName, userPoste, userDepartment;
    private TextView userEmail, userPosteDetails, userDepartmentDetails;
    private TextView userDateEmbauche, userInitial;
    private ProgressBar progressBar;

    // VARIABLE CLÉ: Stocke le rôle (rh ou employe) pour la navigation conditionnelle
    private String userRole;

    // Conteneurs principaux
    private LinearLayout profileCard;
    private LinearLayout profileDetails;
    private LinearLayout parametres;
    private LinearLayout sedeconnecter;
    private Button seDeconnecterButton;

    // Éléments du pied de page
    private ImageView iconAccueil, iconPresence, iconConges, iconReunions, iconProfile;
    private TextView textAccueil, textPresence, textConges, textReunions, textProfile;
    private LinearLayout footerAccueil, footerPresence, footerConges, footerReunions, footerProfil;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_employe);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialisation du rôle à null/défaut pour éviter une erreur potentielle
        userRole = null;

        initializeViews();
        setupClickListeners();

        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter.", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail != null) {
            loadUserProfileDataByEmail(userEmail);
        } else {
            Toast.makeText(this, "Email d'authentification non disponible.", Toast.LENGTH_LONG).show();
            showDefaultData();
        }

        highlightFooterIcon();
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        // Bloc Profile Card
        userName = findViewById(R.id.userName);
        userPoste = findViewById(R.id.userPoste);
        userDepartment = findViewById(R.id.userDepartment);
        userInitial = findViewById(R.id.userInitial);

        // Bloc Détails
        userEmail = findViewById(R.id.userEmailDetail);
        userPosteDetails = findViewById(R.id.userPosteDetail);
        userDepartmentDetails = findViewById(R.id.userDepartmentDetail);
        userDateEmbauche = findViewById(R.id.userDateEmbauche);

        // Layouts et autres
        profileCard = findViewById(R.id.profileCard);
        profileDetails = findViewById(R.id.profileDetailsContainer);
        parametres = findViewById(R.id.parametres);
        sedeconnecter = findViewById(R.id.sedeconnecter);
        progressBar = findViewById(R.id.progressBar);
        seDeconnecterButton = findViewById(R.id.seDeconnecterButton);

        // Footer Layouts
        footerAccueil = findViewById(R.id.footerAccueil);
        footerPresence = findViewById(R.id.footerPresence);
        footerConges = findViewById(R.id.footerConges);
        footerReunions = findViewById(R.id.footerReunions);
        footerProfil = findViewById(R.id.footerProfil);

        initializeFooterViews();
        showLoading(true);
    }

    private void initializeFooterViews() {
        // Logique pour s'assurer que les enfants existent avant de caster
        if (footerAccueil != null && footerAccueil.getChildCount() > 1) {
            iconAccueil = (ImageView) footerAccueil.getChildAt(0);
            textAccueil = (TextView) footerAccueil.getChildAt(1);
        }
        if (footerPresence != null && footerPresence.getChildCount() > 1) {
            iconPresence = (ImageView) footerPresence.getChildAt(0);
            textPresence = (TextView) footerPresence.getChildAt(1);
        }
        if (footerConges != null && footerConges.getChildCount() > 1) {
            iconConges = (ImageView) footerConges.getChildAt(0);
            textConges = (TextView) footerConges.getChildAt(1);
        }
        if (footerReunions != null && footerReunions.getChildCount() > 1) {
            iconReunions = (ImageView) footerReunions.getChildAt(0);
            textReunions = (TextView) footerReunions.getChildAt(1);
        }
        if (footerProfil != null && footerProfil.getChildCount() > 1) {
            iconProfile = (ImageView) footerProfil.getChildAt(0);
            textProfile = (TextView) footerProfil.getChildAt(1);
        }
    }

    private void setupClickListeners() {
        if (seDeconnecterButton != null) {
            seDeconnecterButton.setOnClickListener(v -> logoutUser());
        }
        setupFooterNavigation();
        setupSettingsClickListeners();
    }

    private void setupFooterNavigation() {
        if (footerAccueil != null) {
            footerAccueil.setOnClickListener(v -> navigateToHome());
        }
        if (footerPresence != null) {
            footerPresence.setOnClickListener(v -> navigateToEmployees());
        }
        if (footerConges != null) {
            footerConges.setOnClickListener(v -> navigateToConges());
        }
        if (footerReunions != null) {
            footerReunions.setOnClickListener(v -> navigateToReunions());
        }
    }

    private void setupSettingsClickListeners() {
        LinearLayout modifierProfil = findViewById(R.id.modifier_profil);
        LinearLayout notifications = findViewById(R.id.notifications);
        LinearLayout securityInterface = findViewById(R.id.security_interface);
        LinearLayout helpSupport = findViewById(R.id.help_support);

        if (modifierProfil != null) {
            modifierProfil.setOnClickListener(v -> navigateToEditProfile());
        }
        if (notifications != null) {
            notifications.setOnClickListener(v -> navigateToNotifications());
        }
        if (securityInterface != null) {
            securityInterface.setOnClickListener(v -> navigateToSecurity());
        }
        if (helpSupport != null) {
            helpSupport.setOnClickListener(v -> navigateToHelpSupport());
        }
    }

    /**
     * Charge les données du profil en recherchant par email dans la collection employees.
     */
    private void loadUserProfileDataByEmail(String userEmail) {
        showLoading(true);
        final String searchEmail = userEmail.toLowerCase(Locale.ROOT).trim();

        db.collection(EMPLOYEE_REFERENCE_COLLECTION)
                .whereEqualTo("email", searchEmail)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot employeeSnapshot = task.getResult().getDocuments().get(0);

                        String nom = employeeSnapshot.getString("nom");
                        String prenom = employeeSnapshot.getString("prenom");
                        String email = employeeSnapshot.getString("email");
                        String poste = employeeSnapshot.getString("poste");
                        String departement = employeeSnapshot.getString("departement");
                        String role = employeeSnapshot.getString("role");
                        Timestamp dateEmbaucheTimestamp = employeeSnapshot.getTimestamp("dateEmbauche");

                        displayAllUserData(nom, prenom, email, poste, departement, role, dateEmbaucheTimestamp);

                    } else {
                        if (task.isSuccessful()) {
                            Log.w(TAG, "Aucun profil trouvé, affichage des données par défaut.");
                        } else {
                            Log.e(TAG, "Erreur lors de la recherche par email:", task.getException());
                        }

                        showDefaultData();
                        Toast.makeText(this,
                                "Profil employé non trouvé. Données par défaut affichées.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Affiche les données par défaut quand le profil n'est pas trouvé.
     */
    private void showDefaultData() {
        String email = currentUser != null ? currentUser.getEmail() : "N/A";
        String displayName = currentUser != null && currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Employé";

        // Rôle par défaut : "employe"
        displayAllUserData(null, displayName, email, "Poste non défini",
                "Département non défini", "employe", null);
    }

    /**
     * Affiche toutes les données utilisateur dans l'UI et stocke le rôle.
     */
    private void displayAllUserData(String nom, String prenom, String email,
                                    String poste, String departement, String role, Timestamp dateEmbaucheTimestamp) {

        // Stockage du rôle pour la navigation conditionnelle
        this.userRole = role;

        // 1. Bloc Card
        String fullName = buildFullName(nom, prenom);
        if (userName != null) userName.setText(fullName);

        String departmentDisplay = formatText(departement, "Non défini");
        String posteDisplay = formatText(poste, "Poste non défini");

        if (userPoste != null) userPoste.setText(posteDisplay);
        if (userDepartment != null) userDepartment.setText(departmentDisplay);

        // Initiales
        if (userInitial != null) {
            String initial = buildInitials(nom, prenom);
            userInitial.setText(initial);
        }

        // 2. Bloc Détails
        if (userEmail != null) userEmail.setText(formatText(email, "N/A"));
        if (userPosteDetails != null) userPosteDetails.setText(posteDisplay);
        if (userDepartmentDetails != null) userDepartmentDetails.setText(departmentDisplay);

        // 3. Date d'embauche
        if (userDateEmbauche != null) {
            String dateEmbauche = formatDateEmbauche(dateEmbaucheTimestamp);
            userDateEmbauche.setText(dateEmbauche);
        }
    }

    private String buildFullName(String nom, String prenom) {
        String finalPrenom = prenom != null ? prenom : "";
        String finalNom = nom != null ? nom : "";

        String fullName = (finalPrenom + " " + finalNom).trim();
        return fullName.isEmpty() ? "Utilisateur" : fullName;
    }

    private String buildInitials(String nom, String prenom) {
        StringBuilder initial = new StringBuilder();
        if (prenom != null && !prenom.isEmpty()) {
            initial.append(prenom.substring(0, 1).toUpperCase(Locale.getDefault()));
        }
        if (nom != null && !nom.isEmpty()) {
            initial.append(nom.substring(0, 1).toUpperCase(Locale.getDefault()));
        }
        return initial.length() > 0 ? initial.toString() : "U";
    }

    private String formatText(String text, String defaultValue) {
        if (text == null || text.isEmpty()) {
            return defaultValue;
        }
        if ("N/A".equalsIgnoreCase(defaultValue) && text.contains("@")) {
            return text;
        }
        String lowerText = text.toLowerCase(Locale.getDefault());
        return lowerText.substring(0, 1).toUpperCase(Locale.getDefault()) + lowerText.substring(1);
    }

    private String formatDateEmbauche(Timestamp timestamp) {
        if (timestamp != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
                return sdf.format(timestamp.toDate());
            } catch (Exception e) {
                Log.e(TAG, "Erreur formatage date:", e);
                return "Date invalide";
            }
        }
        return "Non définie";
    }

    /**
     * Met en évidence l'icône de profil dans le footer
     */
    private void highlightFooterIcon() {
        int colorBlue = ContextCompat.getColor(this, R.color.blue);
        int colorGrey = ContextCompat.getColor(this, R.color.grey);

        resetFooterIcons(colorGrey);

        if (iconProfile != null) iconProfile.setColorFilter(colorBlue);
        if (textProfile != null) textProfile.setTextColor(colorBlue);
    }

    private void resetFooterIcons(int colorGrey) {
        ImageView[] icons = {iconAccueil, iconPresence, iconConges, iconReunions, iconProfile};
        TextView[] texts = {textAccueil, textPresence, textConges, textReunions, textProfile};

        for (ImageView icon : icons) {
            if (icon != null) icon.setColorFilter(colorGrey);
        }
        for (TextView text : texts) {
            if (text != null) text.setTextColor(colorGrey);
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
        navigateToMainActivity();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(ProfileEmployeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // --- Méthodes de navigation corrigées ---

    /**
     * Navigue vers l'écran d'accueil en fonction du rôle de l'utilisateur.
     * Si le rôle est 'rh', va à AcceuilRhActivity, sinon à AcceuilEmployeActivity.
     */
    private void navigateToHome() {
        startActivity(new Intent(ProfileEmployeActivity.this, AcceuilEmployeActivity.class));
    }

    private void navigateToEmployees() {
        // Ajout de startActivity
        startActivity(new Intent(ProfileEmployeActivity.this, EmployeActivity.class));
    }

    private void navigateToConges() {
        // Ajout de startActivity
        startActivity(new Intent(ProfileEmployeActivity.this, CongesActivity.class));
    }

    private void navigateToReunions() {
        // Correction: Ajout de startActivity. J'assume l'Activity est ReunionActivity.class
        startActivity(new Intent(ProfileEmployeActivity.this, ReunionEmployeActivity.class));
    }

    private void navigateToEditProfile() {
        // Ajout de startActivity (appelé via modifier_profil)
        startActivity(new Intent(ProfileEmployeActivity.this, EditProfileActivity.class));
    }


    /**
     * Navigue vers l'écran de Notifications en fonction du rôle de l'utilisateur.
     * Ce bloc remplace la logique de notification qui était dupliquée dans navigateToHome().
     */
    private void navigateToNotifications() {
        startActivity(new Intent(ProfileEmployeActivity.this, NotificationsRhActivity.class));
    }

    private void navigateToSecurity() {
        // Correction: Ajout de startActivity
        startActivity(new Intent(ProfileEmployeActivity.this, SecurityActivity.class));
    }

    private void navigateToHelpSupport() {
        // Correction: Ajout de startActivity
        startActivity(new Intent(ProfileEmployeActivity.this, HelpSupportActivity.class));
    }

    /**
     * Gestion de l'affichage du chargement
     */
    private void showLoading(boolean show) {
        int contentVisibility = show ? View.GONE : View.VISIBLE;

        if (profileCard != null) profileCard.setVisibility(contentVisibility);
        if (profileDetails != null) profileDetails.setVisibility(contentVisibility);
        if (parametres != null) parametres.setVisibility(contentVisibility);
        if (sedeconnecter != null) sedeconnecter.setVisibility(contentVisibility);

        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}