package com.example.Software.project.Backend.Service;

import com.example.Software.project.Backend.Model.User;
import com.example.Software.project.Backend.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService: the role (usertype) Spring Security relies on for
 * every @PreAuthorize/hasRole check comes solely from the DB row here — there is no
 * client-supplied role anywhere in the authentication path. If this class ever derived the
 * role from something else (e.g. a login-request field), it would be a privilege-escalation bug.
 */
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("loads a user's authority strictly from the stored usertype column")
    void loadUserByUsername_authorityComesFromStoredUsertype() {
        User user = new User("lecturer1", "lecturer1@example.com", "hashed-pw", "lecture");
        when(userRepository.findByUsername("lecturer1")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("lecturer1");

        assertEquals("lecturer1", details.getUsername());
        assertEquals("hashed-pw", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("lecture", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("an admin user resolves to the admin authority, not any client-supplied role")
    void loadUserByUsername_adminResolvesToAdminAuthority() {
        User user = new User("admin1", "admin1@example.com", "hashed-pw", "admin");
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("admin1");

        GrantedAuthority authority = details.getAuthorities().iterator().next();
        assertEquals("admin", authority.getAuthority());
    }

    @Test
    @DisplayName("throws UsernameNotFoundException for an unknown username")
    void loadUserByUsername_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> customUserDetailsService.loadUserByUsername("ghost"));
    }
}
