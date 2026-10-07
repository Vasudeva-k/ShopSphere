package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lowagie.text.pdf.PdfReader;
import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.entity.User;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvoicePdfServiceTest {

    private InvoicePdfService invoicePdfService;
    private User testUser;
    private Order testOrder;
    private Invoice testInvoice;
    private List<OrderItem> testOrderItems;

    @BeforeEach
    void setUp() {
        invoicePdfService = new InvoicePdfService();

        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);

        testOrder = new Order(testUser, new BigDecimal("450.00"), OrderStatus.CONFIRMED);
        testOrder.setId(42L);

        testInvoice = new Invoice(testOrder, "INV-42-20261004-TEST99", new BigDecimal("450.00"), new BigDecimal("450.00"));
        testInvoice.setId(101L);
        testInvoice.setIssuedAt(LocalDateTime.of(2026, 10, 4, 10, 30, 0));
        testInvoice.setCreatedAt(LocalDateTime.of(2026, 10, 4, 10, 30, 0));

        Product product1 = new Product("Wireless Headphones", "High quality noise cancelling headphones", new BigDecimal("150.00"), 50, null);
        product1.setId(10L);

        Product product2 = new Product("Smart Watch", "Fitness tracker and smart watch", new BigDecimal("150.00"), 30, null);
        product2.setId(20L);

        OrderItem item1 = new OrderItem(testOrder, product1, "Wireless Headphones", new BigDecimal("150.00"), 2, new BigDecimal("300.00"));
        item1.setId(1L);

        OrderItem item2 = new OrderItem(testOrder, product2, "Smart Watch", new BigDecimal("150.00"), 1, new BigDecimal("150.00"));
        item2.setId(2L);

        testOrderItems = new ArrayList<>();
        testOrderItems.add(item1);
        testOrderItems.add(item2);
    }

    @Test
    @DisplayName("generateInvoicePdf generates valid non-empty PDF bytes for valid invoice and items")
    void generateInvoicePdf_withValidInvoiceAndItems_generatesPdf() throws Exception {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(testInvoice, testOrderItems);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Verify PDF magic header bytes (%PDF-)
        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader);

        // Verify with OpenPDF's PdfReader
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        assertTrue(reader.getNumberOfPages() >= 1);
        reader.close();
    }

    @Test
    @DisplayName("generateInvoicePdf generates valid PDF with empty order items list")
    void generateInvoicePdf_withEmptyOrderItems_generatesPdf() throws Exception {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(testInvoice, Collections.emptyList());

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String pdfHeader = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", pdfHeader);

        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        assertTrue(reader.getNumberOfPages() >= 1);
        reader.close();
    }

    @Test
    @DisplayName("generateInvoicePdf throws IllegalArgumentException when invoice is null")
    void generateInvoicePdf_whenInvoiceIsNull_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invoicePdfService.generateInvoicePdf(null, testOrderItems)
        );
        assertEquals("Invoice cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("generateInvoicePdf throws IllegalArgumentException when orderItems is null")
    void generateInvoicePdf_whenOrderItemsIsNull_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invoicePdfService.generateInvoicePdf(testInvoice, null)
        );
        assertEquals("Order items cannot be null", exception.getMessage());
    }
}
