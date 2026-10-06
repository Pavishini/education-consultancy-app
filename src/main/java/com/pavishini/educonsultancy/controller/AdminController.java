package com.pavishini.educonsultancy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.service.AnalyticsService;
import com.pavishini.educonsultancy.service.CourseService;
import com.pavishini.educonsultancy.service.PaymentService;
import com.pavishini.educonsultancy.service.SubscriptionService;
import com.pavishini.educonsultancy.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CourseService courseService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final AnalyticsService analyticsService;

    public AdminController(CourseService courseService, SubscriptionService subscriptionService,
                          PaymentService paymentService, UserService userService,
                          AnalyticsService analyticsService) {
        this.courseService = courseService;
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
        this.userService = userService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCourses", courseService.countCourses());
        model.addAttribute("totalSubscriptions", subscriptionService.countAll());
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        return "admin/dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("monthlyRevenue", analyticsService.getRevenueByMonth());
        model.addAttribute("topCourses", analyticsService.getTopCoursesBySubscriptionCount());
        model.addAttribute("paymentStatusCounts", analyticsService.getPaymentCountsByStatus());
        return "admin/analytics";
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("course", new Course());
        return "admin/add-course";
    }

    @GetMapping("/students")
    public String listStudents(Model model) {
        model.addAttribute("students", userService.getStudents());
        return "admin/students";
    }

    @PostMapping("/students/remove/{id}")
    public String removeStudent(@PathVariable Long id) {
        userService.deleteStudent(id);
        return "redirect:/admin/students";
    }

    @PostMapping("/courses/add")
    public String addCourse(@ModelAttribute Course course) {
        courseService.addCourse(course);
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }
}
