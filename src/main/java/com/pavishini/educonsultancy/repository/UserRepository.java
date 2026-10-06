package com.pavishini.educonsultancy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByStatus(String status);
    Optional<User> findByPasswordResetToken(String token);
    Optional<User> findByEmailIgnoreCase(String email);
}
