package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.CourseRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

class SubscriptionServiceTest {

    private SubscriptionRepository subscriptionRepository;
    private CourseRepository courseRepository;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);
        courseRepository = mock(CourseRepository.class);
        subscriptionService = new SubscriptionService(subscriptionRepository, courseRepository);
    }

    @Test
    void subscribe_shouldCreateSubscriptionForNewCourse() {
        // Prevents valid course applications from being lost or created with the wrong status.
        User student = new User("Student One", "student@example.com", "secret", Role.STUDENT);
        Course course = new Course("Spring Boot Basics", "Intro to Spring", "6 Weeks", 1200.0);
        course.setId(7L);

        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(subscriptionRepository.existsByStudentAndCourse(student, course)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription subscription = subscriptionService.subscribe(student, 7L);

        assertEquals("ACTIVE", subscription.getStatus());
        assertEquals(student, subscription.getStudent());
        assertEquals(course, subscription.getCourse());
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void subscribe_shouldRejectDuplicateSubscription() {
        // Enforces the one-application-per-student-per-course business rule.
        User student = new User("Student One", "student@example.com", "secret", Role.STUDENT);
        Course course = new Course("Spring Boot Basics", "Intro to Spring", "6 Weeks", 1200.0);
        course.setId(7L);

        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(subscriptionRepository.existsByStudentAndCourse(student, course)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> subscriptionService.subscribe(student, 7L));

        assertEquals("You have already subscribed to this course.", exception.getMessage());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        }

        @Test
        void subscribe_shouldRejectNonexistentCourse() {
        // Prevents creating an application that references a course ID missing from the database.
        User student = new User("Student One", "student@example.com", "secret", Role.STUDENT);
        when(courseRepository.findById(404L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> subscriptionService.subscribe(student, 404L));

        assertEquals("Course not found: 404", exception.getMessage());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void markPaid_shouldUpdateSubscriptionStatus() {
        // Confirms successful payment completion is reflected in the subscription record.
        Subscription subscription = new Subscription(
                new User("Student One", "student@example.com", "secret", Role.STUDENT),
                new Course("Databases", "DB basics", "8 Weeks", 1500.0),
                LocalDateTime.now(),
                "ACTIVE"
        );

        subscriptionService.markPaid(subscription);

        assertEquals("PAID", subscription.getStatus());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void markPaid_shouldRejectCancelledSubscription() {
        // Prevents a cancelled subscription from being moved into the paid state.
        Subscription subscription = new Subscription(
                new User("Student One", "student@example.com", "secret", Role.STUDENT),
                new Course("Databases", "DB basics", "8 Weeks", 1500.0),
                LocalDateTime.now(),
                "CANCELLED"
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> subscriptionService.markPaid(subscription));

        assertEquals("Only active subscriptions can be marked as paid.", exception.getMessage());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }
}
