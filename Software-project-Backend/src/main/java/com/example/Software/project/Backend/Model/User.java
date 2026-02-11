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
    private String UserID;


    @Column(name = "email", unique = true, nullable = false)
    private String email;


    @Column(name = "password", nullable = false)
    private String password;


    @Column(name = "user_type")
    private String usertype;


    public User() {
    }


    public User(String UserId, String email, String password, String usertype) {
        this.UserID=UserId;
        this.email = email;
        this.password = password;
        this.usertype = usertype;
    }



    public String getAdminId() {
        return UserID;
    }

    public void setAdminId(String adminId) {
        this.UserID=UserID;
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
        this.usertype = usertype;
    }
}