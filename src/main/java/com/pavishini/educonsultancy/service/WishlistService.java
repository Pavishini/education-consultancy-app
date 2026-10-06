package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.entity.Wishlist;
import com.pavishini.educonsultancy.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    public WishlistService(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    public void addToWishlist(User student, Course course) {
        if (wishlistRepository.existsByStudentAndCourse(student, course)) {
            throw new IllegalStateException("This course is already in your wishlist.");
        }
        wishlistRepository.save(new Wishlist(student, course, LocalDateTime.now()));
    }

    public void removeFromWishlist(User student, Course course) {
        wishlistRepository.findByStudentAndCourse(student, course)
            .ifPresent(wishlistRepository::delete);
    }

    public List<Course> getWishlistCourses(User student) {
        return wishlistRepository.findByStudentOrderByAddedAtDesc(student).stream()
                .map(Wishlist::getCourse)
                .toList();
    }

    public boolean isWishlisted(User student, Course course) {
        return wishlistRepository.existsByStudentAndCourse(student, course);
    }

    public long countWishlist(User student) {
        return wishlistRepository.findByStudentOrderByAddedAtDesc(student).size();
    }
}
