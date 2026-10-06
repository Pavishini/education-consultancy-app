package com.pavishini.educonsultancy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.service.UserService;

@Controller
@RequestMapping("/student")
public class StudentProfileController {

    private final UserService userService;

    public StudentProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("student", student);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                               @RequestParam String phoneNumber,
                               @RequestParam String educationBackground,
                               @RequestParam String preferredCourse,
                               Model model) {
        User student = userService.getCurrentUser(authentication);
        userService.updateProfile(student, phoneNumber, educationBackground, preferredCourse);
        model.addAttribute("student", student);
        model.addAttribute("success", "Profile updated successfully.");
        return "student/profile";
    }
}
