package com.pavishini.educonsultancy.config;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.CourseRepository;
import com.pavishini.educonsultancy.repository.UserRepository;

class DataSeederTest {

    @Test
    void run_shouldResetDemoPasswordsWhenExistingUsersHaveWrongCredentials() {
        UserRepository userRepository = mock(UserRepository.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.existsByEmail("admin@educonsult.com")).thenReturn(true);
        when(userRepository.existsByEmail("counsellor@educonsult.com")).thenReturn(true);
        when(userRepository.existsByEmail("student@educonsult.com")).thenReturn(true);

        when(userRepository.findByEmail("admin@educonsult.com")).thenReturn(Optional.of(new User("Admin", "admin@educonsult.com", "oldAdminHash", Role.ADMIN)));
        when(userRepository.findByEmail("counsellor@educonsult.com")).thenReturn(Optional.of(new User("Counsellor", "counsellor@educonsult.com", "oldCounsellorHash", Role.COUNSELLOR)));
        when(userRepository.findByEmail("student@educonsult.com")).thenReturn(Optional.of(new User("Student", "student@educonsult.com", "oldStudentHash", Role.STUDENT)));

        when(passwordEncoder.matches("admin123", "oldAdminHash")).thenReturn(false);
        when(passwordEncoder.matches("counsellor123", "oldCounsellorHash")).thenReturn(false);
        when(passwordEncoder.matches("student123", "oldStudentHash")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("newAdminHash");
        when(passwordEncoder.encode("counsellor123")).thenReturn("newCounsellorHash");
        when(passwordEncoder.encode("student123")).thenReturn("newStudentHash");
        when(courseRepository.count()).thenReturn(0L);

        DataSeeder seeder = new DataSeeder(userRepository, courseRepository, passwordEncoder);
        seeder.run();

        verify(userRepository, atLeastOnce()).save(any(User.class));
    }
}
