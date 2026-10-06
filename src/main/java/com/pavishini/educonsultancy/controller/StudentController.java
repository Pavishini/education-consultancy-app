package com.pavishini.educonsultancy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Feedback;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.service.CourseService;
import com.pavishini.educonsultancy.service.FeedbackService;
import com.pavishini.educonsultancy.service.PaymentService;
import com.pavishini.educonsultancy.service.SubscriptionService;
import com.pavishini.educonsultancy.service.UserService;
import com.pavishini.educonsultancy.service.WishlistService;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CourseService courseService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final WishlistService wishlistService;
    private final FeedbackService feedbackService;

    public StudentController(CourseService courseService, SubscriptionService subscriptionService,
                              PaymentService paymentService, UserService userService,
                              WishlistService wishlistService, FeedbackService feedbackService) {
        this.courseService = courseService;
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
        this.userService = userService;
        this.wishlistService = wishlistService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("student", student);
        model.addAttribute("subscriptionCount", subscriptionService.getSubscriptionsForStudent(student).size());
        model.addAttribute("wishlistCount", wishlistService.countWishlist(student));
        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String browseCourses(@RequestParam(required = false) String category,
                                @RequestParam(required = false) String level,
                                @RequestParam(required = false) String minPrice,
                                @RequestParam(required = false) String maxPrice,
                                @RequestParam(required = false) String duration,
                                Authentication authentication,
                                Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("courses", CourseService.filterCourses(courseService.getAllCourses(), category, level, minPrice, maxPrice, duration));
        model.addAttribute("category", category);
        model.addAttribute("level", level);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("duration", duration);
        model.addAttribute("wishlistCourses", wishlistService.getWishlistCourses(student));
        return "student/courses";
    }

    @PostMapping("/subscribe/{courseId}")
    public String subscribe(@PathVariable Long courseId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User student = userService.getCurrentUser(authentication);

        try {
            subscriptionService.subscribe(student, courseId);
            redirectAttributes.addFlashAttribute("success", "You have successfully subscribed to this course.");
            return "redirect:/student/subscriptions";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student/courses";
        }
    }

    @GetMapping("/subscriptions")
    public String mySubscriptions(Authentication authentication, Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("subscriptions", subscriptionService.getSubscriptionsForStudent(student));
        return "student/subscriptions";
    }

    @GetMapping("/pay/{subscriptionId}")
    public String payPage(@PathVariable Long subscriptionId, Authentication authentication, Model model) {
        Subscription subscription = subscriptionService.getById(subscriptionId);
        assertSubscriptionOwner(subscription, authentication);
        model.addAttribute("subscription", subscription);
        model.addAttribute("razorpayConfigured", paymentService.isGatewayConfigured());
        return "student/payment";
    }

    @PostMapping("/pay/{subscriptionId}/order")
    public String createPaymentOrder(@PathVariable Long subscriptionId,
                                     Authentication authentication,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Subscription subscription = subscriptionService.getById(subscriptionId);
        assertSubscriptionOwner(subscription, authentication);
        try {
            com.pavishini.educonsultancy.entity.Payment payment = paymentService.createOrder(subscription);
            model.addAttribute("subscription", subscription);
            model.addAttribute("razorpayKeyId", paymentService.getGatewayKeyId());
            model.addAttribute("razorpayOrderId", payment.getGatewayOrderId());
            model.addAttribute("paymentAmountPaise", Math.round(payment.getAmount() * 100));
            return "student/checkout";
        } catch (IllegalStateException | IllegalArgumentException | RestClientException ex) {
            redirectAttributes.addFlashAttribute("paymentError", ex.getMessage());
            return "redirect:/student/pay/" + subscriptionId;
        }
    }

    @PostMapping("/pay/{subscriptionId}/confirm")
    public String confirmPayment(@PathVariable Long subscriptionId,
                                 @RequestParam("razorpay_order_id") String orderId,
                                 @RequestParam("razorpay_payment_id") String paymentId,
                                 @RequestParam("razorpay_signature") String signature,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        Subscription subscription = subscriptionService.getById(subscriptionId);
        assertSubscriptionOwner(subscription, authentication);
        try {
            paymentService.confirmCheckout(subscription, orderId, paymentId, signature);
            return "redirect:/student/transactions";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("paymentError", ex.getMessage());
            return "redirect:/student/pay/" + subscriptionId;
        }
    }

    private void assertSubscriptionOwner(Subscription subscription, Authentication authentication) {
        User student = userService.getCurrentUser(authentication);
        if (!student.getId().equals(subscription.getStudent().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("This subscription belongs to another student.");
        }
    }

    @GetMapping("/transactions")
    public String transactions(Authentication authentication, Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("payments", paymentService.getPaymentsForStudent(student));
        return "student/transactions";
    }

    @PostMapping("/wishlist/{courseId}")
    public String addToWishlist(@PathVariable Long courseId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = userService.getCurrentUser(authentication);
        Course course = courseService.getCourseById(courseId);
        try {
            wishlistService.addToWishlist(student, course);
            redirectAttributes.addFlashAttribute("success", "Course saved to your wishlist.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/courses";
    }

    @GetMapping("/wishlist")
    public String wishlist(Authentication authentication, Model model) {
        User student = userService.getCurrentUser(authentication);
        model.addAttribute("wishlist", wishlistService.getWishlistCourses(student));
        return "student/wishlist";
    }

    @PostMapping("/wishlist/remove/{courseId}")
    public String removeFromWishlist(@PathVariable Long courseId, Authentication authentication, RedirectAttributes redirectAttributes) {
        User student = userService.getCurrentUser(authentication);
        Course course = courseService.getCourseById(courseId);
        wishlistService.removeFromWishlist(student, course);
        redirectAttributes.addFlashAttribute("success", "Course removed from wishlist.");
        return "redirect:/student/wishlist";
    }

    @GetMapping("/feedback/{courseId}")
    public String feedbackPage(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", courseService.getCourseById(courseId));
        return "student/feedback";
    }

    @PostMapping("/feedback/{courseId}")
    public String submitFeedback(@PathVariable Long courseId, Authentication authentication,
                                @RequestParam int rating, @RequestParam(required = false) String comment,
                                RedirectAttributes redirectAttributes) {
        User student = userService.getCurrentUser(authentication);
        Course course = courseService.getCourseById(courseId);
        Feedback feedback = feedbackService.submitFeedback(student, course, rating, comment);
        redirectAttributes.addFlashAttribute("success", "Thanks for your feedback! Your rating of " + feedback.getRating() + " was saved.");
        return "redirect:/student/courses";
    }
}
