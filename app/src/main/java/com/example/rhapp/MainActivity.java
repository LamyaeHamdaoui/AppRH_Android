package com.example.rhapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MyDataBase databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation du ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialisation de la base de données
        databaseHelper = new MyDataBase(this);

        // Gestion du clic sur le bouton de connexion
        binding.connecteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seConnecter();
            }
        });

        // Gestion du clic sur "Créer un nouveau compte"
        binding.createAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateAccActivity.class);
                startActivity(intent);
            }
        });

        // Gestion du clic sur "Mot de passe oublié"
        binding.forgottenPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForgottenPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void seConnecter() {
        try {
            // Récupération des valeurs des champs
            String email = binding.emailBox.getText().toString().trim();
            String motDePasse = binding.motDePasseBox.getText().toString().trim();

            // Validation des champs
            if (email.isEmpty() || motDePasse.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérification des identifiants dans la base de données
            boolean identifiantsValides = databaseHelper.checkEmailPassword(email, motDePasse);

            if (identifiantsValides) {
                Toast.makeText(this, "Connexion réussie!", Toast.LENGTH_SHORT).show();

                // Redirection vers l'activité d'accueil
                Intent intent = new Intent(MainActivity.this, AcceuilRhActivity.class);
                intent.putExtra("EMAIL_UTILISATEUR", email);
                startActivity(intent);
                finish(); // Fermer cette activité

            } else {
                Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("MAIN_ACTIVITY", "Erreur lors de la connexion: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de la connexion", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fermer la connexion à la base de données si nécessaire
    }
}