package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavishini.educonsultancy.entity.Course;
import com.pavishini.educonsultancy.entity.Payment;
import com.pavishini.educonsultancy.entity.RazorpayWebhookEvent;
import com.pavishini.educonsultancy.entity.Role;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.PaymentRepository;
import com.pavishini.educonsultancy.repository.RazorpayWebhookEventRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private RazorpayWebhookEventRepository webhookEventRepository;
    private RazorpayGateway razorpayGateway;
    private SubscriptionRepository subscriptionRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        webhookEventRepository = mock(RazorpayWebhookEventRepository.class);
        razorpayGateway = mock(RazorpayGateway.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        paymentService = new PaymentService(paymentRepository, webhookEventRepository, razorpayGateway,
                new ObjectMapper(), subscriptionRepository);
    }

    @Test
    void createOrder_shouldPersistPendingPaymentWithGatewayOrderId() {
        // Ensures checkout starts in PENDING and cannot count as revenue before gateway confirmation.
        Subscription subscription = activeSubscription();
        subscription.setId(12L);
        when(paymentRepository.findBySubscription(subscription)).thenReturn(Optional.empty());
        when(razorpayGateway.createOrder(80000, "subscription-12"))
                .thenReturn(new RazorpayGateway.RazorpayOrder("order_12", 80000, "INR"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createOrder(subscription);

        assertEquals(800.0, payment.getAmount());
        assertEquals("PENDING", payment.getStatus());
        assertEquals("order_12", payment.getGatewayOrderId());
        assertEquals(subscription, payment.getSubscription());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createOrder_shouldRetryFailedPaymentUsingExistingOrder() {
        // Allows a student to retry a declined attempt without creating a second Razorpay order.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        payment.setStatus("FAILED");
        payment.setFailureReason("declined");
        payment.setGatewayPaymentId("pay_failed");
        when(paymentRepository.findBySubscription(subscription)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment retried = paymentService.createOrder(subscription);

        assertEquals("PENDING", retried.getStatus());
        assertEquals("order_12", retried.getGatewayOrderId());
        assertNull(retried.getFailureReason());
        verify(razorpayGateway, never()).createOrder(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmCheckout_shouldVerifySignatureAndMarkPaymentSuccessful() {
        // Prevents unverified browser data from marking a subscription paid.
        Subscription subscription = activeSubscription();
        Payment payment = new Payment(subscription, 800.0, LocalDateTime.now(), "PENDING");
        payment.setGatewayOrderId("order_12");
        when(razorpayGateway.verifyCheckoutSignature("order_12", "pay_12", "valid-signature"))
                .thenReturn(true);
        when(paymentRepository.findBySubscription(subscription)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment confirmed = paymentService.confirmCheckout(subscription, "order_12", "pay_12", "valid-signature");

        assertEquals("SUCCESS", confirmed.getStatus());
        assertEquals("pay_12", confirmed.getGatewayPaymentId());
        assertEquals("PAID", subscription.getStatus());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void confirmCheckout_shouldRejectInvalidSignature() {
        // Prevents forged checkout callbacks from changing payment or subscription records.
        Subscription subscription = activeSubscription();
        when(razorpayGateway.verifyCheckoutSignature("order_12", "pay_12", "bad-signature"))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.confirmCheckout(subscription, "order_12", "pay_12", "bad-signature"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processWebhook_shouldAcceptValidCapturedPayment() {
        // Confirms a signed payment.captured webhook updates the stored payment and subscription once.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        String body = webhookPayload("payment.captured", "pay_12", "order_12", "captured");
        when(razorpayGateway.verifyWebhookSignature(body, "valid-signature")).thenReturn(true);
        when(webhookEventRepository.existsById("evt_capture")).thenReturn(false);
        when(paymentRepository.findByGatewayOrderId("order_12")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean processed = paymentService.processWebhook("evt_capture", "valid-signature", body);

        assertTrue(processed);
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals("PAID", subscription.getStatus());
        assertEquals("pay_12", payment.getGatewayPaymentId());
        verify(webhookEventRepository).save(any(RazorpayWebhookEvent.class));
    }

    @Test
    void processWebhook_shouldRejectDuplicateEventIdWithoutDoubleUpdating() {
        // Ensures a retried Razorpay webhook is ignored instead of being applied a second time.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        String body = webhookPayload("payment.captured", "pay_12", "order_12", "captured");
        when(razorpayGateway.verifyWebhookSignature(body, "valid-signature")).thenReturn(true);
        when(webhookEventRepository.existsById("evt_dup")).thenReturn(true);

        assertFalse(paymentService.processWebhook("evt_dup", "valid-signature", body));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventRepository, never()).save(any(RazorpayWebhookEvent.class));
    }

    @Test
    void processWebhook_shouldRejectInvalidSignature() {
        // Prevents forged webhook payloads from changing payment state without a valid Razorpay signature.
        String body = webhookPayload("payment.captured", "pay_12", "order_12", "captured");
        when(razorpayGateway.verifyWebhookSignature(body, "bad-signature")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.processWebhook("evt_invalid", "bad-signature", body));
        verify(webhookEventRepository, never()).save(any(RazorpayWebhookEvent.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processWebhook_shouldRejectAmountMismatch() {
        // Rejects webhook payloads whose amount does not match the outstanding subscription payment.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        String body = webhookPayload("payment.captured", "pay_12", "order_12", 50000L, "INR", "captured");
        when(razorpayGateway.verifyWebhookSignature(body, "valid-signature")).thenReturn(true);
        when(webhookEventRepository.existsById("evt_amount_mismatch")).thenReturn(false);
        when(paymentRepository.findByGatewayOrderId("order_12")).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.processWebhook("evt_amount_mismatch", "valid-signature", body));
        assertEquals("PENDING", payment.getStatus());
        verify(webhookEventRepository, never()).save(any(RazorpayWebhookEvent.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processWebhook_shouldProcessCaptureOnlyOnce() {
        // Ensures Razorpay webhook retries do not apply a payment event or status update twice.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        String body = webhookPayload("payment.captured", "pay_12", "order_12", "captured");
        when(razorpayGateway.verifyWebhookSignature(body, "valid-signature")).thenReturn(true);
        when(webhookEventRepository.existsById("evt_capture")).thenReturn(false, true);
        when(paymentRepository.findByGatewayOrderId("order_12")).thenReturn(Optional.of(payment));

        assertTrue(paymentService.processWebhook("evt_capture", "valid-signature", body));
        assertFalse(paymentService.processWebhook("evt_capture", "valid-signature", body));

        assertEquals("SUCCESS", payment.getStatus());
        assertEquals("PAID", subscription.getStatus());
        verify(webhookEventRepository).save(any(RazorpayWebhookEvent.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    void processWebhook_shouldRecordFailureWithoutMarkingSubscriptionPaid() {
        // Confirms declined gateway payments remain visibly failed and can be retried later.
        Subscription subscription = activeSubscription();
        Payment payment = pendingPayment(subscription);
        String body = webhookPayload("payment.failed", "pay_failed", "order_12", "declined");
        when(razorpayGateway.verifyWebhookSignature(body, "valid-signature")).thenReturn(true);
        when(webhookEventRepository.existsById("evt_failed")).thenReturn(false);
        when(paymentRepository.findByGatewayOrderId("order_12")).thenReturn(Optional.of(payment));

        assertTrue(paymentService.processWebhook("evt_failed", "valid-signature", body));

        assertEquals("FAILED", payment.getStatus());
        assertEquals("declined", payment.getFailureReason());
        assertEquals("ACTIVE", subscription.getStatus());
    }

    private Subscription activeSubscription() {
        User student = new User("Student One", "student@example.com", "secret", Role.STUDENT);
        Course course = new Course("Data Structures", "Hands-on DS", "4 Weeks", 800.0);
        return new Subscription(student, course, LocalDateTime.now(), "ACTIVE");
    }

    private Payment pendingPayment(Subscription subscription) {
        Payment payment = new Payment(subscription, 800.0, LocalDateTime.now(), "PENDING");
        payment.setGatewayOrderId("order_12");
        return payment;
    }

    private String webhookPayload(String event, String paymentId, String orderId, String status) {
        return webhookPayload(event, paymentId, orderId, 80000L, "INR", status);
    }

    private String webhookPayload(String event, String paymentId, String orderId, long amount, String currency, String status) {
        return "{\"event\":\"" + event + "\",\"payload\":{\"payment\":{\"entity\":{" +
                "\"id\":\"" + paymentId + "\",\"order_id\":\"" + orderId + "\"," +
                "\"amount\":" + amount + ",\"currency\":\"" + currency + "\",\"error_description\":\"" + status + "\"}}}}";
    }
}
