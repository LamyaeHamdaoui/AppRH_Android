package com.example.rhapp.model;

import com.google.firebase.Timestamp;
import java.util.Date;

/**
 * Modèle de données pour un utilisateur stocké dans la collection Firestore "Users".
 */
public class User {

    // Champs privés
    private String nom;
    private String prenom;
    private Date birthDate;
    private String sexe;
    private String role;
    private String email;
    private Timestamp createdAt;

    /**
     * Constructeur par défaut requis par Firebase.
     */
    public User() {
    }

    /**
     * Constructeur utilisé par CreateAccActivity pour enregistrer un nouvel utilisateur.
     */
    public User(String nom, String prenom, Date birthDate, String sexe, String role, String email, Timestamp createdAt) {
        this.nom = nom;
        this.prenom = prenom;
        this.birthDate = birthDate;
        this.sexe = sexe;
        this.role = role;
        this.email = email;
        this.createdAt = createdAt;
    }

    // --- Getters (Nécessaires pour la lecture par Firestore) ---

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getSexe() {
        return sexe;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // --- Setters (Optionnels) ---

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}