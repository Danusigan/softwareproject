package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    interface LoginAttemptState {
        int getFailedLoginAttempts();
        java.time.LocalDateTime getLockedUntil();
    }

    // Read scalar state under a row lock so a previously loaded User cannot supply stale counters.
    @org.springframework.data.jpa.repository.Query(value = "SELECT failed_login_attempts AS failedLoginAttempts, locked_until AS lockedUntil FROM `user` WHERE User_ID = :username FOR UPDATE", nativeQuery = true)
    Optional<LoginAttemptState> lockLoginState(@org.springframework.data.repository.query.Param("username") String username);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE `user` SET failed_login_attempts = :attempts, locked_until = :until WHERE User_ID = :username", nativeQuery = true)
    void updateLoginState(@org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("attempts") int attempts,
        @org.springframework.data.repository.query.Param("until") java.time.LocalDateTime until);

    Optional<User> findByUsername(String username);
    Optional<User> findByUsertype(String usertype);
    List<User> findAllByUsertype(String usertype);
    Optional<User> findByEmail(String email);
    
    // Add explicit method to find by the username field (which is actually userID)
    // This should work since the field name is 'username' in the entity
    default Optional<User> findByUserID(String userID) {
        return findByUsername(userID);
    }
}
