package com.example.Software.project.Backend.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_access")
public class UserAccess {
    @Id
    private String username;
    private LocalDateTime firstAccess;
    private LocalDateTime lastAccess;

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public LocalDateTime getFirstAccess() { return firstAccess; }
    public void setFirstAccess(LocalDateTime t) { this.firstAccess = t; }
    public LocalDateTime getLastAccess() { return lastAccess; }
    public void setLastAccess(LocalDateTime t) { this.lastAccess = t; }
}