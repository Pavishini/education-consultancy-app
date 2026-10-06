package com.pavishini.educonsultancy.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.pavishini.educonsultancy.entity.Course;

class CourseServiceTest {

    @Test
    void shouldFilterCoursesByCategoryLevelAndPrice() {
        // Ensures students only see courses that satisfy all selected search filters.
        Course python = new Course("Python", "Intro", "8 weeks", 1800.0);
        python.setCategory("Technology");
        python.setLevel("Beginner");

        Course design = new Course("UI Design", "Visual", "6 weeks", 1200.0);
        design.setCategory("Design");
        design.setLevel("Intermediate");

        List<Course> filtered = CourseService.filterCourses(
                List.of(python, design),
                "Technology",
                "Beginner",
                "1000",
                "2000",
                "8"
        );

        assertEquals(1, filtered.size());
        assertEquals("Python", filtered.get(0).getTitle());
    }
}
