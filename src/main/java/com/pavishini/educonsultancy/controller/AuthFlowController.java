package com.pavishini.educonsultancy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.service.UserService;

@Controller
public class AuthFlowController {

    private final UserService userService;

    public AuthFlowController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam String email, Model model) {
        try {
            User user = userService.findByEmail(email);
            userService.createPasswordResetToken(email);
            model.addAttribute("success", "Password reset link created for " + user.getEmail() + ".");
            return "forgot-password";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", "No account found with that email.");
            return "forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String password, Model model) {
        try {
            userService.resetPassword(token, password);
            model.addAttribute("success", "Password changed successfully. Please log in again.");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "reset-password";
        }
    }

    @GetMapping("/otp-login")
    public String otpLoginPage() {
        return "otp-login";
    }

    @PostMapping("/otp-login")
    public String sendOtp(@RequestParam String email, Model model) {
        try {
            User user = userService.findByEmail(email);
            String otp = userService.createOtp(user);
            model.addAttribute("email", user.getEmail());
            model.addAttribute("success", "OTP sent to your email. Demo OTP: " + otp);
            return "otp-login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", "No account found with that email.");
            return "otp-login";
        }
    }

    @PostMapping("/otp-verify")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        try {
            User user = userService.findByEmail(email);
            if (userService.verifyOtp(user, otp)) {
                userService.clearOtp(user);
                model.addAttribute("success", "OTP verified successfully. You can now log in using your normal credentials.");
                return "login";
            }
            model.addAttribute("error", "Invalid or expired OTP.");
            return "otp-login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", "No account found with that email.");
            return "otp-login";
        }
    }
}
