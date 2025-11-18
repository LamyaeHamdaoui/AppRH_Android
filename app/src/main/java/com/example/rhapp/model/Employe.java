package com.example.rhapp.model;

public class Employe extends Utilisateur {
    private String photo;
    private String poste;
    private String departement;
    private String dateEmbauche;
    private int soldeConge;
    private String  telephone;


    public Employe () { }
    public Employe(String id, String nom, String prenom, String email, String motDePasse,
                   String role, String photo, String poste, String departement,
                   String dateEmbauche, int soldeConge, String telephone )  {
        super(id, nom, prenom, email , motDePasse, role);
        this.photo = photo;
        this.poste = poste;
        this.departement = departement;
        this.dateEmbauche = dateEmbauche;
        this.soldeConge = soldeConge;
        this.telephone=telephone;

    }

    // --- GETTERS & SETTERS ---

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

    public String getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(String dateEmbauche) {
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
