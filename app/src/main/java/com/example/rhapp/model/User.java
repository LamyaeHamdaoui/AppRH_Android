package com.example.rhapp.model;

public class User {
    public String nom;
    public String prenom;
    public String birthDate;
    public String role;
    public String sexe;
    public String email;
    public String createdAt;

    // Constructeur par défaut requis pour Firebase
    public User() {
    }
    public User(String nom, String prenom, String birthDate, String sexe, String email, String createdAt) {
        this.nom = nom;
        this.prenom = prenom;
        this.birthDate = birthDate;
        this.sexe = sexe;
        this.email = email;
        this.createdAt = createdAt;
    }

    public User(String nom, String prenom, String birthDate, String sexe,String role, String email, String createdAt) {
        this.nom = nom;
        this.prenom = prenom;
        this.birthDate = birthDate;
        this.sexe = sexe;
        this.role = role;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Vous pouvez ajouter des getters et setters ici si nécessaire,
    // mais Firebase fonctionne bien avec des champs publics.
}