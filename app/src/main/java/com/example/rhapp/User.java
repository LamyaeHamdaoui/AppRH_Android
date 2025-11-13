package com.example.rhapp;

public class User {
    private String nom;
    private String prenom;
    private String birthDate;
    private String sexe;
    private String email;
    private String createdAt;

    // Constructeur vide obligatoire pour Firebase
    public User() {}

    // Constructeur complet
    public User(String nom, String prenom, String birthDate, String sexe, String email, String createdAt) {
        this.nom = nom;
        this.prenom = prenom;
        this.birthDate = birthDate;
        this.sexe = sexe;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters et setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}