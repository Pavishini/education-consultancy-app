package com.pavishini.educonsultancy.service;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pavishini.educonsultancy.repository.PaymentRepository;
import com.pavishini.educonsultancy.repository.SubscriptionRepository;

class AnalyticsServiceTest {

    @Test
    void getRevenueByMonth_shouldReturnEmptyListWhenNoSuccessfulPaymentsExist() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(paymentRepository.sumSuccessfulRevenueByMonth()).thenReturn(List.of());
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.MonthlyRevenue> revenue = analyticsService.getRevenueByMonth();

        assertNotNull(revenue);
        assertTrue(revenue.isEmpty());
        verify(paymentRepository).sumSuccessfulRevenueByMonth();
    }

    @Test
    void getRevenueByMonth_shouldMapRepositoryAggregatesToMonthlyRevenue() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(paymentRepository.sumSuccessfulRevenueByMonth()).thenReturn(List.of(
                new Object[] { 2026, 1, 1200.50 },
                new Object[] { 2026, 2, 800.0 }
        ));
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.MonthlyRevenue> revenue = analyticsService.getRevenueByMonth();

        assertEquals(List.of(
                new AnalyticsService.MonthlyRevenue(YearMonth.of(2026, 1), 1200.50),
                new AnalyticsService.MonthlyRevenue(YearMonth.of(2026, 2), 800.0)
        ), revenue);
        verify(paymentRepository).sumSuccessfulRevenueByMonth();
    }

    @Test
    void getTopCoursesBySubscriptionCount_shouldMapCoursesInRepositoryOrder() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(subscriptionRepository.findCourseSubscriptionCounts()).thenReturn(List.of(
                new Object[] { "Study Abroad Counselling", 8L },
                new Object[] { "IELTS Preparation", 5L }
        ));
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.TopCourse> topCourses = analyticsService.getTopCoursesBySubscriptionCount();

        assertEquals(List.of(
                new AnalyticsService.TopCourse("Study Abroad Counselling", 8L),
                new AnalyticsService.TopCourse("IELTS Preparation", 5L)
        ), topCourses);
        verify(subscriptionRepository).findCourseSubscriptionCounts();
    }

    @Test
    void getTopCoursesBySubscriptionCount_shouldReturnEmptyListWhenNoSubscriptionsExist() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(subscriptionRepository.findCourseSubscriptionCounts()).thenReturn(List.of());
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.TopCourse> topCourses = analyticsService.getTopCoursesBySubscriptionCount();

        assertNotNull(topCourses);
        assertTrue(topCourses.isEmpty());
        verify(subscriptionRepository).findCourseSubscriptionCounts();
    }

    @Test
    void getPaymentCountsByStatus_shouldMapCountsForEachPaymentStatus() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(paymentRepository.countPaymentsByStatus()).thenReturn(List.of(
                new Object[] { "FAILED", 2L },
                new Object[] { "PENDING", 3L },
                new Object[] { "SUCCESS", 7L }
        ));
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.PaymentStatusCount> counts = analyticsService.getPaymentCountsByStatus();

        assertEquals(List.of(
                new AnalyticsService.PaymentStatusCount("FAILED", 2L),
                new AnalyticsService.PaymentStatusCount("PENDING", 3L),
                new AnalyticsService.PaymentStatusCount("SUCCESS", 7L)
        ), counts);
        verify(paymentRepository).countPaymentsByStatus();
    }

    @Test
    void getPaymentCountsByStatus_shouldReturnEmptyListWhenNoPaymentsExist() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(paymentRepository.countPaymentsByStatus()).thenReturn(List.of());
        AnalyticsService analyticsService = new AnalyticsService(paymentRepository, subscriptionRepository);

        List<AnalyticsService.PaymentStatusCount> counts = analyticsService.getPaymentCountsByStatus();

        assertNotNull(counts);
        assertTrue(counts.isEmpty());
        verify(paymentRepository).countPaymentsByStatus();
    }
}