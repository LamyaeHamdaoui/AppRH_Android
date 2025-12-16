package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity implements DeconnecterFragment.LogoutListener {

    private static final String TAG = "ProfileActivity";
    private static final String EMPLOYEE_REFERENCE_COLLECTION = "employees";

    private TextView userName, userPoste, userDepartment;
    private TextView userEmail, userPosteDetails, userDepartmentDetails;
    private TextView userDateEmbauche, userInitial;
    private ProgressBar progressBar;
    private RelativeLayout notificationsButton;

    private String userRole;

    // Conteneurs principaux
    private LinearLayout profileCard;
    private LinearLayout profileDetails;
    private LinearLayout parametres;
    private LinearLayout sedeconnecter;
    private Button seDeconnecterButton;

    // Éléments du pied de page
    private ImageView iconAccueil, iconEmployes, iconConges, iconReunions, iconProfile;
    private TextView textAccueil, textEmployes, textConges, textReunions, textProfile;
    private LinearLayout footerAccueil, footerEmployes, footerConges, footerReunions, footerProfil;

    // ⭐ CORRECTION : L'élément securityInterface est une variable de classe pour une utilisation facile
    private LinearLayout securityInterface;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Référence à l'ImageView de la photo de profil dans la carte
    private ImageView userProfileImage;

    // Executor pour les opérations en arrière-plan
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

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
            // Charger les données en arrière-plan
            loadUserProfileDataByEmail(userEmail);
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "Email d'authentification non disponible.", Toast.LENGTH_LONG).show();
                showDefaultData();
            });
        }

        highlightFooterIcon();
    }

    // Recharge les données à chaque fois que l'activité redevient visible
    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            if (userEmail != null) {
                // Forcer le rechargement pour afficher la nouvelle photo de profil
                loadUserProfileDataByEmail(userEmail);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer l'executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        // Bloc Profile Card
        userName = findViewById(R.id.userName);
        userPoste = findViewById(R.id.userPoste);
        userDepartment = findViewById(R.id.userDepartment);
        userInitial = findViewById(R.id.userInitial);
        userProfileImage = findViewById(R.id.userProfileImage);

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
        footerEmployes = findViewById(R.id.footerEmployes);
        footerConges = findViewById(R.id.footerConges);
        footerReunions = findViewById(R.id.footerReunions);
        footerProfil = findViewById(R.id.footerProfil);

        // Initialisation de la variable de classe securityInterface
        securityInterface = findViewById(R.id.securityInterface);

        initializeFooterViews();
        showLoading(true);
    }

    private void initializeFooterViews() {
        runOnUiThread(() -> {
            if (footerAccueil != null && footerAccueil.getChildCount() > 1) {
                iconAccueil = (ImageView) footerAccueil.getChildAt(0);
                textAccueil = (TextView) footerAccueil.getChildAt(1);
            }
            if (footerEmployes != null && footerEmployes.getChildCount() > 1) {
                iconEmployes = (ImageView) footerEmployes.getChildAt(0);
                textEmployes = (TextView) footerEmployes.getChildAt(1);
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
                iconProfile = (ImageView) footerProfil.getChildAt(1);
                textProfile = (TextView) footerProfil.getChildAt(2);
            }
        });
    }

    private void setupClickListeners() {
        runOnUiThread(() -> {
            if (seDeconnecterButton != null) {
                seDeconnecterButton.setOnClickListener(v -> displayLogoutConfirmation());
            }
            setupFooterNavigation();
            setupSettingsClickListeners();
        });
    }

    private void displayLogoutConfirmation() {
        runOnUiThread(() -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            DeconnecterFragment deconnecterFragment = new DeconnecterFragment();
            deconnecterFragment.show(fragmentManager, "DeconnecterFragmentTag");
        });
    }

    @Override
    public void onLogoutConfirmed() {
        // La déconnexion doit se faire sur le thread principal
        runOnUiThread(this::performLogout);
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();
        navigateToMainActivity();
    }

    private void setupFooterNavigation() {
        runOnUiThread(() -> {
            if (footerAccueil != null) {
                footerAccueil.setOnClickListener(v -> navigateToHome());
            }
            if (footerEmployes != null) {
                footerEmployes.setOnClickListener(v -> navigateToEmployees());
            }
            if (footerConges != null) {
                footerConges.setOnClickListener(v -> navigateToConges());
            }
            if (footerReunions != null) {
                footerReunions.setOnClickListener(v -> navigateToReunions());
            }
            if (notificationsButton != null) {
                notificationsButton.setOnClickListener(v -> navigateToNotifications());
            }
        });
    }

    private void setupSettingsClickListeners() {
        runOnUiThread(() -> {
            LinearLayout modifierProfil = findViewById(R.id.modifier_profil);
            LinearLayout notifications = findViewById(R.id.notifications);
            LinearLayout securityInterfaceLocal = findViewById(R.id.securityInterface);
            LinearLayout helpSupport = findViewById(R.id.help_support);

            if (modifierProfil != null) {
                modifierProfil.setOnClickListener(v -> navigateToEditProfile());
            }
            if (notifications != null) {
                notifications.setOnClickListener(v -> navigateToNotifications());
            }

            if (securityInterfaceLocal != null) {
                securityInterfaceLocal.setOnClickListener(v -> navigateToSecurity());
            }

            if (helpSupport != null) {
                helpSupport.setOnClickListener(v -> navigateToHelpSupport());
            }
        });
    }

    /**
     * Charge les données du profil en recherchant par email (en arrière-plan).
     */
    private void loadUserProfileDataByEmail(String userEmail) {
        runOnUiThread(() -> showLoading(true));

        final String searchEmail = userEmail.toLowerCase(Locale.ROOT).trim();

        // Exécuter la requête Firebase en arrière-plan
        executorService.execute(() -> {
            try {
                db.collection(EMPLOYEE_REFERENCE_COLLECTION)
                        .whereEqualTo("email", searchEmail)
                        .limit(1)
                        .get()
                        .addOnCompleteListener(task -> {
                            // Retour au thread principal pour mettre à jour l'UI
                            runOnUiThread(() -> {
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
                                    String photoUrl = employeeSnapshot.getString("photoUrl");

                                    // Afficher les données sur le thread principal
                                    displayAllUserData(nom, prenom, email, poste, departement, role, dateEmbaucheTimestamp, photoUrl);

                                } else {
                                    if (task.isSuccessful()) {
                                        Log.w(TAG, "Aucun profil trouvé, affichage des données par défaut.");
                                    } else {
                                        Log.e(TAG, "Erreur lors de la recherche par email:", task.getException());
                                    }

                                    showDefaultData();
                                    Toast.makeText(ProfileActivity.this,
                                            "Profil employé non trouvé. Données par défaut affichées.",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        });

            } catch (Exception e) {
                Log.e(TAG, "Erreur dans executor: ", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showDefaultData();
                    Toast.makeText(ProfileActivity.this, "Erreur de chargement du profil", Toast.LENGTH_SHORT).show();
                });
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

        // Ajout de null pour photoUrl
        displayAllUserData(null, displayName, email, "Poste non défini",
                "Département non défini", "employe", null, null);
    }

    /**
     * Affiche toutes les données utilisateur dans l'UI (sur le thread principal).
     */
    private void displayAllUserData(String nom, String prenom, String email,
                                    String poste, String departement, String role,
                                    Timestamp dateEmbaucheTimestamp, String photoUrl) {

        runOnUiThread(() -> {
            this.userRole = role;
            String fullName = buildFullName(nom, prenom);
            String departmentDisplay = formatText(departement, "Non défini");
            String posteDisplay = formatText(poste, "Poste non défini");

            if (userName != null) userName.setText(fullName);
            if (userPoste != null) userPoste.setText(posteDisplay);
            if (userDepartment != null) userDepartment.setText(departmentDisplay);

            // LOGIQUE CLÉ : Affichage de la photo ou des initiales
            if (userProfileImage != null && userInitial != null) {
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    // 1. Photo disponible : Charger l'image et cacher les initiales
                    userInitial.setVisibility(View.GONE);
                    userProfileImage.setVisibility(View.VISIBLE);

                    // Glide gère déjà le chargement en arrière-plan
                    executorService.execute(() -> {
                        mainHandler.post(() -> {
                            Glide.with(ProfileActivity.this)
                                    .load(photoUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(R.drawable.user)
                                    .error(R.drawable.user)
                                    .into(userProfileImage);
                        });
                    });

                } else {
                    // 2. Pas de photo : Afficher les initiales et cacher l'ImageView
                    userInitial.setVisibility(View.VISIBLE);
                    userProfileImage.setVisibility(View.INVISIBLE);

                    String initial = buildInitials(nom, prenom);
                    userInitial.setText(initial);
                }
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
        });
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
        runOnUiThread(() -> {
            int colorBlue = ContextCompat.getColor(this, R.color.blue);
            int colorGrey = ContextCompat.getColor(this, R.color.grey);

            resetFooterIcons(colorGrey);

            if (iconProfile != null) iconProfile.setColorFilter(colorBlue);
            if (textProfile != null) textProfile.setTextColor(colorBlue);
        });
    }

    private void resetFooterIcons(int colorGrey) {
        ImageView[] icons = {iconAccueil, iconEmployes, iconConges, iconReunions, iconProfile};
        TextView[] texts = {textAccueil, textEmployes, textConges, textReunions, textProfile};

        for (ImageView icon : icons) {
            if (icon != null) icon.setColorFilter(colorGrey);
        }
        for (TextView text : texts) {
            if (text != null) text.setTextColor(colorGrey);
        }
    }

    private void navigateToMainActivity() {
        runOnUiThread(() -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToHome() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, AcceuilRhActivity.class));
        });
    }

    private void navigateToEmployees() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, EmployeActivity.class));
        });
    }

    private void navigateToConges() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, CongesActivity.class));
        });
    }

    private void navigateToReunions() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, reunionActivity.class));
        });
    }

    private void navigateToEditProfile() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });
    }

    private void navigateToNotifications() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, AcceuilRhActivity.class));
        });
    }

    // Navigation vers SecurityActivity
    private void navigateToSecurity() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, SecurityActivity.class));
        });
    }

    private void navigateToHelpSupport() {
        runOnUiThread(() -> {
            startActivity(new Intent(ProfileActivity.this, HelpSupportActivity.class));
        });
    }

    /**
     * Gestion de l'affichage du chargement
     */
    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            int contentVisibility = show ? View.GONE : View.VISIBLE;

            if (profileCard != null) profileCard.setVisibility(contentVisibility);
            if (profileDetails != null) profileDetails.setVisibility(contentVisibility);
            if (parametres != null) parametres.setVisibility(contentVisibility);
            if (sedeconnecter != null) sedeconnecter.setVisibility(contentVisibility);

            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}