package com.pavishini.educonsultancy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.RazorpayWebhookEvent;

public interface RazorpayWebhookEventRepository extends JpaRepository<RazorpayWebhookEvent, String> {
}