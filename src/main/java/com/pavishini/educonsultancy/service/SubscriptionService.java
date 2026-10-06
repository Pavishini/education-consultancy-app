package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.CourseRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CourseRepository courseRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, CourseRepository courseRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.courseRepository = courseRepository;
    }

    public Subscription subscribe(User student, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (subscriptionRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalStateException("You have already subscribed to this course.");
        }

        Subscription subscription = new Subscription(student, course, LocalDateTime.now(), "ACTIVE");
        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getSubscriptionsForStudent(User student) {
        return subscriptionRepository.findByStudentOrderBySubscribedAtDesc(student);
    }

    public Subscription getById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
    }

    public void markPaid(Subscription subscription) {
        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new IllegalStateException("Only active subscriptions can be marked as paid.");
        }

        subscription.setStatus("PAID");
        subscriptionRepository.save(subscription);
    }

    public long countAll() {
        return subscriptionRepository.count();
    }
}
