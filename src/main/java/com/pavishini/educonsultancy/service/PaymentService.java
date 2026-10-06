package com.pavishini.educonsultancy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavishini.educonsultancy.entity.Payment;
import com.pavishini.educonsultancy.entity.RazorpayWebhookEvent;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;
import com.pavishini.educonsultancy.repository.PaymentRepository;
import com.pavishini.educonsultancy.repository.RazorpayWebhookEventRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayWebhookEventRepository webhookEventRepository;
    private final RazorpayGateway razorpayGateway;
    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          RazorpayWebhookEventRepository webhookEventRepository,
                          RazorpayGateway razorpayGateway,
                          ObjectMapper objectMapper,
                          SubscriptionRepository subscriptionRepository) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.razorpayGateway = razorpayGateway;
        this.objectMapper = objectMapper;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Payment createOrder(Subscription subscription) {
        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new IllegalStateException("Only active subscriptions can be paid.");
        }

        Optional<Payment> existing = paymentRepository.findBySubscription(subscription);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if ("SUCCESS".equals(payment.getStatus())) {
                throw new IllegalStateException("This subscription has already been paid.");
            }
            if ("PENDING".equals(payment.getStatus())) {
                return payment;
            }
            payment.setStatus("PENDING");
            payment.setFailureReason(null);
            payment.setPaymentDate(LocalDateTime.now());
            return paymentRepository.save(payment);
        }

        long amountPaise = Math.round(subscription.getCourse().getPrice() * 100);
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        RazorpayGateway.RazorpayOrder order = razorpayGateway.createOrder(amountPaise,
                "subscription-" + subscription.getId());
        if (order.amountPaise() != amountPaise || !"INR".equals(order.currency())) {
            throw new IllegalStateException("Razorpay order amount or currency did not match the subscription.");
        }

        Payment payment = new Payment(subscription, amountPaise / 100.0, LocalDateTime.now(), "PENDING");
        payment.setGatewayOrderId(order.id());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment confirmCheckout(Subscription subscription, String orderId, String paymentId, String signature) {
        if (!razorpayGateway.verifyCheckoutSignature(orderId, paymentId, signature)) {
            throw new IllegalArgumentException("Invalid Razorpay checkout signature.");
        }

        Payment payment = paymentRepository.findBySubscription(subscription)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found."));
        if (!orderId.equals(payment.getGatewayOrderId())) {
            throw new IllegalArgumentException("Payment order does not match this subscription.");
        }
        markSuccessful(payment, paymentId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public boolean processWebhook(String eventId, String signature, String payload) {
        if (!razorpayGateway.verifyWebhookSignature(payload, signature)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature.");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Razorpay event ID is required.");
        }
        if (webhookEventRepository.existsById(eventId)) {
            return false;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Razorpay webhook payload.", ex);
        }

        String eventType = root.path("event").asText();
        if (!"payment.captured".equals(eventType) && !"payment.failed".equals(eventType)) {
            return false;
        }

        JsonNode entity = root.path("payload").path("payment").path("entity");
        String orderId = entity.path("order_id").asText();
        String paymentId = entity.path("id").asText();
        Payment payment = paymentRepository.findByGatewayOrderId(orderId).orElse(null);
        if (payment == null || paymentId.isBlank()) {
            return false;
        }
        long receivedAmount = entity.path("amount").asLong(-1);
        long expectedAmount = Math.round(payment.getAmount() * 100);
        if (receivedAmount != expectedAmount || !"INR".equals(entity.path("currency").asText())) {
            throw new IllegalArgumentException("Razorpay webhook amount or currency did not match the order.");
        }

        webhookEventRepository.save(new RazorpayWebhookEvent(eventId, LocalDateTime.now()));
        if ("payment.captured".equals(eventType)) {
            markSuccessful(payment, paymentId);
        } else if (!"SUCCESS".equals(payment.getStatus())) {
            payment.setStatus("FAILED");
            payment.setGatewayPaymentId(paymentId);
            payment.setFailureReason(entity.path("error_description").asText("Payment failed."));
            payment.setPaymentDate(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        return true;
    }

    private void markSuccessful(Payment payment, String paymentId) {
        if (!"SUCCESS".equals(payment.getStatus())) {
            payment.setStatus("SUCCESS");
            payment.setGatewayPaymentId(paymentId);
            payment.setFailureReason(null);
            payment.setPaymentDate(LocalDateTime.now());
        }
        Subscription subscription = payment.getSubscription();
        if (subscription == null) {
            throw new IllegalStateException("Payment is not associated with a subscription.");
        }
        if ("ACTIVE".equals(subscription.getStatus())) {
            subscription.setStatus("PAID");
            subscriptionRepository.save(subscription);
        }
    }

    public List<Payment> getPaymentsForStudent(User student) {
        return paymentRepository.findBySubscription_StudentOrderByPaymentDateDesc(student);
    }

    public List<Payment> getByStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByStatusOrderByPaymentDateDesc(status.toUpperCase());
    }

    public boolean alreadyPaid(Subscription subscription) {
        return paymentRepository.findBySubscription(subscription)
            .map(payment -> "SUCCESS".equals(payment.getStatus()))
            .orElse(false);
    }

    public boolean isGatewayConfigured() {
        return razorpayGateway.isConfigured();
    }

    public String getGatewayKeyId() {
        return razorpayGateway.getKeyId();
    }

    public double getTotalRevenue() {
        double total = 0;
        for (Payment payment : paymentRepository.findAll()) {
            if ("SUCCESS".equals(payment.getStatus())) {
                total += payment.getAmount();
            }
        }
        return total;
    }
}
