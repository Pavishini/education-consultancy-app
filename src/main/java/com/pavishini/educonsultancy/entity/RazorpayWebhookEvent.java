package com.pavishini.educonsultancy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "razorpay_webhook_events")
public class RazorpayWebhookEvent {

    @Id
    @Column(length = 255)
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    protected RazorpayWebhookEvent() {
    }

    public RazorpayWebhookEvent(String eventId, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.receivedAt = receivedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}