package com.pavishini.educonsultancy.service;

public interface RazorpayGateway {

    RazorpayOrder createOrder(long amountPaise, String receipt);

    boolean verifyCheckoutSignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signature);

    String getKeyId();

    boolean isConfigured();

    record RazorpayOrder(String id, long amountPaise, String currency) {
    }
}