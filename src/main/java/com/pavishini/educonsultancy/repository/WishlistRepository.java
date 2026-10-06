package com.pavishini.educonsultancy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByStudentOrderByAddedAtDesc(User student);
    Optional<Wishlist> findByStudentAndCourse(User student, Course course);
    boolean existsByStudentAndCourse(User student, Course course);
}
