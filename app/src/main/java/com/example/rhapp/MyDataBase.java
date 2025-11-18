package com.example.rhapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.Nullable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyDataBase extends SQLiteOpenHelper {

    // Constantes pour la base de données
    public static final String DATABASE_NAME = "rhapplication.db";
    public static final int DATABASE_VERSION = 1;

    // Constantes pour la table Utilisateur
    public static final String TABLE_UTILISATEUR = "Utilisateur";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOM = "nom";
    public static final String COLUMN_PRENOM = "prenom";
    public static final String COLUMN_EMAIL = "email";
    // ATTENTION: Le nom de la colonne a été mis à jour pour refléter qu'il est haché
    public static final String COLUMN_MOT_DE_PASSE_HASH = "motDePasseHash";
    public static final String COLUMN_DATE_NAISSANCE = "dateNaissance"; // Camel Case
    public static final String COLUMN_SEXE = "sexe";
    private static final String TAG = "DATABASE_HELPER"; // Tag de log standardisé

    public MyDataBase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Création de la table Utilisateur
        // Utilisation de COLUMN_MOT_DE_PASSE_HASH
        String createTableQuery = "CREATE TABLE " + TABLE_UTILISATEUR + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NOM + " TEXT NOT NULL, " +
                COLUMN_PRENOM + " TEXT NOT NULL, " +
                COLUMN_DATE_NAISSANCE + " TEXT, " +
                COLUMN_SEXE + " TEXT, " +
                COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COLUMN_MOT_DE_PASSE_HASH + " TEXT NOT NULL)"; // Stockage du Hash

        db.execSQL(createTableQuery);
        Log.d(TAG, "Table Utilisateur créée avec succès");

        // Insérer un utilisateur de test (optionnel)
        insererUtilisateurTest(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Logique de mise à jour simple: détruire et recréer (attention en production!)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UTILISATEUR);
        onCreate(db);
    }

    // SIMULATION DE HACHAGE (Pour une application réelle, utilisez une librairie forte comme BCrypt)
    // Cette méthode est utilisée pour DEMONTRER la bonne pratique du hachage.
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Erreur de hachage: " + e.getMessage());
            return null; // ou gérer l'erreur de manière plus robuste
        }
    }

    // Méthode pour insérer un utilisateur de test
    private void insererUtilisateurTest(SQLiteDatabase db) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOM, "Admin");
            values.put(COLUMN_PRENOM, "RH");
            values.put(COLUMN_EMAIL, "admin@rh.com");
            // Hachage du mot de passe
            String hashedPassword = hashPassword("password123");
            if (hashedPassword == null) return; // Arrêter si le hachage échoue
            values.put(COLUMN_MOT_DE_PASSE_HASH, hashedPassword);
            values.put(COLUMN_DATE_NAISSANCE, "01/01/1990");
            values.put(COLUMN_SEXE, "Masculin");

            long result = db.insert(TABLE_UTILISATEUR, null, values);
            if (result != -1) {
                Log.d(TAG, "Utilisateur test inséré avec succès");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur insertion utilisateur test: " + e.getMessage());
        }
        // Pas besoin de db.close() ici car db est passé depuis onCreate et doit rester ouvert.
    }

    // Méthode principale pour insérer un nouvel utilisateur avec toutes les données
    public Boolean insertData(String nom, String prenom, String dateNaissance, String sexe,
                              String email, String motDePasse) {
        SQLiteDatabase db = this.getWritableDatabase();
        Boolean success = false;

        try {
            // Hachage du mot de passe avant insertion
            String hashedPassword = hashPassword(motDePasse);
            if (hashedPassword == null) return false;

            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NOM, nom);
            contentValues.put(COLUMN_PRENOM, prenom);
            contentValues.put(COLUMN_DATE_NAISSANCE, dateNaissance);
            contentValues.put(COLUMN_SEXE, sexe);
            contentValues.put(COLUMN_EMAIL, email);
            contentValues.put(COLUMN_MOT_DE_PASSE_HASH, hashedPassword); // Utilisation du Hash

            long result = db.insert(TABLE_UTILISATEUR, null, contentValues);
            success = result != -1;

            Log.d(TAG, "Insertion utilisateur - " +
                    "Nom: " + nom + ", " +
                    "Email: " + email + ", " +
                    "Succès: " + success);

        } catch (Exception e) {
            Log.e(TAG, "Erreur insertion: " + e.getMessage());
        } finally {
            db.close(); // Fermeture de la base de données
        }
        return success;
    }

    // Surcharge de la méthode pour compatibilité (sans date et sexe)
    public Boolean insertData(String nom, String prenom, String email, String motDePasse) {
        // Appel de la méthode complète avec des chaînes vides pour les champs optionnels
        return insertData(nom, prenom, "", "", email, motDePasse);
    }

    // Vérifier si un email existe déjà
    public Boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                    new String[]{email}
            );

            exists = cursor.getCount() > 0;
            Log.d(TAG, "Vérification email: " + email + ", Existe: " + exists);

        } catch (Exception e) {
            Log.e(TAG, "Erreur vérification email: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close(); // Fermeture du curseur
            }
            db.close(); // Fermeture de la base de données
        }
        return exists;
    }

    // Vérifier email et mot de passe pour la connexion
    public Boolean checkEmailPassword(String email, String motDePasse) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean credentialsValides = false;

        try {
            // 1. On vérifie l'existence de l'utilisateur par email
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_MOT_DE_PASSE_HASH + " FROM " + TABLE_UTILISATEUR +
                            " WHERE " + COLUMN_EMAIL + " = ?",
                    new String[]{email}
            );

            if (cursor.moveToFirst()) {
                // 2. On récupère le hash stocké
                String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOT_DE_PASSE_HASH));

                // 3. On hache le mot de passe fourni et on le compare au hash stocké
                String providedHash = hashPassword(motDePasse);
                if (providedHash != null && providedHash.equals(storedHash)) {
                    credentialsValides = true;
                }
            }

            Log.d(TAG, "Connexion - Email: " + email + ", Valide: " + credentialsValides);

        } catch (Exception e) {
            Log.e(TAG, "Erreur vérification connexion: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close(); // Fermeture du curseur
            }
            db.close(); // Fermeture de la base de données
        }
        return credentialsValides;
    }

    // Méthode pour récupérer les informations d'un utilisateur par email
    public String getNomUtilisateur(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String nomComplet = "";

        try {
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_NOM + ", " + COLUMN_PRENOM +
                            " FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                    new String[]{email}
            );

            if (cursor != null && cursor.moveToFirst()) {
                // Utilisation de getColumnIndexOrThrow standardisée
                String nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOM));
                String prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRENOM));
                nomComplet = prenom + " " + nom;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur récupération nom: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close(); // Fermeture du curseur
            }
            db.close(); // Fermeture de la base de données
        }
        return nomComplet;
    }

    // Méthode pour récupérer toutes les informations d'un utilisateur
    // ATTENTION: La méthode originale retournait un Cursor non fermé et une DB ouverte.
    // Il est plus sûr de retourner un type de données (ex: Cursor) et d'assurer que
    // l'appelant est informé qu'il doit le fermer, MAIS nous allons fermer la DB ici.
    public Cursor getUserData(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Le Cursor est retourné et doit être fermé par l'appelant.
        // Laissez la fermeture de la DB à la fin du cycle de vie de l'application
        // ou adaptez cette méthode pour retourner un objet POJO (plus sûr).
        // Pour rester fidèle à la signature, on retourne le Cursor, mais c'est risqué.
        return db.rawQuery(
                "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                new String[]{email}
        );
        // Note: db.close() n'est pas appelé ici car le Cursor retourné dépend toujours de la DB ouverte.
        // Si vous utilisez cette méthode, vous devez impérativement fermer le Cursor après usage.
    }

    // Correction de getUserData pour retourner un Cursor et éviter de laisser la DB ouverte
    // (Une approche plus professionnelle serait de retourner un objet User, pas un Cursor)
    public Cursor getUserDataSafe(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Fermeture dans la méthode appelante est recommandée pour les Cursors
        return db.rawQuery(
                "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                new String[]{email}
        );
    }


    public boolean clearUserTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        try {
            db.execSQL("DELETE FROM " + TABLE_UTILISATEUR);
            success = true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur vidage table: " + e.getMessage());
        } finally {
            db.close();
        }
        return success;
    }

    // Méthode pour mettre à jour les informations utilisateur
    public Boolean updateUserData(String email, String nom, String prenom, String dateNaissance, String sexe) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = 0;

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NOM, nom);
            contentValues.put(COLUMN_PRENOM, prenom);
            contentValues.put(COLUMN_DATE_NAISSANCE, dateNaissance);
            contentValues.put(COLUMN_SEXE, sexe);

            result = db.update(TABLE_UTILISATEUR, contentValues,
                    COLUMN_EMAIL + " = ?", new String[]{email});

            Log.d(TAG, "Mise à jour utilisateur - Email: " + email + ", Lignes modifiées: " + result);

        } catch (Exception e) {
            Log.e(TAG, "Erreur mise à jour: " + e.getMessage());
        } finally {
            db.close();
        }
        return result > 0;
    }
}