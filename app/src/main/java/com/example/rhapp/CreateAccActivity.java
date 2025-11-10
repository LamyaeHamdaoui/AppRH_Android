package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rhapp.databinding.ActivityCreateAccBinding;

public class CreateAccActivity extends AppCompatActivity {

    ActivityCreateAccBinding binding;
    MyDataBase databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreateAccBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new MyDataBase(this);


        binding.valideCreateAcc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String email = binding.email.getText().toString().trim();
                String motDePasse = binding.motDePasse.getText().toString().trim();
                String confirmerMDP = binding.confirmerMDP.getText().toString().trim();

                if(email.isEmpty() || motDePasse.isEmpty() || confirmerMDP.isEmpty()){
                    Toast.makeText(CreateAccActivity.this, "C'est obligatoire de remplir tous les champs", Toast.LENGTH_SHORT).show();
                } else {
                    if(motDePasse.equals(confirmerMDP)){
                        Boolean checkUserEmail = databaseHelper.checkEmail(email);
                        if(!checkUserEmail){ // CORRECTION: ! au lieu de == false
                            // Ici vous devez aussi récupérer nom et prénom si votre layout les a
                            // Pour l'instant, je mets des valeurs par défaut
                            Boolean insert = databaseHelper.insertData("Nom", "Prénom", email, motDePasse);
                            if(insert){ // CORRECTION: == true enlevé
                                Toast.makeText(CreateAccActivity.this, "Votre compte a été créé avec succès", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), ValidationEmailActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(CreateAccActivity.this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(CreateAccActivity.this, "Il y a déjà un compte avec cet email", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CreateAccActivity.this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        binding.connecterInterface.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}