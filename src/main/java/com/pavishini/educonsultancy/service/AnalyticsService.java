package com.pavishini.educonsultancy.service;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pavishini.educonsultancy.repository.PaymentRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

@Service
public class AnalyticsService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AnalyticsService(PaymentRepository paymentRepository, SubscriptionRepository subscriptionRepository) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<MonthlyRevenue> getRevenueByMonth() {
        return paymentRepository.sumSuccessfulRevenueByMonth().stream()
                .map(row -> new MonthlyRevenue(
                        YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).doubleValue()))
                .toList();
    }

    public List<TopCourse> getTopCoursesBySubscriptionCount() {
        return subscriptionRepository.findCourseSubscriptionCounts().stream()
                .map(row -> new TopCourse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public List<PaymentStatusCount> getPaymentCountsByStatus() {
        return paymentRepository.countPaymentsByStatus().stream()
                .map(row -> new PaymentStatusCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    public record MonthlyRevenue(YearMonth month, double total) {
    }

    public record TopCourse(String courseName, long subscriptionCount) {
    }

    public record PaymentStatusCount(String status, long count) {
    }
}