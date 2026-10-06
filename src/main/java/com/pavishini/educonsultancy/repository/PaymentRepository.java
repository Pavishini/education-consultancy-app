package com.pavishini.educonsultancy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pavishini.educonsultancy.entity.Payment;
import com.pavishini.educonsultancy.entity.Subscription;
import com.pavishini.educonsultancy.entity.User;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
            SELECT EXTRACT(YEAR FROM p.paymentDate), EXTRACT(MONTH FROM p.paymentDate), SUM(p.amount)
            FROM Payment p
            WHERE p.status = 'SUCCESS'
            GROUP BY EXTRACT(YEAR FROM p.paymentDate), EXTRACT(MONTH FROM p.paymentDate)
            ORDER BY EXTRACT(YEAR FROM p.paymentDate), EXTRACT(MONTH FROM p.paymentDate)
            """)
    List<Object[]> sumSuccessfulRevenueByMonth();

        @Query("""
            SELECT p.status, COUNT(p.id)
            FROM Payment p
            GROUP BY p.status
            ORDER BY p.status ASC
            """)
        List<Object[]> countPaymentsByStatus();

    List<Payment> findBySubscription_StudentOrderByPaymentDateDesc(User student);
    List<Payment> findByStatusOrderByPaymentDateDesc(String status);
    Optional<Payment> findBySubscription(Subscription subscription);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
}
