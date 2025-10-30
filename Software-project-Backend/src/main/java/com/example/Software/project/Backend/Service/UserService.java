package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    // Ensure the UserRepository has the findByUsername method:
    // Optional<User> findByUsername(String username);
    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticates a user using their username and password.
     * This is the core logic used by the /api/auth/login endpoint.
     * * @param username The User ID (username) of the user.
     * @param password The password of the user.
     * @return An Optional containing the User if authentication is successful, otherwise Optional.empty().
     */
    public Optional<User> authenticateUser(String username, String password) {
        // CORRECTED: Find the user by their actual field name (username)
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Check if the provided password matches the stored password
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their username (which is mapped as the User_ID).
     */
    public Optional<User> findByUserId(String username) {
        // Using findByUsername, assuming it is defined in the Repository
        // as the method that correctly queries the username field.
        return userRepository.findByUsername(username);
    }

    /**
     * Finds a user by their email address.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Finds a user by their user type.
     */
    public Optional<User> findByUsertype(String usertype) {
        return userRepository.findByUsertype(usertype);
    }
}