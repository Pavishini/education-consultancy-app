package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.UserRepository;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_shouldEncodePasswordAndSaveUser() {
        // Protects accounts from storing raw passwords and verifies new users start active.
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        User saved = userService.registerUser("Alice Johnson", "alice@example.com", "Password123", Role.STUDENT);

        assertNotNull(saved);
        assertEquals("encoded-password", saved.getPassword());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Role.STUDENT, saved.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_shouldRejectDuplicateEmailBeforeSaving() {
        // Enforces unique account emails and prevents a duplicate registration from being persisted.
        String email = "alice@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.registerUser("Alice Johnson", email, "Password123", Role.STUDENT));

        assertEquals("Email already registered: " + email, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyOtp_shouldRejectWrongCode() {
        // Prevents a valid, unexpired OTP from accepting an unrelated code.
        User user = new User("Alice Johnson", "alice@example.com", "encoded", Role.STUDENT);
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        assertFalse(userService.verifyOtp(user, "654321"));
    }

    @Test
    void verifyOtp_shouldRejectExpiredCode() {
        // Prevents an expired OTP from remaining usable after its five-minute window.
        User user = new User("Alice Johnson", "alice@example.com", "encoded", Role.STUDENT);
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusSeconds(1));

        assertFalse(userService.verifyOtp(user, "123456"));
    }

    @Test
    void verifyOtp_shouldAcceptCorrectUnexpiredCode() {
        // Confirms the legitimate user can verify the active OTP they were issued.
        User user = new User("Alice Johnson", "alice@example.com", "encoded", Role.STUDENT);
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        assertTrue(userService.verifyOtp(user, "123456"));
    }

    @Test
    void resetPassword_shouldRejectExpiredToken() {
        // Prevents an expired reset link from changing the account password.
        User user = new User("Alice Johnson", "alice@example.com", "old-hash", Role.STUDENT);
        user.setPasswordResetToken("expired-token");
        user.setPasswordResetExpiry(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByPasswordResetToken("expired-token")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.resetPassword("expired-token", "NewPassword123"));

        assertEquals("Reset token expired.", exception.getMessage());
        assertEquals("old-hash", user.getPassword());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_shouldChangePasswordAndConsumeToken() {
        // Ensures a valid reset updates the password and makes the reset link single-use.
        User user = new User("Alice Johnson", "alice@example.com", "old-hash", Role.STUDENT);
        user.setPasswordResetToken("valid-token");
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByPasswordResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");

        userService.resetPassword("valid-token", "NewPassword123");

        assertEquals("new-hash", user.getPassword());
        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetExpiry());
        verify(userRepository).save(user);
    }

    @Test
    void deleteStudent_shouldRejectUnknownUser() {
        // Prevents deletion from silently succeeding when the requested ID does not exist.
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteStudent(42L));

        assertEquals("Student not found: 42", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteStudent_shouldRejectNonStudentAccount() {
        // Protects admin accounts from being deleted through the student-management operation.
        User admin = new User("Admin User", "admin@example.com", "encoded", Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteStudent(7L));

        assertEquals("Only student accounts can be removed from this section.", exception.getMessage());
        verify(userRepository, never()).delete(any(User.class));
    }
}
