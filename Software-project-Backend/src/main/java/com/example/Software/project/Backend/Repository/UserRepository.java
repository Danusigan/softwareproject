package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // CORRECTED: 'username' is the actual field name in the User entity.
    Optional<User> findByUsername(String username);

    // Note: If you want to use the getter/setter convention for the property name,
    // it would also work with findByUsername, as the field is 'username'.
    // The previous findByUserID was incorrect because 'userID' is not a field.

    Optional<User> findByUsertype(String usertype);
    Optional<User> findByEmail(String email);
}