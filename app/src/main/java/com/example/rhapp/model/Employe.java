package com.example.rhapp.model;

import com.google.firebase.Timestamp;


public class Employe  {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String photo;
    private String poste;
    private String departement;
    private Timestamp dateEmbauche;
    private int soldeConge;
    private String  telephone;



    public Employe () { }
    public Employe(String id, String nom, String prenom, String email,
                   String role, String photo, String poste, String departement,
                   Timestamp dateEmbauche, int soldeConge, String telephone )  {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.role = role;
        this.photo = photo;
        this.poste = poste;
        this.departement = departement;
        this.dateEmbauche = dateEmbauche;
        this.soldeConge = soldeConge;
        this.telephone=telephone;

    }

    // --- GETTERS & SETTERS ---
    public String getId(){ return id;}
    public void setId (String id){
        this.id = id; }

    public String getNom(){ return nom;}
    public void setNom (String nom){
        this.nom = nom; }

    public String getPrenom(){ return prenom;}
    public void setPrenom (String prenom){
        this.prenom = prenom; }

    public String getEmail(){ return email;}
    public void setEmail(String email){
        this.email = email; }


    public String getRole(){ return role;}
    public void setRole(String role){
        this.role = role; }

    public String getNomComplet() {
        return prenom + " " + nom;
    }


    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPoste() {
        return poste;
    }

    public void setPoste(String poste) {
        this.poste = poste;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public Timestamp getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(Timestamp dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }

    public int getSoldeConge() {
        return soldeConge;
    }

    public void setSoldeConge(int soldeConge) {
        this.soldeConge = soldeConge;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }


}
