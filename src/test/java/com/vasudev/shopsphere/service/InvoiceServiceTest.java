package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.InvoiceRepository;
import com.vasudev.shopsphere.repository.OrderItemRepository;
import com.vasudev.shopsphere.repository.OrderRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private InvoicePdfService invoicePdfService;

    @InjectMocks
    private InvoiceService invoiceService;

    private User testUser;
    private User otherUser;
    private Order testOrder;
    private Product testProduct1;
    private Product testProduct2;
    private OrderItem orderItem1;
    private OrderItem orderItem2;

    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        otherUser = new User("Jane", "Smith", "jane@example.com", "Password123", "0987654321");
        otherUser.setId(2L);

        testOrder = new Order(testUser, new BigDecimal("350.00"), OrderStatus.PENDING);
        testOrder.setId(10L);

        testProduct1 = new Product("Product 1", "Description 1", new BigDecimal("100.00"), 50, null);
        testProduct1.setId(101L);

        testProduct2 = new Product("Product 2", "Description 2", new BigDecimal("50.00"), 50, null);
        testProduct2.setId(102L);

        orderItem1 = new OrderItem(testOrder, testProduct1, "Product 1", new BigDecimal("100.00"), 2, new BigDecimal("200.00"));
        orderItem1.setId(201L);

        orderItem2 = new OrderItem(testOrder, testProduct2, "Product 2", new BigDecimal("50.00"), 3, new BigDecimal("150.00"));
        orderItem2.setId(202L);
    }

    // 1. Successful invoice creation
    @Test
    @DisplayName("createInvoice successfully generates, saves, and returns an invoice")
    void createInvoice_successful() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(500L);
            return inv;
        });

        Invoice created = invoiceService.createInvoice(1L, 10L);

        assertNotNull(created);
        assertEquals(500L, created.getId());
        assertEquals(testOrder, created.getOrder());
        assertEquals(new BigDecimal("350.00"), created.getSubtotal());
        assertEquals(new BigDecimal("350.00"), created.getTotalAmount());
        assertNotNull(created.getInvoiceNumber());
        assertTrue(created.getInvoiceNumber().startsWith("INV-"));
        assertTrue(created.getInvoiceNumber().contains("10"));

        verify(invoiceRepository).findByOrder(testOrder);
        verify(orderRepository).findByIdAndUser(10L, testUser);
        verify(orderItemRepository).findByOrder(testOrder);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // 2. User not found -> ResourceNotFoundException
    @Test
    @DisplayName("createInvoice throws ResourceNotFoundException when user does not exist")
    void createInvoice_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(999L, 10L));

        verify(orderRepository, never()).findByIdAndUser(any(), any());
        verify(invoiceRepository, never()).findByOrder(any());
        verify(invoiceRepository, never()).save(any());
    }

    // 3. Order not found / wrong owner -> ResourceNotFoundException
    @Test
    @DisplayName("createInvoice throws ResourceNotFoundException when order does not belong to user")
    void createInvoice_whenOrderNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(1L, 999L));

        verify(invoiceRepository, never()).findByOrder(any());
        verify(orderItemRepository, never()).findByOrder(any());
        verify(invoiceRepository, never()).save(any());
    }

    // 4. Duplicate invoice -> IllegalStateException("Invoice already exists for this order")
    @Test
    @DisplayName("createInvoice rejects duplicate invoice creation with IllegalStateException")
    void createInvoice_whenInvoiceAlreadyExists_throwsIllegalStateException() {
        Invoice existingInvoice = new Invoice(testOrder, "INV-10-20261004-EXIST1", new BigDecimal("350.00"), new BigDecimal("350.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(existingInvoice));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> invoiceService.createInvoice(1L, 10L));

        assertEquals("Invoice already exists for this order", exception.getMessage());
        verify(orderItemRepository, never()).findByOrder(any());
        verify(invoiceRepository, never()).save(any());
    }

    // 5. Subtotal calculation with multiple items
    @Test
    @DisplayName("createInvoice accurately computes subtotal using BigDecimal arithmetic")
    void createInvoice_subtotalCalculation() {
        OrderItem itemA = new OrderItem(testOrder, testProduct1, "Product 1", new BigDecimal("19.99"), 3, new BigDecimal("59.97"));
        OrderItem itemB = new OrderItem(testOrder, testProduct2, "Product 2", new BigDecimal("49.50"), 2, new BigDecimal("99.00"));
        OrderItem itemC = new OrderItem(testOrder, testProduct1, "Product 1", new BigDecimal("10.00"), 1, new BigDecimal("10.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(itemA, itemB, itemC));

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.createInvoice(1L, 10L);

        Invoice saved = invoiceCaptor.getValue();
        assertEquals(new BigDecimal("168.97"), saved.getSubtotal());
    }

    // 6. Invoice totalAmount is derived authoritatively from Order.totalAmount
    @Test
    @DisplayName("createInvoice takes totalAmount from Order.totalAmount")
    void createInvoice_usesOrderTotalAmount() {
        testOrder.setTotalAmount(new BigDecimal("499.99"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.createInvoice(1L, 10L);

        Invoice saved = invoiceCaptor.getValue();
        assertEquals(new BigDecimal("499.99"), saved.getTotalAmount());
    }

    // 7. Invoice number generation format and uniqueness
    @Test
    @DisplayName("createInvoice generates unique invoice numbers containing prefix and order ID")
    void createInvoice_invoiceNumberGeneration() {
        Order order2 = new Order(testUser, new BigDecimal("500.00"), OrderStatus.PENDING);
        order2.setId(20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findByIdAndUser(20L, testUser)).thenReturn(Optional.of(order2));
        when(invoiceRepository.findByOrder(any(Order.class))).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(any(Order.class))).thenReturn(List.of(orderItem1));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.createInvoice(1L, 10L);
        Invoice firstInvoice = captor.getValue();

        invoiceService.createInvoice(1L, 20L);
        Invoice secondInvoice = captor.getValue();

        assertNotNull(firstInvoice.getInvoiceNumber());
        assertFalse(firstInvoice.getInvoiceNumber().isBlank());
        assertTrue(firstInvoice.getInvoiceNumber().startsWith("INV-"));
        assertTrue(firstInvoice.getInvoiceNumber().contains("10"));

        assertNotNull(secondInvoice.getInvoiceNumber());
        assertFalse(secondInvoice.getInvoiceNumber().isBlank());
        assertTrue(secondInvoice.getInvoiceNumber().startsWith("INV-"));
        assertTrue(secondInvoice.getInvoiceNumber().contains("20"));

        assertNotEquals(firstInvoice.getInvoiceNumber(), secondInvoice.getInvoiceNumber());
    }

    // 8. createInvoice does not mutate unrelated objects
    @Test
    @DisplayName("createInvoice does not modify Order status or perform unrelated updates")
    void createInvoice_doesNotMutateUnrelatedObjects() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.createInvoice(1L, 10L);

        assertEquals(OrderStatus.PENDING, testOrder.getStatus());
        verify(orderRepository, never()).save(any());
    }

    // 9. Successful getInvoice by order
    @Test
    @DisplayName("getInvoice retrieves invoice for order scoped to authenticated user")
    void getInvoice_successful() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST01", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(501L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoice(1L, 10L);

        assertNotNull(result);
        assertEquals(501L, result.getId());
        assertEquals(testOrder, result.getOrder());
        assertEquals("INV-10-20261004-TEST01", result.getInvoiceNumber());
    }

    // 10. getInvoice user not found
    @Test
    @DisplayName("getInvoice throws ResourceNotFoundException when user is not found")
    void getInvoice_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(999L, 10L));
        verify(orderRepository, never()).findByIdAndUser(any(), any());
        verify(invoiceRepository, never()).findByOrder(any());
    }

    // 11. getInvoice order not found / wrong owner
    @Test
    @DisplayName("getInvoice throws ResourceNotFoundException when order is not found for user")
    void getInvoice_whenOrderNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(1L, 999L));
        verify(invoiceRepository, never()).findByOrder(any());
    }

    // 12. getInvoice invoice not found
    @Test
    @DisplayName("getInvoice throws ResourceNotFoundException when no invoice exists for order")
    void getInvoice_whenInvoiceNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(1L, 10L));
    }

    // 13. getInvoiceById successful
    @Test
    @DisplayName("getInvoiceById retrieves invoice when owned by authenticated user")
    void getInvoiceById_successful() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST02", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(502L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(invoiceRepository.findById(502L)).thenReturn(Optional.of(invoice));

        Invoice result = invoiceService.getInvoiceById(1L, 502L);

        assertNotNull(result);
        assertEquals(502L, result.getId());
        assertEquals("INV-10-20261004-TEST02", result.getInvoiceNumber());
        assertEquals(1L, result.getOrder().getUser().getId());
    }

    // 14. getInvoiceById invoice not found
    @Test
    @DisplayName("getInvoiceById throws ResourceNotFoundException when invoice is not found")
    void getInvoiceById_whenInvoiceNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(1L, 999L));
    }

    // 15. getInvoiceById wrong owner (isolation test)
    @Test
    @DisplayName("getInvoiceById throws ResourceNotFoundException when invoice belongs to another user")
    void getInvoiceById_whenWrongOwner_throwsResourceNotFoundException() {
        Order otherOrder = new Order(otherUser, new BigDecimal("700.00"), OrderStatus.PENDING);
        otherOrder.setId(20L);

        Invoice otherInvoice = new Invoice(otherOrder, "INV-20-20261004-OTHER1", new BigDecimal("700.00"), new BigDecimal("700.00"));
        otherInvoice.setId(503L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(invoiceRepository.findById(503L)).thenReturn(Optional.of(otherInvoice));

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(1L, 503L));
    }

    // 16. getInvoiceById ownership comparison logic
    @Test
    @DisplayName("getInvoiceById strictly verifies invoice->order->user relationship")
    void getInvoiceById_verifiesOwnershipThroughOrderUser() {
        Order unownedOrder = new Order();
        unownedOrder.setId(30L);
        unownedOrder.setUser(null);

        Invoice detachedInvoice = new Invoice(unownedOrder, "INV-30-20261004-DETACH", new BigDecimal("100.00"), new BigDecimal("100.00"));
        detachedInvoice.setId(504L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(invoiceRepository.findById(504L)).thenReturn(Optional.of(detachedInvoice));

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(1L, 504L));
    }

    // 17. Repository interaction safety on failures
    @Test
    @DisplayName("Repository safety: No save interactions occur on any validation or lookup failure")
    void repositoryInteractionSafety_onFailures() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(999L, 100L));
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(999L, 100L));
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(999L, 100L));

        verify(invoiceRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // 18. sendInvoiceEmail: Successful email sending with PDF attachment
    @Test
    @DisplayName("sendInvoiceEmail generates PDF and sends invoice email with attachment")
    void sendInvoiceEmail_successful() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST01", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(501L);
        invoice.setIssuedAt(LocalDateTime.of(2026, 10, 4, 10, 0));

        byte[] fakePdfBytes = "%PDF-1.4 sample pdf content".getBytes(StandardCharsets.UTF_8);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(invoice));
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));
        when(invoicePdfService.generateInvoicePdf(invoice, List.of(orderItem1, orderItem2))).thenReturn(fakePdfBytes);

        invoiceService.sendInvoiceEmail(1L, 10L, "customer@example.com");

        verify(orderItemRepository).findByOrder(testOrder);
        verify(invoicePdfService).generateInvoicePdf(invoice, List.of(orderItem1, orderItem2));

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> attachmentDataCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<String> attachmentFileNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmailWithAttachment(
                toCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                attachmentDataCaptor.capture(),
                attachmentFileNameCaptor.capture()
        );

        assertEquals("customer@example.com", toCaptor.getValue());
        assertEquals("Invoice INV-10-20261004-TEST01", subjectCaptor.getValue());
        assertTrue(bodyCaptor.getValue().contains("INV-10-20261004-TEST01"));
        assertTrue(bodyCaptor.getValue().contains("10"));
        assertTrue(bodyCaptor.getValue().contains("2026-10-04T10:00"));
        assertTrue(bodyCaptor.getValue().contains("350.00"));
        assertArrayEquals(fakePdfBytes, attachmentDataCaptor.getValue());
        assertEquals("invoice-INV-10-20261004-TEST01.pdf", attachmentFileNameCaptor.getValue());
    }

    // 19. sendInvoiceEmail: User not found
    @Test
    @DisplayName("sendInvoiceEmail throws ResourceNotFoundException when user does not exist")
    void sendInvoiceEmail_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.sendInvoiceEmail(999L, 10L, "customer@example.com"));

        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 20. sendInvoiceEmail: Order not found / wrong owner
    @Test
    @DisplayName("sendInvoiceEmail throws ResourceNotFoundException when order does not belong to user")
    void sendInvoiceEmail_whenOrderNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 999L, "customer@example.com"));

        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 21. sendInvoiceEmail: Invoice not found
    @Test
    @DisplayName("sendInvoiceEmail throws ResourceNotFoundException when invoice is not found for order")
    void sendInvoiceEmail_whenInvoiceNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 10L, "customer@example.com"));

        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 22. sendInvoiceEmail: Null email
    @Test
    @DisplayName("sendInvoiceEmail throws IllegalArgumentException when email is null")
    void sendInvoiceEmail_whenEmailIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 10L, null));

        verify(userRepository, never()).findById(any());
        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 23. sendInvoiceEmail: Blank email
    @Test
    @DisplayName("sendInvoiceEmail throws IllegalArgumentException when email is blank")
    void sendInvoiceEmail_whenEmailIsBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 10L, "   "));

        verify(userRepository, never()).findById(any());
        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 24. sendInvoiceEmail: PDF generation failure propagation
    @Test
    @DisplayName("sendInvoiceEmail propagates PDF generation runtime exceptions and does not send email")
    void sendInvoiceEmail_whenPdfGenerationFails_propagatesExceptionAndDoesNotSendEmail() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST01", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(501L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(invoice));
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));
        when(invoicePdfService.generateInvoicePdf(invoice, List.of(orderItem1, orderItem2)))
                .thenThrow(new RuntimeException("PDF generation failed due to low memory"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 10L, "customer@example.com"));

        assertEquals("PDF generation failed due to low memory", ex.getMessage());
        verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any(), any());
    }

    // 25. sendInvoiceEmail: EmailService failure propagation
    @Test
    @DisplayName("sendInvoiceEmail propagates EmailService runtime exceptions")
    void sendInvoiceEmail_whenEmailServiceFails_propagatesException() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST01", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(501L);

        byte[] fakePdfBytes = "%PDF-1.4 sample pdf".getBytes(StandardCharsets.UTF_8);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(invoice));
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));
        when(invoicePdfService.generateInvoicePdf(invoice, List.of(orderItem1, orderItem2))).thenReturn(fakePdfBytes);

        doThrow(new RuntimeException("SMTP connection timed out"))
                .when(emailService).sendEmailWithAttachment(any(), any(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invoiceService.sendInvoiceEmail(1L, 10L, "customer@example.com"));

        assertEquals("SMTP connection timed out", ex.getMessage());
    }

    // 26. sendInvoiceEmail: No mutation on any repository
    @Test
    @DisplayName("sendInvoiceEmail does not mutate any repository entities")
    void sendInvoiceEmail_doesNotMutateRepositories() {
        Invoice invoice = new Invoice(testOrder, "INV-10-20261004-TEST01", new BigDecimal("350.00"), new BigDecimal("350.00"));
        invoice.setId(501L);

        byte[] fakePdfBytes = "%PDF-1.4 sample pdf".getBytes(StandardCharsets.UTF_8);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(testOrder));
        when(invoiceRepository.findByOrder(testOrder)).thenReturn(Optional.of(invoice));
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(List.of(orderItem1, orderItem2));
        when(invoicePdfService.generateInvoicePdf(invoice, List.of(orderItem1, orderItem2))).thenReturn(fakePdfBytes);

        invoiceService.sendInvoiceEmail(1L, 10L, "customer@example.com");

        verify(invoiceRepository, never()).save(any());
        verify(invoiceRepository, never()).delete(any());
        verify(orderRepository, never()).save(any());
        verify(orderRepository, never()).delete(any());
        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository, never()).delete(any());
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
    }

    @Nested
    @SpringJUnitConfig(classes = {InvoiceService.class, CacheLifecycleTest.CachingTestConfig.class})
    class CacheLifecycleTest {

        @Configuration
        @EnableCaching
        static class CachingTestConfig {
            @Bean
            public CacheManager cacheManager() {
                return new ConcurrentMapCacheManager("invoices-by-order", "invoices-by-id");
            }

            @Bean
            public InvoiceRepository invoiceRepository() {
                return Mockito.mock(InvoiceRepository.class);
            }

            @Bean
            public OrderRepository orderRepository() {
                return Mockito.mock(OrderRepository.class);
            }

            @Bean
            public OrderItemRepository orderItemRepository() {
                return Mockito.mock(OrderItemRepository.class);
            }

            @Bean
            public UserRepository userRepository() {
                return Mockito.mock(UserRepository.class);
            }

            @Bean
            public EmailService emailService() {
                return Mockito.mock(EmailService.class);
            }

            @Bean
            public InvoicePdfService invoicePdfService() {
                return Mockito.mock(InvoicePdfService.class);
            }
        }

        @Autowired
        private InvoiceService invoiceService;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CacheManager cacheManager;

        private User user1;
        private User user2;
        private Order order1;
        private Order order2;
        private Invoice invoice1;
        private Invoice invoice2;

        @BeforeEach
        void setUp() {
            Mockito.reset(invoiceRepository, orderRepository, userRepository);
            Cache orderCache = cacheManager.getCache("invoices-by-order");
            if (orderCache != null) {
                orderCache.clear();
            }
            Cache idCache = cacheManager.getCache("invoices-by-id");
            if (idCache != null) {
                idCache.clear();
            }

            user1 = new User("John", "Doe", "john@example.com", "Password123", "1234567890");
            user1.setId(1L);

            user2 = new User("Jane", "Smith", "jane@example.com", "Password123", "0987654321");
            user2.setId(2L);

            order1 = new Order(user1, new BigDecimal("350.00"), OrderStatus.PENDING);
            order1.setId(10L);

            order2 = new Order(user2, new BigDecimal("450.00"), OrderStatus.PENDING);
            order2.setId(10L);

            invoice1 = new Invoice(order1, "INV-10-001", new BigDecimal("350.00"), new BigDecimal("350.00"));
            invoice1.setId(501L);

            invoice2 = new Invoice(order2, "INV-10-002", new BigDecimal("450.00"), new BigDecimal("450.00"));
            invoice2.setId(501L);
        }

        @Test
        void testA_and_testB_getInvoiceByOrder_cacheMissAndHit() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));
            when(invoiceRepository.findByOrder(order1)).thenReturn(Optional.of(invoice1));

            // Test A — First lookup (Cache Miss)
            Invoice first = invoiceService.getInvoice(1L, 10L);
            assertNotNull(first);
            assertEquals("INV-10-001", first.getInvoiceNumber());
            verify(invoiceRepository, times(1)).findByOrder(order1);

            // Test B — Second lookup (Cache Hit)
            Invoice second = invoiceService.getInvoice(1L, 10L);
            assertNotNull(second);
            assertEquals("INV-10-001", second.getInvoiceNumber());
            verify(invoiceRepository, times(1)).findByOrder(order1);
        }

        @Test
        void testC_and_testD_getInvoiceById_cacheMissAndHit() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(invoiceRepository.findById(501L)).thenReturn(Optional.of(invoice1));

            // Test C — First ID lookup (Cache Miss)
            Invoice first = invoiceService.getInvoiceById(1L, 501L);
            assertNotNull(first);
            assertEquals(501L, first.getId());
            verify(invoiceRepository, times(1)).findById(501L);

            // Test D — Second ID lookup (Cache Hit)
            Invoice second = invoiceService.getInvoiceById(1L, 501L);
            assertNotNull(second);
            assertEquals(501L, second.getId());
            verify(invoiceRepository, times(1)).findById(501L);
        }

        @Test
        void testE_userIsolation_orderAndIdLookups() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));
            when(orderRepository.findByIdAndUser(10L, user2)).thenReturn(Optional.of(order2));
            when(invoiceRepository.findByOrder(order1)).thenReturn(Optional.of(invoice1));
            when(invoiceRepository.findByOrder(order2)).thenReturn(Optional.of(invoice2));
            when(invoiceRepository.findById(501L)).thenReturn(Optional.of(invoice1));

            // user1/order10 and user2/order10
            Invoice inv1 = invoiceService.getInvoice(1L, 10L);
            Invoice inv2 = invoiceService.getInvoice(2L, 10L);
            assertEquals("INV-10-001", inv1.getInvoiceNumber());
            assertEquals("INV-10-002", inv2.getInvoiceNumber());

            Cache orderCache = cacheManager.getCache("invoices-by-order");
            assertNotNull(orderCache);
            assertNotNull(orderCache.get("1:10"));
            assertNotNull(orderCache.get("2:10"));

            // user1/id501
            Invoice byId1 = invoiceService.getInvoiceById(1L, 501L);
            assertEquals(501L, byId1.getId());
            Cache idCache = cacheManager.getCache("invoices-by-id");
            assertNotNull(idCache);
            assertNotNull(idCache.get("1:501"));
        }

        @Test
        void testF_creationInvalidation_evictsInvoiceByOrderCache() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(orderRepository.findByIdAndUser(10L, user1)).thenReturn(Optional.of(order1));
            when(invoiceRepository.findByOrder(order1)).thenReturn(Optional.empty()).thenReturn(Optional.of(invoice1));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            Cache orderCache = cacheManager.getCache("invoices-by-order");
            assertNotNull(orderCache);
            orderCache.put("1:10", "stale-invoice");
            assertNotNull(orderCache.get("1:10"));

            // Create invoice
            invoiceService.createInvoice(1L, 10L);

            // Cache entry should be evicted
            assertNull(orderCache.get("1:10"));
        }

        @Test
        void testG_missingInvoice_notCached() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(orderRepository.findByIdAndUser(99L, user1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoice(1L, 99L));

            Cache orderCache = cacheManager.getCache("invoices-by-order");
            assertNotNull(orderCache);
            assertNull(orderCache.get("1:99"));
        }
    }
}
