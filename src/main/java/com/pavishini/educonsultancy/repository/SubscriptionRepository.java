package com.pavishini.educonsultancy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query("""
            SELECT c.title, COUNT(s.id)
            FROM Subscription s JOIN s.course c
            GROUP BY c.id, c.title
            ORDER BY COUNT(s.id) DESC, c.title ASC
            """)
    List<Object[]> findCourseSubscriptionCounts();

    List<Subscription> findByStudent(User student);
    List<Subscription> findByStudentOrderBySubscribedAtDesc(User student);
    boolean existsByStudentAndCourse(User student, Course course);
}
