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


    public Optional<User> findByUserId(String username) {

        return userRepository.findByUsername(username);
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Finds a user by their user type.
     */
    public Optional<User> findByUsertype(String usertype) {
        return userRepository.findByUsertype(usertype);
    }

    /**
     * Adds a new user based on the creator's role.
     * Superadmin can add Admin.
     * Admin can add Lecture.
     */
    public User addUser(User newUser, String creatorUsername) throws Exception {
        Optional<User> creatorOptional = userRepository.findByUsername(creatorUsername);
        if (creatorOptional.isEmpty()) {
            throw new Exception("Creator user not found");
        }
        User creator = creatorOptional.get();
        String creatorType = creator.getUsertype();

        if (creatorType == null) creatorType = "";

        // Automatically assign role based on creator
        if (creatorType.equalsIgnoreCase("Superadmin")) {
            newUser.setUsertype("Admin");
        } else if (creatorType.equalsIgnoreCase("Admin")) {
            newUser.setUsertype("Lecture");
        } else {
            throw new Exception("You are not authorized to add users");
        }

        // Check if user exists
        if (userRepository.findByUsername(newUser.getUserID()).isPresent()) {
            throw new Exception("Username already exists");
        }
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new Exception("Email already exists");
        }

        return userRepository.save(newUser);
    }
}