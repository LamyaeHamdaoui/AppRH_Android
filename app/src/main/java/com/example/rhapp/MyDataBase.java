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

    // Base
    public static final String DATABASE_NAME = "rhapplication.db";
    public static final int DATABASE_VERSION = 1;

    // Table + colonnes
    public static final String TABLE_UTILISATEUR = "Utilisateur";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NOM = "nom";
    public static final String COLUMN_PRENOM = "prenom";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_MOT_DE_PASSE_HASH = "motDePasseHash";
    public static final String COLUMN_DATE_NAISSANCE = "dateNaissance";
    public static final String COLUMN_SEXE = "sexe";

    private static final String TAG = "DATABASE_HELPER";

    public MyDataBase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        String createTableQuery = "CREATE TABLE " + TABLE_UTILISATEUR + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NOM + " TEXT NOT NULL, " +
                COLUMN_PRENOM + " TEXT NOT NULL, " +
                COLUMN_DATE_NAISSANCE + " TEXT, " +
                COLUMN_SEXE + " TEXT, " +
                COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COLUMN_MOT_DE_PASSE_HASH + " TEXT NOT NULL)";

        db.execSQL(createTableQuery);
        Log.d(TAG, "Table Utilisateur créée avec succès");

        insererUtilisateurTest(db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_UTILISATEUR);
        onCreate(db);
    }


    // ---------------------
    //   HACHAGE PASSWORD
    // ---------------------
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
            return null;
        }
    }


    // --------------------------------------------------
    //   USER PAR DÉFAUT (admin)
    // --------------------------------------------------
    private void insererUtilisateurTest(SQLiteDatabase db) {
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOM, "Admin");
            values.put(COLUMN_PRENOM, "RH");
            values.put(COLUMN_EMAIL, "admin@rh.com");

            String hashedPassword = hashPassword("password123");
            if (hashedPassword == null) return;

            values.put(COLUMN_MOT_DE_PASSE_HASH, hashedPassword);
            values.put(COLUMN_DATE_NAISSANCE, "01/01/1990");
            values.put(COLUMN_SEXE, "Masculin");

            db.insert(TABLE_UTILISATEUR, null, values);

        } catch (Exception e) {
            Log.e(TAG, "Erreur insertion utilisateur test: " + e.getMessage());
        }
    }


    // --------------------------------------------------
    //   INSERTION UTILISATEUR
    // --------------------------------------------------
    public Boolean insertData(String nom, String prenom, String dateNaissance,
                              String sexe, String email, String motDePasse) {

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            String hashedPassword = hashPassword(motDePasse);
            if (hashedPassword == null) return false;

            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NOM, nom);
            contentValues.put(COLUMN_PRENOM, prenom);
            contentValues.put(COLUMN_DATE_NAISSANCE, dateNaissance);
            contentValues.put(COLUMN_SEXE, sexe);
            contentValues.put(COLUMN_EMAIL, email);
            contentValues.put(COLUMN_MOT_DE_PASSE_HASH, hashedPassword);

            long result = db.insert(TABLE_UTILISATEUR, null, contentValues);
            return result != -1;

        } catch (Exception e) {
            Log.e(TAG, "Erreur insertion: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }


    public Boolean insertData(String nom, String prenom, String email, String motDePasse) {
        return insertData(nom, prenom, "", "", email, motDePasse);
    }


    // --------------------------------------------------
    //   CHECK EMAIL
    // --------------------------------------------------
    public Boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT id FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + "=?",
                    new String[]{email}
            );
            return cursor.getCount() > 0;

        } catch (Exception e) {
            Log.e(TAG, "Erreur checkEmail: " + e.getMessage());
            return false;

        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }


    // --------------------------------------------------
    //   LOGIN
    // --------------------------------------------------
    public Boolean checkEmailPassword(String email, String motDePasse) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_MOT_DE_PASSE_HASH +
                            " FROM " + TABLE_UTILISATEUR +
                            " WHERE " + COLUMN_EMAIL + "=?",
                    new String[]{email}
            );

            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(0);
                String providedHash = hashPassword(motDePasse);
                return storedHash.equals(providedHash);
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Erreur checkEmailPassword: " + e.getMessage());
            return false;

        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }


    // --------------------------------------------------
    //   NOM COMPLET
    // --------------------------------------------------
    public String getNomUtilisateur(String email) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String nomComplet = "";

        try {
            cursor = db.rawQuery(
                    "SELECT " + COLUMN_NOM + ", " + COLUMN_PRENOM +
                            " FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + "=?",
                    new String[]{email}
            );

            if (cursor.moveToFirst()) {
                String nom = cursor.getString(0);
                String prenom = cursor.getString(1);
                nomComplet = prenom + " " + nom;
            }

            return nomComplet;

        } catch (Exception e) {
            Log.e(TAG, "Erreur getNomUtilisateur: " + e.getMessage());
            return "";

        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }


    // --------------------------------------------------
    //   GET USER SAFELY
    // --------------------------------------------------
    public Cursor getUserData(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_UTILISATEUR + " WHERE " + COLUMN_EMAIL + "=?",
                new String[]{email}
        );
        // L’appelant DOIT fermer le Cursor ET la DB.
    }


    // --------------------------------------------------
    //   UPDATE
    // --------------------------------------------------
    public Boolean updateUserData(String email, String nom, String prenom,
                                  String dateNaissance, String sexe) {

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NOM, nom);
            values.put(COLUMN_PRENOM, prenom);
            values.put(COLUMN_DATE_NAISSANCE, dateNaissance);
            values.put(COLUMN_SEXE, sexe);

            int rows = db.update(TABLE_UTILISATEUR, values, COLUMN_EMAIL + "=?",
                    new String[]{email});

            return rows > 0;

        } catch (Exception e) {
            Log.e(TAG, "Erreur updateUserData: " + e.getMessage());
            return false;

        } finally {
            db.close();
        }
    }


    // --------------------------------------------------
    //   DELETE TABLE
    // --------------------------------------------------
    public boolean clearUserTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM " + TABLE_UTILISATEUR);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur clearUserTable: " + e.getMessage());
            return false;
        } finally {
            db.close();
        }
    }
}
