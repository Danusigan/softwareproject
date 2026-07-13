package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.PasswordResetToken;
import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.PasswordResetTokenRepository;
import com.example.Software.project.Backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles the forgot-password / reset-password flow. Works the same way
 * regardless of the account's usertype (superadmin/admin/lecture), since
 * all three roles share the same User table keyed by email.
 */
@Service
public class PasswordResetService {

    private static final long TOKEN_EXPIRY_MINUTES = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public void requestReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Don't reveal whether the email exists
            return;
        }

        User user = userOptional.get();
        tokenRepository.deleteByUserId(user.getUserID());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user.getUserID(), LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/resetpassword?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws Exception {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new Exception("This reset link is invalid or has expired."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new Exception("This reset link is invalid or has expired.");
        }

        User user = userRepository.findByUsername(resetToken.getUserId())
                .orElseThrow(() -> new Exception("Account no longer exists."));

        user.setPassword(newPassword);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
