package com.pavishini.educonsultancy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Feedback;
import com.pavishini.educonsultancy.entity.User;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByCourseOrderByCreatedAtDesc(Course course);
    Optional<Feedback> findByStudentAndCourse(User student, Course course);
    boolean existsByStudentAndCourse(User student, Course course);
}
