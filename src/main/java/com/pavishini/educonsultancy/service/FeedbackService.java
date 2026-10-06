package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Feedback;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submitFeedback(User student, Course course, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (feedbackRepository.existsByStudentAndCourse(student, course)) {
            Feedback existing = feedbackRepository.findByStudentAndCourse(student, course).orElseThrow();
            existing.setRating(rating);
            existing.setComment(comment);
            existing.setCreatedAt(LocalDateTime.now());
            return feedbackRepository.save(existing);
        }

        Feedback feedback = new Feedback(student, course, rating, comment, LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbackByCourse(Course course) {
        return feedbackRepository.findByCourseOrderByCreatedAtDesc(course);
    }

    public double getAverageRating(Course course) {
        List<Feedback> feedbacks = getFeedbackByCourse(course);
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        double total = feedbacks.stream().mapToInt(Feedback::getRating).sum();
        return total / feedbacks.size();
    }
}
