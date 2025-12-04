package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class HelpSupportActivity extends AppCompatActivity {

    private static final String TAG = "HelpSupportActivity";
    private static final String SUPPORT_EMAIL = "support@entreprise.com";
    private static final String SUPPORT_PHONE = "+212690743261"; // Format sans espaces pour Uri

    // Section Contact rapide
    private LinearLayout layoutEmailSupport;
    private LinearLayout layoutPhoneSupport;

    // Section Documentation
    private LinearLayout layoutUserGuide;
    private LinearLayout layoutOnlineHelp;

    // Autres vues - SI ELLES EXISTENT DANS VOTRE XML
    private LinearLayout layoutFaq1;
    private LinearLayout layoutFaq2;
    private LinearLayout layoutFaq3;
    private Button btnAllFaq;
    private LinearLayout layoutVideoTutorials;
    private LinearLayout layoutLiveChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupClickListeners();
    }

    /**
     * Initialise toutes les vues interactives du XML.
     * AVEC VÉRIFICATIONS NULL
     */
    private void initializeViews() {
        Log.d(TAG, "Initialisation des vues...");

        // Contact rapide
        layoutEmailSupport = findViewById(R.id.layoutEmailSupport);
        layoutPhoneSupport = findViewById(R.id.layoutPhoneSupport);

        // Documentation
        layoutUserGuide = findViewById(R.id.layoutUserGuide);
        layoutOnlineHelp = findViewById(R.id.layoutOnlineHelp);

        // Initialiser les autres vues uniquement si elles existent


        // Log pour déboguer
        logViewStatus("layoutEmailSupport", layoutEmailSupport);
        logViewStatus("layoutPhoneSupport", layoutPhoneSupport);
        logViewStatus("layoutUserGuide", layoutUserGuide);
        logViewStatus("layoutOnlineHelp", layoutOnlineHelp);
        logViewStatus("layoutFaq1", layoutFaq1);
    }

    private void logViewStatus(String viewName, Object view) {
        if (view == null) {
            Log.w(TAG, viewName + " est NULL - Vérifiez l'ID dans le XML");
        } else {
            Log.d(TAG, viewName + " initialisé avec succès");
        }
    }

    /**
     * Configure les écouteurs de clic pour chaque élément interactif.
     * AVEC VÉRIFICATIONS POUR ÉVITER NULL POINTER EXCEPTION
     */
    private void setupClickListeners() {
        // --- Contact rapide ---
        if (layoutEmailSupport != null) {
            layoutEmailSupport.setOnClickListener(v -> sendEmail());
        } else {
            Log.e(TAG, "Impossible de configurer le click listener - layoutEmailSupport est null");
        }

        if (layoutPhoneSupport != null) {
            layoutPhoneSupport.setOnClickListener(v -> dialPhoneNumber());
        } else {
            Log.e(TAG, "Impossible de configurer le click listener - layoutPhoneSupport est null");
        }

        // --- FAQ (uniquement si les vues existent) ---
        if (layoutFaq1 != null) {
            layoutFaq1.setOnClickListener(v -> navigateToFaqDetail(1));
        }
        if (layoutFaq2 != null) {
            layoutFaq2.setOnClickListener(v -> navigateToFaqDetail(2));
        }
        if (layoutFaq3 != null) {
            layoutFaq3.setOnClickListener(v -> navigateToFaqDetail(3));
        }
        if (btnAllFaq != null) {
            btnAllFaq.setOnClickListener(v -> navigateToAllFaqs());
        }

        // --- Documentation ---
        if (layoutUserGuide != null) {
            layoutUserGuide.setOnClickListener(v -> openUserGuide());
        } else {
            Log.e(TAG, "Impossible de configurer le click listener - layoutUserGuide est null");
        }

        if (layoutOnlineHelp != null) {
            layoutOnlineHelp.setOnClickListener(v -> openOnlineHelpCenter());
        } else {
            Log.e(TAG, "Impossible de configurer le click listener - layoutOnlineHelp est null");
        }

        if (layoutVideoTutorials != null) {
            layoutVideoTutorials.setOnClickListener(v -> openVideoTutorials());
        }

        if (layoutLiveChat != null) {
            layoutLiveChat.setOnClickListener(v -> openLiveChat());
        }
    }

    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (Contact rapide)
    // ==========================================================

    /**
     * Ouvre l'application d'email pour envoyer un message au support.
     */
    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Demande de Support RH App");
            intent.putExtra(Intent.EXTRA_TEXT, "Bonjour,\n\nJe vous contacte pour...");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Choisir une application email"));
            } else {
                Toast.makeText(this, "Aucune application d'email trouvée.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Impossible de lancer l'intent EMAIL.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi d'email: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'ouverture de l'email", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ouvre le composeur de téléphone avec le numéro du support pré-rempli.
     */
    private void dialPhoneNumber() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + SUPPORT_PHONE));

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Impossible d'ouvrir le composeur de téléphone.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Impossible de lancer l'intent DIAL.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'appel: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'ouverture du téléphone", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Simule l'ouverture d'une interface de Chat en direct.
     */
    private void openLiveChat() {
        Toast.makeText(this, "Chat en direct - Fonctionnalité à venir", Toast.LENGTH_SHORT).show();
    }

    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (FAQ)
    // ==========================================================

    /**
     * Navigue vers une activité de détail de la FAQ.
     */
    private void navigateToFaqDetail(int faqNumber) {
        // Dans une application réelle, vous passeriez l'ID de la FAQ
        Toast.makeText(this, "Affichage de la FAQ #" + faqNumber, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Navigation vers FAQ #" + faqNumber);
        // Exemple:
        // Intent intent = new Intent(this, FaqDetailActivity.class);
        // intent.putExtra("FAQ_ID", faqNumber);
        // startActivity(intent);
    }

    /**
     * Navigue vers l'activité listant toutes les questions fréquentes.
     */
    private void navigateToAllFaqs() {
        Toast.makeText(this, "Affichage de toutes les FAQ...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Navigation vers toutes les FAQ");
        // Exemple: startActivity(new Intent(this, FaqListActivity.class));
    }

    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (Documentation)
    // ==========================================================

    /**
     * Ouvre un guide utilisateur (simulé).
     */
    private void openUserGuide() {
        try {
            // URL de démonstration pour un PDF
            String pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl));

            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Aucune application trouvée pour ouvrir le PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture du guide: " + e.getMessage());
            Toast.makeText(this, "Impossible d'ouvrir le guide", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Ouvre une activité ou un lien vers les tutoriels vidéo.
     */
    private void openVideoTutorials() {
        Toast.makeText(this, "Ouverture des Tutoriels Vidéo...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Ouverture des tutoriels vidéo");
        // Exemple: startActivity(new Intent(this, VideoTutorialsActivity.class));
    }

    /**
     * Ouvre le Centre d'aide en ligne dans un navigateur web.
     */
    private void openOnlineHelpCenter() {
        try {
            final String HELP_CENTER_URL = "https://www.example.com/aide";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_CENTER_URL));

            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "Aucun navigateur trouvé pour ouvrir l'aide en ligne.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ouverture de l'aide en ligne: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'ouverture du navigateur", Toast.LENGTH_SHORT).show();
        }
    }
}