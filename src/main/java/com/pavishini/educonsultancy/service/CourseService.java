package com.pavishini.educonsultancy.service;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    public Course addCourse(Course course) {
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    public long countCourses() {
        return courseRepository.count();
    }

    public static List<Course> filterCourses(List<Course> courses, String category, String level, String minPrice, String maxPrice, String duration) {
        List<Course> filtered = new ArrayList<>();

        for (Course course : courses) {
            boolean matchesCategory = category == null || category.isBlank() || category.equalsIgnoreCase("ALL") ||
                    (course.getCategory() != null && course.getCategory().equalsIgnoreCase(category));
            boolean matchesLevel = level == null || level.isBlank() || level.equalsIgnoreCase("ALL") ||
                    (course.getLevel() != null && course.getLevel().equalsIgnoreCase(level));
            boolean matchesMinPrice = minPrice == null || minPrice.isBlank() || course.getPrice() >= Double.parseDouble(minPrice);
            boolean matchesMaxPrice = maxPrice == null || maxPrice.isBlank() || course.getPrice() <= Double.parseDouble(maxPrice);
            boolean matchesDuration = duration == null || duration.isBlank() || duration.equalsIgnoreCase("ALL") ||
                    (course.getDuration() != null && course.getDuration().toLowerCase().contains(duration.toLowerCase()));

            if (matchesCategory && matchesLevel && matchesMinPrice && matchesMaxPrice && matchesDuration) {
                filtered.add(course);
            }
        }

        return filtered;
    }
}
