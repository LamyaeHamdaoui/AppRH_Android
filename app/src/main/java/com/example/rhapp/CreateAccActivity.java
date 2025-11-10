package com.example.rhapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rhapp.databinding.ActivityCreateAccBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateAccActivity extends AppCompatActivity {

    private ActivityCreateAccBinding binding;
    private MyDataBase databaseHelper;
    private Calendar calendar;
    private String selectedSexe = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation du ViewBinding
        binding = ActivityCreateAccBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialisation de la base de données
        databaseHelper = new MyDataBase(this);

        // Initialisation du calendrier pour la date de naissance
        calendar = Calendar.getInstance();

        // Configuration du sélecteur de date
        setupDatePicker();

        // Configuration du RadioGroup pour le sexe
        setupRadioGroup();

        // Gestion du clic sur le bouton Valider
        binding.valideCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                creerCompte();
            }
        });

        // Gestion du clic sur "Se connecter"
        binding.connecterInterface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish(); // Fermer cette activité
            }
        });
    }

    // Configuration du sélecteur de date
    private void setupDatePicker() {
        // Définir un écouteur de clic sur le champ date de naissance
        binding.birthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Définir un écouteur de focus pour ouvrir le date picker
        binding.birthDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showDatePickerDialog();
                }
            }
        });
    }

    // Affichage du dialogue de sélection de date
    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Formater la date en JJ/MM/AAAA
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
                        binding.birthDate.setText(dateFormat.format(calendar.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Optionnel: définir une date maximale (18 ans minimum par exemple)
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -18);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePickerDialog.show();
    }

    // Configuration du RadioGroup pour le sexe
    private void setupRadioGroup() {
        binding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = findViewById(checkedId);
                if (radioButton != null) {
                    selectedSexe = radioButton.getText().toString();
                    Log.d("SEXE", "Sexe sélectionné: " + selectedSexe);
                }
            }
        });
    }

    // Méthode principale pour créer le compte
    private void creerCompte() {
        try {
            // Récupération des valeurs des champs
            String nom = binding.nom.getText().toString().trim();
            String prenom = binding.prenom.getText().toString().trim();
            String dateNaissance = binding.birthDate.getText().toString().trim();
            String email = binding.email.getText().toString().trim();
            String motDePasse = binding.motDePasse.getText().toString().trim();
            String confirmerMDP = binding.confirmerMDP.getText().toString().trim();

            // Validation des champs
            if (!validerFormulaire(nom, prenom, dateNaissance, email, motDePasse, confirmerMDP)) {
                return; // Arrêter si validation échoue
            }

            // Vérifier si l'email existe déjà
            if (databaseHelper.checkEmail(email)) {
                Toast.makeText(this, "Un compte avec cet email existe déjà", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insérer l'utilisateur dans la base de données
            boolean insertionReussie = databaseHelper.insertData(nom, prenom, email, motDePasse);

            if (insertionReussie) {
                Toast.makeText(this, "Compte créé avec succès!", Toast.LENGTH_SHORT).show();
                Log.d("COMPTE", "Nouvel utilisateur: " + nom + " " + prenom + " - " + email);

                // Rediriger vers l'activité de connexion
                Intent intent = new Intent(CreateAccActivity.this, MainActivity.class);
                intent.putExtra("EMAIL", email); // Optionnel: passer l'email
                startActivity(intent);
                finish(); // Fermer cette activité

            } else {
                Toast.makeText(this, "Erreur lors de la création du compte", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("CREATE_ACCOUNT", "Erreur: " + e.getMessage());
            Toast.makeText(this, "Une erreur est survenue", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode de validation du formulaire
    private boolean validerFormulaire(String nom, String prenom, String dateNaissance,
                                      String email, String motDePasse, String confirmerMDP) {

        // Validation du nom
        if (TextUtils.isEmpty(nom)) {
            binding.nom.setError("Le nom est obligatoire");
            binding.nom.requestFocus();
            return false;
        }

        // Validation du prénom
        if (TextUtils.isEmpty(prenom)) {
            binding.prenom.setError("Le prénom est obligatoire");
            binding.prenom.requestFocus();
            return false;
        }

        // Validation de la date de naissance (optionnelle)
        if (TextUtils.isEmpty(dateNaissance)) {
            binding.birthDate.setError("La date de naissance est obligatoire");
            binding.birthDate.requestFocus();
            return false;
        }

        // Validation du format de date
        if (!isValidDate(dateNaissance)) {
            binding.birthDate.setError("Format de date invalide (JJ/MM/AAAA)");
            binding.birthDate.requestFocus();
            return false;
        }

        // Validation du sexe
        if (selectedSexe.isEmpty()) {
            Toast.makeText(this, "Veuillez sélectionner votre sexe", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validation de l'email
        if (TextUtils.isEmpty(email)) {
            binding.email.setError("L'email est obligatoire");
            binding.email.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.setError("Format d'email invalide");
            binding.email.requestFocus();
            return false;
        }

        // Validation du mot de passe
        if (TextUtils.isEmpty(motDePasse)) {
            binding.motDePasse.setError("Le mot de passe est obligatoire");
            binding.motDePasse.requestFocus();
            return false;
        }

        if (motDePasse.length() < 6) {
            binding.motDePasse.setError("Le mot de passe doit contenir au moins 6 caractères");
            binding.motDePasse.requestFocus();
            return false;
        }

        // Validation de la confirmation du mot de passe
        if (TextUtils.isEmpty(confirmerMDP)) {
            binding.confirmerMDP.setError("Veuillez confirmer le mot de passe");
            binding.confirmerMDP.requestFocus();
            return false;
        }

        if (!motDePasse.equals(confirmerMDP)) {
            binding.confirmerMDP.setError("Les mots de passe ne correspondent pas");
            binding.confirmerMDP.requestFocus();
            return false;
        }

        return true; // Toutes les validations sont passées
    }

    // Validation du format de date
    private boolean isValidDate(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            dateFormat.setLenient(false); // Strict validation
            dateFormat.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyage des ressources si nécessaire
    }
}