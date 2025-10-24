package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserID(String userID);  // Changed from findByUserId to findByUserID
    Optional<User> findByUsertype(String usertype);
    Optional<User> findByEmail(String email);
}