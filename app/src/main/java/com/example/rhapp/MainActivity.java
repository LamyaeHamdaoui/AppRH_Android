package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rhapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    MyDataBase databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // INITIALISER le binding AVANT setContentView
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new MyDataBase(this);

        binding.connecteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String email = binding.emailBox.getText().toString().trim();
                String motDePasse = binding.motDePasseBox.getText().toString().trim();
                if(email.isEmpty() || motDePasse.isEmpty()){
                    Toast.makeText(MainActivity.this, "C'est obligatoire de remplir tous les champs", Toast.LENGTH_SHORT).show();
                }else{
                    Boolean checkCredentials = databaseHelper.checkEmailPassword(email, motDePasse);
                    if(checkCredentials){
                        Toast.makeText(MainActivity.this, "Se connecter avec succès", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), AcceuilRhActivity.class);
                        startActivity(intent);
                    }else {
                        Toast.makeText(MainActivity.this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        binding.createAccBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, CreateAccActivity.class);
                startActivity(intent);
            }
        });

        binding.forgottenPasswordBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, ForgottenPasswordActivity.class);
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