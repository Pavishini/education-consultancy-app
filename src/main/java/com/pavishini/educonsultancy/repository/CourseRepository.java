package com.pavishini.educonsultancy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByCategoryIgnoreCase(String category);
    List<Course> findByLevelIgnoreCase(String level);
}
