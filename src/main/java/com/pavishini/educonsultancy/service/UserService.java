package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerStudent(String fullName, String email, String rawPassword) {
        return registerUser(fullName, email, rawPassword, Role.STUDENT);
    }

    public User registerUser(String fullName, String email, String rawPassword, Role role) {
        if (emailTaken(email)) {
            throw new IllegalStateException("Email already registered: " + email);
        }

        User user = new User(fullName, email, passwordEncoder.encode(rawPassword), role);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    public List<User> getStudents() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .toList();
    }

    public List<User> getStudentsByStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return getStudents();
        }
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .filter(user -> user.getStatus() != null && user.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    public void deleteStudent(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only student accounts can be removed from this section.");
        }

        userRepository.delete(user);
    }

    public User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Logged in user not found: " + email));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void updateProfile(User user, String phoneNumber, String educationBackground, String preferredCourse) {
        user.setPhoneNumber(phoneNumber);
        user.setEducationBackground(educationBackground);
        user.setPreferredCourse(preferredCourse);
        userRepository.save(user);
    }

    public String createOtp(User user) {
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        return otp;
    }

    public boolean verifyOtp(User user, String code) {
        if (user.getOtpCode() == null || user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        return user.getOtpCode().equals(code);
    }

    public void clearOtp(User user) {
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }

    public User createPasswordResetToken(String email) {
        User user = findByEmail(email);
        String token = java.util.UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));
        return userRepository.save(user);
    }

    public User findByPasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));
    }

    public void resetPassword(String token, String newPassword) {
        User user = findByPasswordResetToken(token);
        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token expired.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
    }
}
