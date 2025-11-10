package com.example.rhapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

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
    public static final String COLUMN_MOT_DE_PASSE = "motDePasse";

    public MyDataBase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Création de la table Utilisateur
        String createTableQuery = "CREATE TABLE " + TABLE_UTILISATEUR + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NOM + " TEXT NOT NULL, " +
                COLUMN_PRENOM + " TEXT NOT NULL, " +
                COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COLUMN_MOT_DE_PASSE + " TEXT NOT NULL)";

        db.execSQL(createTableQuery);
        Log.d("DATABASE", "Table Utilisateur créée avec succès");

        // Insérer un utilisateur de test (optionnel)
        insererUtilisateurTest(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UTILISATEUR);
        onCreate(db);
    }

    // Méthode pour insérer un utilisateur de test
    private void insererUtilisateurTest(SQLiteDatabase db) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOM, "Admin");
            values.put(COLUMN_PRENOM, "RH");
            values.put(COLUMN_EMAIL, "admin@rh.com");
            values.put(COLUMN_MOT_DE_PASSE, "password123");

            long result = db.insert(TABLE_UTILISATEUR, null, values);
            if (result != -1) {
                Log.d("DATABASE", "Utilisateur test inséré avec succès");
            }
        } catch (Exception e) {
            Log.e("DATABASE", "Erreur insertion utilisateur test: " + e.getMessage());
        }
    }

    // Méthode pour insérer un nouvel utilisateur
    public Boolean insertData(String nom, String prenom, String email, String motDePasse) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NOM, nom);
            contentValues.put(COLUMN_PRENOM, prenom);
            contentValues.put(COLUMN_EMAIL, email);
            contentValues.put(COLUMN_MOT_DE_PASSE, motDePasse);

            long result = db.insert(TABLE_UTILISATEUR, null, contentValues);

            Log.d("DATABASE", "Insertion utilisateur - Email: " + email + ", Succès: " + (result != -1));

            return result != -1;

        } catch (Exception e) {
            Log.e("DATABASE", "Erreur insertion: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }

    // Vérifier si un email existe déjà
    public Boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                    new String[]{email}
            );

            boolean exists = cursor.getCount() > 0;
            Log.d("DATABASE", "Vérification email: " + email + ", Existe: " + exists);

            return exists;

        } catch (Exception e) {
            Log.e("DATABASE", "Erreur vérification email: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    // Vérifier email et mot de passe pour la connexion
    public Boolean checkEmailPassword(String email, String motDePasse) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " +
                            COLUMN_EMAIL + " = ? AND " + COLUMN_MOT_DE_PASSE + " = ?",
                    new String[]{email, motDePasse}
            );

            boolean credentialsValides = cursor.getCount() > 0;
            Log.d("DATABASE", "Connexion - Email: " + email + ", Valide: " + credentialsValides);

            return credentialsValides;

        } catch (Exception e) {
            Log.e("DATABASE", "Erreur vérification connexion: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    // Méthode pour récupérer les informations d'un utilisateur par email
    public String getNomUtilisateur(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_NOM + ", " + COLUMN_PRENOM +
                            " FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + " = ?",
                    new String[]{email}
            );

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressWarnings("Range") String nom = cursor.getString(cursor.getColumnIndex(COLUMN_NOM));
                @SuppressWarnings("Range") String prenom = cursor.getString(cursor.getColumnIndex(COLUMN_PRENOM));
                return prenom + " " + nom;
            }

            return "";

        } catch (Exception e) {
            Log.e("DATABASE", "Erreur récupération nom: " + e.getMessage());
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }
}