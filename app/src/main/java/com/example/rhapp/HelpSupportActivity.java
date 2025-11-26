package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

// L'activité est basique et ne nécessite pas de Firebase, sauf pour des liens potentiels.
public class HelpSupportActivity extends AppCompatActivity {

    private static final String TAG = "HelpSupportActivity";
    private static final String SUPPORT_EMAIL = "support@entreprise.com";
    private static final String SUPPORT_PHONE = "+212690743261"; // Format sans espaces pour Uri

    // Section Contact rapide
    private LinearLayout layoutEmailSupport;
    private LinearLayout layoutPhoneSupport;

    // Section FAQ
    private LinearLayout layoutFaq1;
    private LinearLayout layoutFaq2;
    private LinearLayout layoutFaq3;
    private Button btnAllFaq;

    // Section Documentation
    private LinearLayout layoutUserGuide;
    private LinearLayout layoutVideoTutorials;
    private LinearLayout layoutOnlineHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupClickListeners();
    }

    /**
     * Initialise toutes les vues interactives du XML.
     */
    private void initializeViews() {
        // Contact rapide
        layoutEmailSupport = findViewById(R.id.layoutEmailSupport);
        layoutPhoneSupport = findViewById(R.id.layoutPhoneSupport);



        // Documentation
        layoutUserGuide = findViewById(R.id.layoutUserGuide);
        layoutOnlineHelp = findViewById(R.id.layoutOnlineHelp);
    }

    /**
     * Configure les écouteurs de clic pour chaque élément interactif.
     */
    private void setupClickListeners() {
        // --- Contact rapide ---
        layoutEmailSupport.setOnClickListener(v -> sendEmail());
        layoutPhoneSupport.setOnClickListener(v -> dialPhoneNumber());

        // --- FAQ ---
        layoutFaq1.setOnClickListener(v -> navigateToFaqDetail(1));
        layoutFaq2.setOnClickListener(v -> navigateToFaqDetail(2));
        layoutFaq3.setOnClickListener(v -> navigateToFaqDetail(3));
        btnAllFaq.setOnClickListener(v -> navigateToAllFaqs());

        // --- Documentation ---
        layoutUserGuide.setOnClickListener(v -> openUserGuide());
        layoutVideoTutorials.setOnClickListener(v -> openVideoTutorials());
        layoutOnlineHelp.setOnClickListener(v -> openOnlineHelpCenter());
    }

    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (Contact rapide)
    // ==========================================================

    /**
     * Ouvre l'application d'email pour envoyer un message au support.
     */
    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL)); // Utilise le scheme "mailto"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Demande de Support RH App");

        // Vérifie si une application d'email peut gérer l'intention
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Aucune application d'email trouvée.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Impossible de lancer l'intent EMAIL.");
        }
    }

    /**
     * Ouvre le composeur de téléphone avec le numéro du support pré-rempli.
     */
    private void dialPhoneNumber() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + SUPPORT_PHONE));

        // Vérifie si une application de téléphone peut gérer l'intention
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Impossible d'ouvrir le composeur de téléphone.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Impossible de lancer l'intent DIAL.");
        }
    }

    /**
     * Simule l'ouverture d'une interface de Chat en direct.
     */


    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (FAQ)
    // ==========================================================

    /**
     * Navigue vers une activité de détail de la FAQ.
     */
    private void navigateToFaqDetail(int faqNumber) {
        // Dans une application réelle, vous passeriez l'ID de la FAQ au lieu du numéro
        Toast.makeText(this, "Affichage de la FAQ #" + faqNumber, Toast.LENGTH_SHORT).show();
        // Exemple: Intent intent = new Intent(this, FaqDetailActivity.class);
        // intent.putExtra("FAQ_ID", faqNumber);
        // startActivity(intent);
    }

    /**
     * Navigue vers l'activité listant toutes les questions fréquentes.
     */
    private void navigateToAllFaqs() {
        Toast.makeText(this, "Affichage de toutes les FAQ...", Toast.LENGTH_SHORT).show();
        // Exemple: startActivity(new Intent(this, FaqListActivity.class));
    }


    // ==========================================================
    // MÉTHODES DE GESTION DES ACTIONS (Documentation)
    // ==========================================================

    /**
     * Ouvre un guide utilisateur (simulé).
     */
    private void openUserGuide() {
        // Ceci ouvrirait un PDF ou un document interne/externe.
        Toast.makeText(this, "Ouverture du Guide d'utilisation (PDF)...", Toast.LENGTH_SHORT).show();
        // Exemple pour ouvrir un lien externe:
        // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("URL_DU_PDF"));
        // startActivity(browserIntent);
    }

    /**
     * Ouvre une activité ou un lien vers les tutoriels vidéo.
     */
    private void openVideoTutorials() {
        Toast.makeText(this, "Ouverture des Tutoriels Vidéo...", Toast.LENGTH_SHORT).show();
        // Exemple: startActivity(new Intent(this, VideoTutorialsActivity.class));
    }

    /**
     * Ouvre le Centre d'aide en ligne dans un navigateur web.
     */
    private void openOnlineHelpCenter() {
        final String HELP_CENTER_URL = "https://www.votre-entreprise.com/aide";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(HELP_CENTER_URL));

        if (browserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "Aucun navigateur trouvé pour ouvrir l'aide en ligne.", Toast.LENGTH_SHORT).show();
        }
    }
}