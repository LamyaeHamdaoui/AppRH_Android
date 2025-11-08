package com.example.rhapp;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDataBase extends SQLiteOpenHelper {
    public static final String databaseName = "rhapplication.db";

    public MyDataBase(@Nullable Context context) {
        super(context, "rhapplication.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Utilisateur(id INTEGER PRIMARY KEY AUTOINCREMENT, nom TEXT NOT NULL, prenom TEXT NOT NULL, email TEXT NOT NULL UNIQUE, motDePasse TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Utilisateur");
        onCreate(db);
    }

    public Boolean insertData(String nom, String prenom, String email, String motDePasse){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("nom", nom);
        contentValues.put("prenom", prenom);
        contentValues.put("email", email);
        contentValues.put("motDePasse", motDePasse);
        long result = db.insert("Utilisateur", null, contentValues);
        db.close(); // IMPORTANT: fermer la connexion
        return result != -1;
    }

    public Boolean checkEmail(String email){
        SQLiteDatabase db = this.getReadableDatabase(); // Utiliser readable pour les requêtes
        Cursor cursor = db.rawQuery("SELECT * FROM Utilisateur WHERE email = ?", new String[]{email});
        boolean exists = cursor.getCount() > 0;
        cursor.close(); // IMPORTANT: fermer le cursor
        db.close(); // IMPORTANT: fermer la connexion
        return exists;
    }

    public Boolean checkEmailPassword(String email, String motDePasse){
        SQLiteDatabase db = this.getReadableDatabase(); // Utiliser readable pour les requêtes
        Cursor cursor = db.rawQuery("SELECT * FROM Utilisateur WHERE email = ? AND motDePasse = ?", new String[]{email, motDePasse});
        boolean exists = cursor.getCount() > 0;
        cursor.close(); // IMPORTANT: fermer le cursor
        db.close(); // IMPORTANT: fermer la connexion
        return exists;
    }
}