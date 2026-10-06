package com.pavishini.educonsultancy.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class RazorpayRestGateway implements RazorpayGateway {

    private static final String API_BASE_URL = "https://api.razorpay.com";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayRestGateway(RestClient.Builder restClientBuilder,
                               @Value("${razorpay.key-id:}") String keyId,
                               @Value("${razorpay.key-secret:}") String keySecret,
                               @Value("${razorpay.webhook-secret:}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.restClient = restClientBuilder
                .baseUrl(API_BASE_URL)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public RazorpayOrder createOrder(long amountPaise, String receipt) {
        if (!isConfigured()) {
            throw new IllegalStateException("Razorpay test credentials are not configured.");
        }

        JsonNode response = restClient.post()
                .uri("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("id").asText().isBlank()) {
            throw new IllegalStateException("Razorpay did not return a valid order.");
        }
        return new RazorpayOrder(response.path("id").asText(), response.path("amount").asLong(),
                response.path("currency").asText());
    }

    @Override
    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        if (keySecret.isBlank() || signature == null) {
            return false;
        }
        return signatureMatches(orderId + "|" + paymentId, signature, keySecret);
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret.isBlank() || signature == null) {
            return false;
        }
        return signatureMatches(payload, signature, webhookSecret);
    }

    private boolean signatureMatches(String payload, String suppliedSignature, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, suppliedSignature.toLowerCase().getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not verify Razorpay signature.", ex);
        }
    }

    @Override
    public String getKeyId() {
        return keyId;
    }

    @Override
    public boolean isConfigured() {
        return keyId.startsWith("rzp_test_") && !keySecret.isBlank() && !webhookSecret.isBlank();
    }
}