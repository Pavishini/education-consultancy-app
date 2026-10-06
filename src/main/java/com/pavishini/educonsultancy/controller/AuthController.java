package com.pavishini.educonsultancy.controller;

import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam(defaultValue = "STUDENT") Role profile,
                            Model model) {

        if (userService.emailTaken(email)) {
            model.addAttribute("error", "An account with this email already exists.");
            return "register";
        }

        userService.registerUser(fullName, email, password, profile);
        model.addAttribute("success", "Account created. You can log in now.");
        return "login";
    }
}
