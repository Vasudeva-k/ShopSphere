package com.vasudev.shopsphere.service;

import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.InvoiceRepository;
import com.vasudev.shopsphere.repository.OrderItemRepository;
import com.vasudev.shopsphere.repository.OrderRepository;
import com.vasudev.shopsphere.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            EmailService emailService,
            InvoicePdfService invoicePdfService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.invoicePdfService = invoicePdfService;
    }

    @Transactional
    @CacheEvict(value = "invoices-by-order", key = "#userId + ':' + #orderId")
    public Invoice createInvoice(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (invoiceRepository.findByOrder(order).isPresent()) {
            throw new IllegalStateException("Invoice already exists for this order");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        BigDecimal subtotal = orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String invoiceNumber = generateInvoiceNumber(order.getId());

        Invoice invoice = new Invoice(order, invoiceNumber, subtotal, order.getTotalAmount());
        return invoiceRepository.save(invoice);
    }

    @Cacheable(value = "invoices-by-order", key = "#userId + ':' + #orderId")
    public Invoice getInvoice(Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return invoiceRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order id: " + orderId));
    }

    @Cacheable(value = "invoices-by-id", key = "#userId + ':' + #invoiceId")
    public Invoice getInvoiceById(Long userId, Long invoiceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (invoice.getOrder() == null || invoice.getOrder().getUser() == null || !invoice.getOrder().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Invoice not found with id: " + invoiceId);
        }

        return invoice;
    }

    public void sendInvoiceEmail(Long userId, Long orderId, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        Invoice invoice = invoiceRepository.findByOrder(order)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order id: " + orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

        byte[] pdfData = invoicePdfService.generateInvoicePdf(invoice, orderItems);

        String subject = "Invoice " + invoice.getInvoiceNumber();
        String body = "Invoice: " + invoice.getInvoiceNumber() + "\n"
                + "Order ID: " + order.getId() + "\n"
                + "Issued At: " + invoice.getIssuedAt() + "\n"
                + "Subtotal: " + invoice.getSubtotal() + "\n"
                + "Total Amount: " + invoice.getTotalAmount();

        emailService.sendEmailWithAttachment(
                email,
                subject,
                body,
                pdfData,
                "invoice-" + invoice.getInvoiceNumber() + ".pdf"
        );
    }

    private String generateInvoiceNumber(Long orderId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "INV-" + orderId + "-" + timestamp + "-" + suffix;
    }
}
