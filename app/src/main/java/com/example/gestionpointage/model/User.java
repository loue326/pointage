package com.example.gestionpointage.model;

/**
 * Represente un utilisateur (employe ou administrateur).
 */
public class User {

    public int id;
    public String name;
    public String email;
    public String password;
    public String role;     // "admin" ou "employee"
    public String job;      // poste occupe

    public User() {
    }

    public User(int id, String name, String email, String password, String role, String job) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.job = job;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Initiales de l'utilisateur (pour l'avatar rond).
     */
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1 && parts[1].length() > 0) {
            sb.append(Character.toUpperCase(parts[1].charAt(0)));
        }
        return sb.toString();
    }
}
