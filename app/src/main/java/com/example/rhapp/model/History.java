package com.example.rhapp.model;

import com.google.firebase.Timestamp;

public class History {
    private String userId;
    private String status;
    private String details;
    private String time;
    private Timestamp timestamp;
    private String date;

    // Constructeurs
    public History() {}

    // Getters et setters pour tous les champs
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}