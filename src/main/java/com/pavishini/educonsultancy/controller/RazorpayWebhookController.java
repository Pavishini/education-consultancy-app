package com.pavishini.educonsultancy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.pavishini.educonsultancy.service.PaymentService;

@RestController
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    public RazorpayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> receive(@RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                         @RequestBody String payload) {
        try {
            paymentService.processWebhook(eventId, signature, payload);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}