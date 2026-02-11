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
            // Convert to lowercase for consistency
            this.usertype = usertype.toLowerCase();
        } else {
            this.usertype = null;
        }
    }
}