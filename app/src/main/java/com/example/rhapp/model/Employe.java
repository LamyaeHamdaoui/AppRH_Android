package com.example.rhapp.model;

import com.google.firebase.Timestamp;


public class Employe  {
    private String id;
    private String nom;
    private String prenom;
    private String emailPro;
    private String role;
    private String photo;
    private String poste;
    private String departement;
    private Timestamp dateEmbauche;
    private int soldeConge;
    private String  telephone;
    private boolean compteCree;
    private String userId;



    public Employe () { }
    public Employe(String id, String nom, String prenom, String email,
                   String role, String photo, String poste, String departement,
                   Timestamp dateEmbauche, int soldeConge, String telephone, String userId,boolean compteCree )  {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.emailPro = email;
        this.role = role;
        this.photo = photo;
        this.poste = poste;
        this.departement = departement;
        this.dateEmbauche = dateEmbauche;
        this.soldeConge = soldeConge;
        this.telephone=telephone;
        this.userId=userId;
        this.compteCree=compteCree;

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

    public String getEmail(){ return emailPro;}
    public void setEmail(String email){
        this.emailPro = email; }


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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public boolean getCompteCree() {
        return compteCree;
    }

    public void setCompteCree(boolean compteCree) {
        this.compteCree = compteCree;
    }







}
