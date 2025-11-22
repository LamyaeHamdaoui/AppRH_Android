package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class AcceuilRhActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private LinearLayout reunionsInterface, congesInterface, employesInterface, profileInterface;
    private Button logoutButton; // Assurez-vous d'ajouter un bouton de déconnexion dans activity_acceuil_rh.xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceuil_rh);

        mAuth = FirebaseAuth.getInstance();

        // 1. Initialisation des Vues (Décommentées et corrigées)
        initializeViews();

        // 2. Configuration des écouteurs de clics
        setupClickListeners();

        // Gestion de l'affichage en mode plein écran (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        // Supposons que activity_acceuil_rh.xml a un conteneur principal avec l'ID 'main'
        // et les boutons/interfaces listés dans votre code commenté.

        // Initialisation des éléments de navigation
        reunionsInterface = findViewById(R.id.reunions_interface);
        congesInterface = findViewById(R.id.conges_interface); // J'ai corrigé l'ID ici si c'était 'conges_interafce'
        employesInterface = findViewById(R.id.employes_interface);
        profileInterface = findViewById(R.id.profile_interface);

        // Initialisation du bouton de déconnexion (Ajouter l'ID dans votre XML)
        // Si vous n'avez pas de bouton spécifique, vous pouvez le lier à un autre élément.
        // Exemple:
        // logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupClickListeners() {
        if (reunionsInterface != null) {
            reunionsInterface.setOnClickListener(v -> {
                // Remplacer reunionActivity.class par la classe réelle
               startActivity(new Intent(AcceuilRhActivity.this, reunionActivity.class));
                //Toast.makeText(this, "Accès Réunions", Toast.LENGTH_SHORT).show();
            });
        }

        if (congesInterface != null) {
            congesInterface.setOnClickListener(v -> {
                // Remplacer CongesActivity.class par la classe réelle
                 startActivity(new Intent(AcceuilRhActivity.this, CongesActivity.class));
                //Toast.makeText(this, "Accès Congés", Toast.LENGTH_SHORT).show();
            });
        }

        if (employesInterface != null) {
            employesInterface.setOnClickListener(v -> {
                // Remplacer EmployeActivity.class par la classe réelle
                startActivity(new Intent(AcceuilRhActivity.this, EmployeActivity.class));
                //Toast.makeText(this, "Accès Employés", Toast.LENGTH_SHORT).show();
            });
        }

        if (profileInterface != null) {
            profileInterface.setOnClickListener(v -> {
                // Remplacer ProfileActivity.class par la classe réelle
                startActivity(new Intent(AcceuilRhActivity.this, ProfileActivity.class));
               // Toast.makeText(this, "Accès Profile", Toast.LENGTH_SHORT).show();
            });
        }

        // if (logoutButton != null) {
        //     logoutButton.setOnClickListener(v -> logoutUser());
        // }
    }

    /**
     * Gère la déconnexion de l'utilisateur.
     */
    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Déconnexion réussie.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AcceuilRhActivity.this, MainActivity.class);
        // Empêche l'utilisateur de revenir en arrière sur l'écran d'accueil avec le bouton "retour"
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}