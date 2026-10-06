package com.pavishini.educonsultancy.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.CourseRepository;
import com.pavishini.educonsultancy.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CourseRepository courseRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureDemoUser("admin@educonsult.com", "Admin", Role.ADMIN, "admin123");
        ensureDemoUser("counsellor@educonsult.com", "Counsellor", Role.COUNSELLOR, "counsellor123");
        ensureDemoUser("student@educonsult.com", "Student", Role.STUDENT, "student123");

        if (courseRepository.count() == 0) {
            Course course1 = new Course("Study Abroad Counselling", "One-on-one guidance for choosing universities, countries, and courses abroad.", "4 weeks", 4999.0);
            course1.setCategory("International Education");
            course1.setLevel("Beginner");
            courseRepository.save(course1);

            Course course2 = new Course("IELTS Preparation", "Structured training covering all four IELTS modules with mock tests.", "6 weeks", 6999.0);
            course2.setCategory("Language");
            course2.setLevel("Intermediate");
            courseRepository.save(course2);

            Course course3 = new Course("Visa Application Support", "End-to-end help with visa documentation, SOPs, and interview preparation.", "2 weeks", 2999.0);
            course3.setCategory("Visa");
            course3.setLevel("Advanced");
            courseRepository.save(course3);

            Course course4 = new Course("Career Coaching", "Build your global career plan with market guidance and action steps.", "5 weeks", 3999.0);
            course4.setCategory("Career");
            course4.setLevel("Beginner");
            courseRepository.save(course4);

            System.out.println(">>> Seeded 4 sample courses");
        }
    }

    private void ensureDemoUser(String email, String fullName, Role role, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User(fullName, email, passwordEncoder.encode(rawPassword), role);
            user.setStatus("ACTIVE");
            userRepository.save(user);
            System.out.println(">>> Seeded " + role.name().toLowerCase() + " login -> " + email + " / " + rawPassword);
            return;
        }

        boolean needsPasswordReset = user.getRole() != role || !passwordEncoder.matches(rawPassword, user.getPassword());
        if (needsPasswordReset) {
            user.setFullName(fullName);
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setStatus("ACTIVE");
            userRepository.save(user);
            System.out.println(">>> Updated demo account -> " + email + " / " + rawPassword);
        }
    }
}
