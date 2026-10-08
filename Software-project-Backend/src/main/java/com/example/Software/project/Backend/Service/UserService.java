package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    @Autowired
    private AccountValidation accountValidation;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleService moduleService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Records a failed login attempt; locks the account for 15 minutes after 5 consecutive failures.
     * No-op if the username doesn't exist (avoids revealing account existence via lockout side-effects).
     */
    @org.springframework.transaction.annotation.Transactional
    public void recordFailedLogin(String username) {
        if (username == null || username.isBlank()) return;
        userRepository.lockLoginState(username).ifPresent(state -> {
            LocalDateTime now = LocalDateTime.now();
            if (state.getLockedUntil() != null && state.getLockedUntil().isAfter(now)) return;
            // An expired lock starts a fresh attempt window. Active locks are never extended.
            int previous = state.getLockedUntil() == null ? state.getFailedLoginAttempts() : 0;
            int attempts = Math.min(previous + 1, MAX_FAILED_LOGIN_ATTEMPTS);
            LocalDateTime until = attempts >= MAX_FAILED_LOGIN_ATTEMPTS ? now.plusMinutes(LOCKOUT_DURATION_MINUTES) : null;
            userRepository.updateLoginState(username, attempts, until);
        });
    }

    /**
     * Clears failed-attempt state on successful login.
     */
    @org.springframework.transaction.annotation.Transactional
    public void resetFailedLogins(String username) {
        userRepository.lockLoginState(username).ifPresent(state -> {
            // A concurrent failure may have locked the account after password verification.
            if (state.getLockedUntil() != null && state.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new org.springframework.security.authentication.LockedException("Account temporarily locked");
            }
            if (state.getFailedLoginAttempts() != 0 || state.getLockedUntil() != null) {
                userRepository.updateLoginState(username, 0, null);
            }
        });
    }

    /**
     * Authenticates a user using their username and password.
     */
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!user.isCurrentlyLocked() && passwordEncoder.matches(password, user.getPassword())) {
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
     * Lets a logged-in user change their own password. Verifies the current password,
     * enforces the password policy, and stores the new one BCrypt-encoded.
     */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        AccountValidation.password(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    /**
     * Lists all lecturers, for admin module-assignment pickers.
     */
    public List<User> findAllLecturers() {
        return userRepository.findAllByUsertype("lecture");
    }

    /**
     * Updates a lecturer's email (and password, if provided). Username/usertype are fixed.
     */
    public User updateLecturer(String username, String email, String password) throws Exception {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Lecturer not found: " + username));
        if (!"lecture".equalsIgnoreCase(user.getUsertype())) {
            throw new Exception(username + " is not a lecturer");
        }
        validateUpdate(user, email, password);
        if (email != null) {
            user.setEmail(email);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return saveAccount(user);
    }

    /**
     * Deletes a lecturer. Clears their module assignments first so the
     * module_lecturers foreign key doesn't block the delete.
     */
    public void deleteLecturer(String username) throws Exception {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Lecturer not found: " + username));
        if (!"lecture".equalsIgnoreCase(user.getUsertype())) {
            throw new Exception(username + " is not a lecturer");
        }
        moduleService.removeLecturerFromAllModules(username);
        userRepository.delete(user);
    }

    /**
     * Lists all admins, for the superadmin's Manage Admins page.
     */
    public List<User> findAllAdmins() {
        return userRepository.findAllByUsertype("admin");
    }

    /**
     * Updates an admin's email (and password, if provided). Username/usertype are fixed.
     */
    public User updateAdmin(String username, String email, String password) throws Exception {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Admin not found: " + username));
        if (!"admin".equalsIgnoreCase(user.getUsertype())) {
            throw new Exception(username + " is not an admin");
        }
        validateUpdate(user, email, password);
        if (email != null) {
            user.setEmail(email);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return saveAccount(user);
    }

    /**
     * Deletes an admin account.
     */
    public void deleteAdmin(String username) throws Exception {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("Admin not found: " + username));
        if (!"admin".equalsIgnoreCase(user.getUsertype())) {
            throw new Exception(username + " is not an admin");
        }
        userRepository.delete(user);
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

        String requestedRole = newUser.getUsertype();
        if (!("superadmin".equalsIgnoreCase(creatorType)
                && java.util.Set.of("admin", "lecture").contains(requestedRole == null ? "" : requestedRole))
                && !("admin".equalsIgnoreCase(creatorType) && "lecture".equals(requestedRole))) {
            throw new Exception("You are not authorized to add users");
        }
        // Lockout state is managed by the server, never by the creation request.
        newUser.setFailedLoginAttempts(0);
        newUser.setLockedUntil(null);

        AccountValidation.username(newUser.getUserID());
        accountValidation.email(newUser.getEmail());
        AccountValidation.password(newUser.getPassword());
        // Mirror case-insensitive identity uniqueness on the local MySQL database.
        if (userRepository.existsByUsernameIgnoreCase(newUser.getUserID())) {
            throw new Exception("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUsernameNot(newUser.getEmail(), newUser.getUserID())) {
            throw new Exception("Email already exists");
        }

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        return saveAccount(newUser);
    }

    /**
     * Creates a test user - for development/testing only
     */
    public User createTestUser(String username, String password, String email, String userType) throws Exception {
        AccountValidation.username(username);
        accountValidation.email(email);
        AccountValidation.password(password);
        // Check if user already exists
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new Exception("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUsernameNot(email, username)) {
            throw new Exception("Email already exists");
        }

        User testUser = new User();
        testUser.setUserID(username);
        testUser.setPassword(passwordEncoder.encode(password));
        testUser.setEmail(email);
        testUser.setUsertype(userType);

        return saveAccount(testUser);
    }

    private User saveAccount(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // The unique database constraints also protect simultaneous duplicate requests.
            throw new IllegalArgumentException("Username or email already exists, or the account details conflict with an existing record.");
        }
    }

    private void validateUpdate(User user, String email, String password) {
        // Validate the entire change before mutating the managed entity.
        if (email != null) {
            accountValidation.email(email);
            if (userRepository.existsByEmailIgnoreCaseAndUsernameNot(email, user.getUserID()))
                throw new IllegalArgumentException("Email already exists");
        }
        if (password != null && !password.isBlank()) AccountValidation.password(password);
    }
}
