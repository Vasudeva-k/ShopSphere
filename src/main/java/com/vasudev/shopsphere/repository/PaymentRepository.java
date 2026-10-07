package com.vasudev.shopsphere.repository;

import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.Payment;
import com.vasudev.shopsphere.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndOrderUser(Long id, User user);

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByTransactionId(String transactionId);
}
