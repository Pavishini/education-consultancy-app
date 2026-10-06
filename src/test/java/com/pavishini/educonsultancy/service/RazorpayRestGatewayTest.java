package com.pavishini.educonsultancy.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RazorpayRestGatewayTest {

    @Test
    void verifyWebhookSignature_shouldAcceptCorrectSignatureAndRejectTampering() throws Exception {
        // Prevents forged or altered webhook payloads from changing payment status.
        String body = "{\"event\":\"payment.captured\"}";
        String secret = "webhook-test-secret";
        String validSignature = hmac(body, secret);
        RazorpayRestGateway gateway = new RazorpayRestGateway(
                RestClient.builder(), "rzp_test_key", "key-secret", secret);

        assertTrue(gateway.verifyWebhookSignature(body, validSignature));
        assertFalse(gateway.verifyWebhookSignature(body + " ", validSignature));
    }

    @Test
    void verifyCheckoutSignature_shouldBindSignatureToOrderAndPayment() throws Exception {
        // Prevents a valid signature from being reused with a different order or payment ID.
        String keySecret = "razorpay-test-secret";
        RazorpayRestGateway gateway = new RazorpayRestGateway(
                RestClient.builder(), "rzp_test_key", keySecret, "webhook-secret");
        String signature = hmac("order_123|pay_456", keySecret);

        assertTrue(gateway.verifyCheckoutSignature("order_123", "pay_456", signature));
        assertFalse(gateway.verifyCheckoutSignature("order_other", "pay_456", signature));
    }

    @Test
    void isConfigured_shouldRejectLiveKeyForTestMode() {
        // Prevents this test-only checkout integration from accepting live Razorpay credentials.
        RazorpayRestGateway liveGateway = new RazorpayRestGateway(
                RestClient.builder(), "rzp_live_key", "key-secret", "webhook-secret");

        assertFalse(liveGateway.isConfigured());
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
