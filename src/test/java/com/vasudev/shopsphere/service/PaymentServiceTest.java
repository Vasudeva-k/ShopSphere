package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Payment;
import com.vasudev.shopsphere.entity.PaymentStatus;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.OrderRepository;
import com.vasudev.shopsphere.repository.PaymentRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentService paymentService;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, userRepository);

        testUser = new User("John", "Doe", "john@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testOrder = new Order(testUser, new BigDecimal("1299.99"), OrderStatus.PENDING);
        testOrder.setId(10L);
    }

    // 1. Successful payment creation
    @Test
    void createPayment_successful() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        Payment payment = paymentService.createPayment(1L, 10L);

        assertNotNull(payment);
        assertEquals(100L, payment.getId());
        assertEquals(testOrder, payment.getOrder());
        assertEquals(new BigDecimal("1299.99"), payment.getAmount());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    // 2. User not found -> ResourceNotFoundException
    @Test
    void createPayment_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.createPayment(999L, 10L));
        verify(paymentRepository, never()).save(any());
    }

    // 3. Order not found -> ResourceNotFoundException
    @Test
    void createPayment_whenOrderNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.createPayment(1L, 999L));
        verify(paymentRepository, never()).save(any());
    }

    // 4. Existing payment prevents duplicate payment -> IllegalArgumentException
    @Test
    void createPayment_whenPaymentAlreadyExists_throwsIllegalArgumentException() {
        Payment existingPayment = new Payment(testOrder, new BigDecimal("1299.99"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.of(existingPayment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(1L, 10L));
        verify(paymentRepository, never()).save(any());
    }

    // 5. Payment amount comes from Order.totalAmount
    @Test
    void createPayment_paymentAmountMatchesOrderTotalAmount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createPayment(1L, 10L);

        assertEquals(testOrder.getTotalAmount(), payment.getAmount());
    }

    // 6. New payment starts with PENDING status
    @Test
    void createPayment_startsInPendingStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createPayment(1L, 10L);

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    // 7. Order status is not changed during payment creation
    @Test
    void createPayment_doesNotChangeOrderStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(1L, 10L);

        assertEquals(OrderStatus.PENDING, testOrder.getStatus());
        verify(orderRepository, never()).save(any());
    }

    // 8. Successful simulated payment
    @Test
    void processPayment_successfulSimulatedPayment() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L, 100L, true);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(paymentRepository).save(payment);
    }

    // 9. Successful payment generates transaction ID
    @Test
    void processPayment_successfulGeneratesTransactionId() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L, 100L, true);

        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN-"));
    }

    // 10. Successful payment sets Order status to CONFIRMED
    @Test
    void processPayment_successfulSetsOrderStatusToConfirmed() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(1L, 100L, true);

        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    // 11. Failed simulated payment
    @Test
    void processPayment_failedSimulatedPayment() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L, 100L, false);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    // 12. Failed payment does not generate transaction ID
    @Test
    void processPayment_failedDoesNotGenerateTransactionId() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(1L, 100L, false);

        assertNull(result.getTransactionId());
    }

    // 13. Failed payment does not change Order status
    @Test
    void processPayment_failedDoesNotChangeOrderStatus() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(1L, 100L, false);

        assertEquals(OrderStatus.PENDING, testOrder.getStatus());
        verify(orderRepository, never()).save(any());
    }

    // 14. Already SUCCESS payment is rejected
    @Test
    void processPayment_whenAlreadySuccess_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.SUCCESS);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment(1L, 100L, true));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 15. Already FAILED payment is rejected
    @Test
    void processPayment_whenAlreadyFailed_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.FAILED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment(1L, 100L, true));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 16. REFUNDED payment is rejected
    @Test
    void processPayment_whenRefunded_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.REFUNDED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment(1L, 100L, true));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 17. Missing user/payment produces ResourceNotFoundException
    @Test
    void processPayment_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.processPayment(999L, 100L, true));
    }

    @Test
    void processPayment_whenPaymentNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.processPayment(1L, 999L, true));
    }

    // 18. Successful refund changes Payment to REFUNDED and Order to CANCELLED
    @Test
    void refundPayment_successful() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("TXN-12345");
        testOrder.setStatus(OrderStatus.CONFIRMED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.refundPayment(1L, 100L);

        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        assertEquals("TXN-12345", result.getTransactionId());
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(testOrder);
    }

    // 19. Refund of PENDING payment is rejected
    @Test
    void refundPayment_whenPending_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.refundPayment(1L, 100L));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 20. Refund of FAILED payment is rejected
    @Test
    void refundPayment_whenFailed_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.FAILED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.refundPayment(1L, 100L));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 21. Refund of already REFUNDED payment is rejected
    @Test
    void refundPayment_whenAlreadyRefunded_throwsIllegalArgumentException() {
        Payment payment = new Payment(testOrder, new BigDecimal("1299.99"));
        payment.setId(100L);
        payment.setStatus(PaymentStatus.REFUNDED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByIdAndOrderUser(100L, testUser)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.refundPayment(1L, 100L));
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {PaymentService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("orders");
            }

            @Bean
            public PaymentRepository paymentRepository() {
                return Mockito.mock(PaymentRepository.class);
            }

            @Bean
            public OrderRepository orderRepository() {
                return Mockito.mock(OrderRepository.class);
            }

            @Bean
            public UserRepository userRepository() {
                return Mockito.mock(UserRepository.class);
            }
        }

        @Autowired
        private PaymentService paymentService;

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user;
        private Order order;
        private Payment payment;

        @BeforeEach
        void setUp() {
            Mockito.reset(paymentRepository, orderRepository, userRepository);
            Cache cache = cacheManager.getCache("orders");
            if (cache != null) {
                cache.clear();
            }

            user = new User("John", "Doe", "john@example.com", "Password123", "1234567890");
            user.setId(1L);

            order = new Order(user, new BigDecimal("1299.99"), OrderStatus.PENDING);
            order.setId(10L);

            payment = new Payment(order, new BigDecimal("1299.99"));
            payment.setId(100L);
        }

        @Test
        void testF_successfulPayment_evictsOrderCache() {
            payment.setStatus(PaymentStatus.PENDING);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(paymentRepository.findByIdAndOrderUser(100L, user)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Cache cache = cacheManager.getCache("orders");
            assertNotNull(cache);
            cache.put("1:10", "cached-order-response");
            assertNotNull(cache.get("1:10"));

            paymentService.processPayment(1L, 100L, true);

            assertNull(cache.get("1:10"));
        }

        @Test
        void testG_failedPayment_doesNotEvictOrderCache() {
            payment.setStatus(PaymentStatus.PENDING);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(paymentRepository.findByIdAndOrderUser(100L, user)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Cache cache = cacheManager.getCache("orders");
            assertNotNull(cache);
            cache.put("1:10", "cached-order-response");
            assertNotNull(cache.get("1:10"));

            paymentService.processPayment(1L, 100L, false);

            assertNotNull(cache.get("1:10"));
            assertEquals("cached-order-response", cache.get("1:10").get());
        }

        @Test
        void testH_refundPayment_evictsOrderCache() {
            payment.setStatus(PaymentStatus.SUCCESS);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(paymentRepository.findByIdAndOrderUser(100L, user)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Cache cache = cacheManager.getCache("orders");
            assertNotNull(cache);
            cache.put("1:10", "cached-order-response");
            assertNotNull(cache.get("1:10"));

            paymentService.refundPayment(1L, 100L);

            assertNull(cache.get("1:10"));
        }
    }
}
