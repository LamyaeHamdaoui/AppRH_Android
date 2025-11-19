package com.example.rhapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.*;

import com.example.rhapp.model.User;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ValidationEmailActivity extends AppCompatActivity {

    private EditText code1, code2, code3, code4;
    private Button suivantBtn;

    private String expectedCode;
    private String userNom, userPrenom, userBirthDate, userSexe, userRole, userEmail, userPassword;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        retrieveData();

        if (!isDataValid()) {
            Toast.makeText(this, "Erreur : données manquantes.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupCodeInputSystem();

        Toast.makeText(this, "Code : " + expectedCode, Toast.LENGTH_LONG).show();

        suivantBtn.setOnClickListener(v -> validateCode());
    }

    private void retrieveData() {
        Intent i = getIntent();
        userNom = i.getStringExtra("NOM");
        userPrenom = i.getStringExtra("PRENOM");
        userBirthDate = i.getStringExtra("NAISS");
        userSexe = i.getStringExtra("SEXE");
        userRole = i.getStringExtra("ROLE");
        userEmail = i.getStringExtra("EMAIL");
        userPassword = i.getStringExtra("PASSWORD");
        expectedCode = i.getStringExtra("CODE");
    }

    private boolean isDataValid() {
        return userNom != null && userPrenom != null && userBirthDate != null &&
                userSexe != null && userRole != null &&
                userEmail != null && userPassword != null &&
                expectedCode != null;
    }

    private void initializeViews() {
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        suivantBtn = findViewById(R.id.suivant2);
    }

    private void setupCodeInputSystem() {
        EditText[] fields = {code1, code2, code3, code4};

        for (int i = 0; i < fields.length; i++) {
            int index = i;

            fields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < 3) {
                        fields[index + 1].requestFocus();
                    }
                }
            });

            fields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        fields[index].getText().length() == 0 &&
                        index > 0) {

                    fields[index - 1].setText("");
                    fields[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private void validateCode() {
        String entered =
                code1.getText().toString() +
                        code2.getText().toString() +
                        code3.getText().toString() +
                        code4.getText().toString();

        if (entered.length() != 4) {
            Toast.makeText(this, "Code incomplet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!entered.equals(expectedCode)) {
            Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show();
            return;
        }

        createFirebaseAccount();
    }

    private void createFirebaseAccount() {
        suivantBtn.setEnabled(false);
        suivantBtn.setText("Création...");

        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = mAuth.getCurrentUser();
                        if (fbUser != null) saveUserToDatabase(fbUser.getUid());
                    }
                    else {
                        suivantBtn.setEnabled(true);
                        suivantBtn.setText("Suivant");

                        Exception e = task.getException();
                        String message = "Erreur";

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            message = "Cet email est déjà utilisé.";
                        } else if (e != null) {
                            message = e.getMessage();
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid) {

        String createdAt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        User user = new User(
                userNom,
                userPrenom,
                userBirthDate,
                userSexe,
                userRole,
                userEmail,
                createdAt
        );

        mDatabase.child(uid).setValue(user)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Compte créé avec succès", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_LONG).show();
                    }

                    mAuth.signOut();

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
    }
}
