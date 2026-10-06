package com.pavishini.educonsultancy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:educonsultancy-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-key-at-least-32-bytes-long-123456789"
})
class ControllerLayerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User("Alice Student", "student@educonsult.com", "encoded-password", Role.STUDENT));
        userRepository.save(new User("Admin User", "admin@educonsult.com", "encoded-password", Role.ADMIN));
    }

    @Test
    void loginPage_shouldRender() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Alice Student")
                        .param("email", "student@educonsult.com")
                        .param("password", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void studentRoutes_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/student/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void studentRoutes_shouldBeAccessibleToStudentUsers() throws Exception {
        mockMvc.perform(get("/student/courses")
                        .with(user("student@educonsult.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/courses"));
    }

    @Test
    void adminRoutes_shouldBeForbiddenForNonAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user("student@educonsult.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAnalytics_shouldRenderForAdminWithAllAnalyticsData() throws Exception {
        mockMvc.perform(get("/admin/analytics")
                        .with(user("admin@educonsult.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/analytics"))
                .andExpect(model().attributeExists("monthlyRevenue", "topCourses", "paymentStatusCounts"));
    }

    @Test
    void adminAnalytics_shouldBeForbiddenForStudent() throws Exception {
        mockMvc.perform(get("/admin/analytics")
                        .with(user("student@educonsult.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboard_shouldRedirectByRole() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("admin@educonsult.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        mockMvc.perform(get("/dashboard")
                        .with(user("student@educonsult.com").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/dashboard"));
    }

    @Test
    void razorpayWebhook_shouldBePublicAndRejectInvalidSignature() throws Exception {
        // Confirms Razorpay can reach the webhook without a session while forged requests are rejected.
        mockMvc.perform(post("/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt-invalid")
                        .header("X-Razorpay-Signature", "invalid-signature")
                        .contentType("application/json")
                        .content("{\"event\":\"payment.captured\"}"))
                .andExpect(status().isBadRequest());
    }
}
