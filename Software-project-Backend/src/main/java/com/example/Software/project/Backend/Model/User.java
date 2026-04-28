package com.example.Software.project.Backend.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "User")
public class User {

    @Id
    @Column(name = "User_ID")
    private String  username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_type")
    private String usertype;

    public User() {
    }

    public User(String username, String email, String password, String usertype) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.usertype = usertype;
    }

    // Getters and setters
    public String getUserID() {
        return username;
    }

    public void setUserID(String userID) {
        this.username = userID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsertype() {
        return usertype;
    }

    public void setUsertype(String usertype) {
        // Normalize usertype to have consistent casing
        if (usertype != null) {
            String normalized = usertype.toLowerCase().trim();
            // Validate role: must be one of superadmin, admin, or lecture
            if (!normalized.equals("superadmin") && !normalized.equals("admin") && !normalized.equals("lecture")) {
                throw new IllegalArgumentException(
                    "Invalid user type: " + usertype + ". Allowed types: superadmin, admin, lecture"
                );
            }
            this.usertype = normalized;
        } else {
            this.usertype = null;
        }
    }

    /**
     * Check if this user is a superadmin (can create admins)
     */
    public boolean isSuperAdmin() {
        return this.usertype != null && this.usertype.equals("superadmin");
    }

    /**
     * Check if this user is admin (can create lectures, CRUD modules and POs)
     */
    public boolean isAdmin() {
        return this.usertype != null && (this.usertype.equals("admin") || this.usertype.equals("superadmin"));
    }

    /**
     * Check if this user is a lecturer (can CRUD Los, upload marks)
     */
    public boolean isLecturer() {
        return this.usertype != null && this.usertype.equals("lecture");
    }
}