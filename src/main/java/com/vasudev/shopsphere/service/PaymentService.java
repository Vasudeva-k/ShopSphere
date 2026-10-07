package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Payment;
import com.vasudev.shopsphere.entity.PaymentStatus;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.OrderRepository;
import com.vasudev.shopsphere.repository.PaymentRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Payment createPayment(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new IllegalArgumentException("Payment already exists for order id: " + orderId);
        }

        Payment payment = new Payment(order, order.getTotalAmount());
        return paymentRepository.save(payment);
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#userId + ':' + #result.order.id", condition = "#simulateSuccess && #result != null && #result.order != null")
    public Payment processPayment(Long userId, Long paymentId, boolean simulateSuccess) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Payment payment = paymentRepository.findByIdAndOrderUser(paymentId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Cannot process payment with status: " + payment.getStatus());
        }

        if (simulateSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("TXN-" + UUID.randomUUID());
            Payment savedPayment = paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return savedPayment;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            return paymentRepository.save(payment);
        }
    }

    @Transactional
    @CacheEvict(value = "orders", key = "#userId + ':' + #result.order.id", condition = "#result != null && #result.order != null")
    public Payment refundPayment(Long userId, Long paymentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Payment payment = paymentRepository.findByIdAndOrderUser(paymentId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Cannot refund payment with status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }
}
