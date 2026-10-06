package com.pavishini.educonsultancy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pavishini.educonsultancy.service.CourseService;
import com.pavishini.educonsultancy.service.PaymentService;
import com.pavishini.educonsultancy.service.UserService;

@Controller
@RequestMapping("/counsellor")
public class CounsellorController {

    private final CourseService courseService;
    private final PaymentService paymentService;
    private final UserService userService;

    public CounsellorController(CourseService courseService, PaymentService paymentService, UserService userService) {
        this.courseService = courseService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCourses", courseService.countCourses());
        model.addAttribute("totalStudents", userService.getStudents().size());
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        return "counsellor/dashboard";
    }

    @GetMapping("/students")
    public String students(@RequestParam(required = false, defaultValue = "ALL") String status, Model model) {
        model.addAttribute("students", userService.getStudentsByStatus(status));
        model.addAttribute("selectedStatus", status);
        return "counsellor/students";
    }

    @GetMapping("/payments")
    public String payments(@RequestParam(required = false, defaultValue = "ALL") String status, Model model) {
        model.addAttribute("payments", paymentService.getByStatus(status));
        model.addAttribute("selectedStatus", status);
        return "counsellor/payments";
    }
}
