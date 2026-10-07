package com.vasudev.shopsphere.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasudev.shopsphere.config.SecurityConfig;
import com.vasudev.shopsphere.dto.InvoiceEmailRequest;
import com.vasudev.shopsphere.dto.InvoiceResponse;
import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.Order;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.OrderStatus;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.OrderItemRepository;
import com.vasudev.shopsphere.security.JwtAuthenticationFilter;
import com.vasudev.shopsphere.security.JwtService;
import com.vasudev.shopsphere.service.InvoicePdfService;
import com.vasudev.shopsphere.service.InvoiceService;
import com.vasudev.shopsphere.service.UserService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = InvoiceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=ShopSphere-test-secret-key-2026-very-long-and-secure",
        "jwt.expiration=3600000"
})
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InvoicePdfService invoicePdfService;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    private User testUser;
    private String validToken;
    private Order testOrder;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = new User("Alice", "Smith", "alice@example.com", "Password123", "1234567890");
        testUser.setId(1L);
        testUser.setEnabled(true);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        validToken = jwtService.generateToken(testUser);

        testOrder = new Order(testUser, new BigDecimal("350.00"), OrderStatus.CONFIRMED);
        testOrder.setId(50L);

        testInvoice = new Invoice(testOrder, "INV-50-20261004-ABC123", new BigDecimal("350.00"), new BigDecimal("350.00"));
        testInvoice.setId(100L);
        testInvoice.setIssuedAt(LocalDateTime.now());
        testInvoice.setCreatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. POST /api/invoices/orders/{orderId}: Authenticated user creates invoice -> 201
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId} creates invoice and returns 201 with response body")
    void createInvoice_whenAuthenticated_returns201() throws Exception {
        when(invoiceService.createInvoice(1L, 50L)).thenReturn(testInvoice);

        mockMvc.perform(post("/api/invoices/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.orderId").value(50L))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-50-20261004-ABC123"))
                .andExpect(jsonPath("$.subtotal").value(350.00))
                .andExpect(jsonPath("$.totalAmount").value(350.00))
                .andExpect(jsonPath("$.issuedAt").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    // 2. POST /api/invoices/orders/{orderId}: Correct user ID and order ID passed to InvoiceService
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId} passes resolved authenticated userId and orderId to service")
    void createInvoice_passesAuthenticatedUserIdAndOrderIdToService() throws Exception {
        when(invoiceService.createInvoice(1L, 50L)).thenReturn(testInvoice);

        mockMvc.perform(post("/api/invoices/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isCreated());

        verify(invoiceService).createInvoice(1L, 50L);
    }

    // 3. POST /api/invoices/orders/{orderId}: ResourceNotFoundException -> 404
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId} returns 404 when order is not found")
    void createInvoice_whenOrderNotFound_returns404() throws Exception {
        when(invoiceService.createInvoice(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(post("/api/invoices/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    // 4. POST /api/invoices/orders/{orderId}: Unauthenticated request -> 403
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId} returns 403 when unauthenticated")
    void createInvoice_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/invoices/orders/50"))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).createInvoice(any(), any());
    }

    // 5. GET /api/invoices/orders/{orderId}: Authenticated retrieval -> 200
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId} retrieves invoice for order and returns 200")
    void getInvoice_whenAuthenticated_returns200() throws Exception {
        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);

        mockMvc.perform(get("/api/invoices/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.orderId").value(50L))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-50-20261004-ABC123"))
                .andExpect(jsonPath("$.subtotal").value(350.00))
                .andExpect(jsonPath("$.totalAmount").value(350.00));
    }

    // 6. GET /api/invoices/orders/{orderId}: Correct user ID and order ID passed
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId} passes resolved authenticated userId and orderId")
    void getInvoice_passesAuthenticatedUserIdAndOrderIdToService() throws Exception {
        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);

        mockMvc.perform(get("/api/invoices/orders/50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(invoiceService).getInvoice(1L, 50L);
    }

    // 7. GET /api/invoices/orders/{orderId}: ResourceNotFoundException -> 404
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId} returns 404 when invoice or order is not found")
    void getInvoice_whenInvoiceNotFound_returns404() throws Exception {
        when(invoiceService.getInvoice(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Invoice not found for order id: 999"));

        mockMvc.perform(get("/api/invoices/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found for order id: 999"));
    }

    // 8. GET /api/invoices/orders/{orderId}: Unauthenticated request -> 403
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId} returns 403 when unauthenticated")
    void getInvoice_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/invoices/orders/50"))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).getInvoice(any(), any());
    }

    // 9. GET /api/invoices/{invoiceId}: Authenticated retrieval by invoiceId -> 200
    @Test
    @DisplayName("GET /api/invoices/{invoiceId} retrieves invoice by ID and returns 200")
    void getInvoiceById_whenAuthenticatedOwner_returns200() throws Exception {
        when(invoiceService.getInvoiceById(1L, 100L)).thenReturn(testInvoice);

        mockMvc.perform(get("/api/invoices/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.orderId").value(50L))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-50-20261004-ABC123"))
                .andExpect(jsonPath("$.subtotal").value(350.00))
                .andExpect(jsonPath("$.totalAmount").value(350.00));
    }

    // 10. GET /api/invoices/{invoiceId}: Passes authenticated userId and invoiceId
    @Test
    @DisplayName("GET /api/invoices/{invoiceId} passes resolved authenticated userId and invoiceId")
    void getInvoiceById_passesAuthenticatedUserIdAndInvoiceIdToService() throws Exception {
        when(invoiceService.getInvoiceById(1L, 100L)).thenReturn(testInvoice);

        mockMvc.perform(get("/api/invoices/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(invoiceService).getInvoiceById(1L, 100L);
    }

    // 11. GET /api/invoices/{invoiceId}: ResourceNotFoundException -> 404
    @Test
    @DisplayName("GET /api/invoices/{invoiceId} returns 404 when invoice is not found or not owned")
    void getInvoiceById_whenNotFoundOrWrongOwner_returns404() throws Exception {
        when(invoiceService.getInvoiceById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Invoice not found with id: 999"));

        mockMvc.perform(get("/api/invoices/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found with id: 999"));
    }

    // 12. GET /api/invoices/{invoiceId}: Unauthenticated request -> 403
    @Test
    @DisplayName("GET /api/invoices/{invoiceId} returns 403 when unauthenticated")
    void getInvoiceById_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/invoices/100"))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).getInvoiceById(any(), any());
    }

    // 13. POST /api/invoices/orders/{orderId}/email: Successful request -> 204
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email sends invoice email and returns 204")
    void sendInvoiceEmail_whenAuthenticated_returns204() throws Exception {
        InvoiceEmailRequest request = new InvoiceEmailRequest("customer@example.com");

        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(invoiceService).sendInvoiceEmail(1L, 50L, "customer@example.com");
    }

    // 14. POST /api/invoices/orders/{orderId}/email: Correct user/order delegation
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email delegates exact userId, orderId, and email to service")
    void sendInvoiceEmail_passesAuthenticatedUserIdAndOrderIdAndEmailToService() throws Exception {
        InvoiceEmailRequest request = new InvoiceEmailRequest("customer@example.com");

        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(invoiceService).sendInvoiceEmail(eq(1L), eq(50L), eq("customer@example.com"));
    }

    // 15. POST /api/invoices/orders/{orderId}/email: Invalid email -> 400
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email returns 400 for invalid email format")
    void sendInvoiceEmail_whenInvalidEmail_returns400() throws Exception {
        InvoiceEmailRequest request = new InvoiceEmailRequest("not-an-email");

        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(invoiceService, never()).sendInvoiceEmail(any(), any(), any());
    }

    // 16. POST /api/invoices/orders/{orderId}/email: Blank email -> 400
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email returns 400 for blank email")
    void sendInvoiceEmail_whenBlankEmail_returns400() throws Exception {
        InvoiceEmailRequest request = new InvoiceEmailRequest("");

        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(invoiceService, never()).sendInvoiceEmail(any(), any(), any());
    }

    // 17. POST /api/invoices/orders/{orderId}/email: Missing email -> 400
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email returns 400 for missing email property")
    void sendInvoiceEmail_whenMissingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(invoiceService, never()).sendInvoiceEmail(any(), any(), any());
    }

    // 18. POST /api/invoices/orders/{orderId}/email: Unauthenticated request -> 403
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email returns 403 when unauthenticated")
    void sendInvoiceEmail_whenUnauthenticated_returns403() throws Exception {
        InvoiceEmailRequest request = new InvoiceEmailRequest("customer@example.com");

        mockMvc.perform(post("/api/invoices/orders/50/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).sendInvoiceEmail(any(), any(), any());
    }

    // 19. POST /api/invoices/orders/{orderId}/email: Order/invoice not found -> 404
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email returns 404 when invoice or order is not found")
    void sendInvoiceEmail_whenOrderOrInvoiceNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Invoice not found for order id: 999"))
                .when(invoiceService).sendInvoiceEmail(1L, 999L, "customer@example.com");

        InvoiceEmailRequest request = new InvoiceEmailRequest("customer@example.com");

        mockMvc.perform(post("/api/invoices/orders/999/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found for order id: 999"));
    }

    // 20. POST /api/invoices/orders/{orderId}/email: Email sending failure propagation
    @Test
    @DisplayName("POST /api/invoices/orders/{orderId}/email propagates email sending failure")
    void sendInvoiceEmail_whenEmailServiceFails_propagatesException() {
        doThrow(new RuntimeException("Mail server unavailable"))
                .when(invoiceService).sendInvoiceEmail(1L, 50L, "customer@example.com");

        InvoiceEmailRequest request = new InvoiceEmailRequest("customer@example.com");

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/invoices/orders/50/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))));
    }

    // --- GET /api/invoices/orders/{orderId}/pdf Tests ---

    // 21. GET /api/invoices/orders/{orderId}/pdf: Successful PDF download
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf returns 200 with PDF content-type, content-disposition, and bytes")
    void downloadInvoicePdf_whenAuthenticated_returns200WithPdf() throws Exception {
        byte[] expectedPdfBytes = "%PDF-1.4 mock pdf data".getBytes(StandardCharsets.UTF_8);
        List<OrderItem> orderItems = Collections.emptyList();

        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(orderItems);
        when(invoicePdfService.generateInvoicePdf(testInvoice, orderItems)).thenReturn(expectedPdfBytes);

        MvcResult result = mockMvc.perform(get("/api/invoices/orders/50/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-INV-50-20261004-ABC123.pdf\""))
                .andReturn();

        assertArrayEquals(expectedPdfBytes, result.getResponse().getContentAsByteArray());
    }

    // 22. GET /api/invoices/orders/{orderId}/pdf: Correct delegation
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf delegates correctly to invoiceService, orderItemRepository, and invoicePdfService")
    void downloadInvoicePdf_delegatesToDependencies() throws Exception {
        byte[] expectedPdfBytes = "%PDF-1.4 mock pdf data".getBytes(StandardCharsets.UTF_8);
        List<OrderItem> orderItems = Collections.emptyList();

        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(orderItems);
        when(invoicePdfService.generateInvoicePdf(testInvoice, orderItems)).thenReturn(expectedPdfBytes);

        mockMvc.perform(get("/api/invoices/orders/50/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(invoiceService).getInvoice(1L, 50L);
        verify(orderItemRepository).findByOrder(testOrder);
        verify(invoicePdfService).generateInvoicePdf(testInvoice, orderItems);
    }

    // 23. GET /api/invoices/orders/{orderId}/pdf: Invoice/order not found -> 404
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf returns 404 when invoice or order is not found")
    void downloadInvoicePdf_whenInvoiceNotFound_returns404() throws Exception {
        when(invoiceService.getInvoice(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Invoice not found for order id: 999"));

        mockMvc.perform(get("/api/invoices/orders/999/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found for order id: 999"));

        verify(orderItemRepository, never()).findByOrder(any());
        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
    }

    // 24. GET /api/invoices/orders/{orderId}/pdf: Unauthenticated request -> 403
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf returns 403 when unauthenticated")
    void downloadInvoicePdf_whenUnauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/invoices/orders/50/pdf"))
                .andExpect(status().isForbidden());

        verify(invoiceService, never()).getInvoice(any(), any());
        verify(orderItemRepository, never()).findByOrder(any());
        verify(invoicePdfService, never()).generateInvoicePdf(any(), any());
    }

    // 25. GET /api/invoices/orders/{orderId}/pdf: PDF generation failure propagation
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf propagates PDF generation failure")
    void downloadInvoicePdf_whenPdfGenerationFails_propagatesException() {
        List<OrderItem> orderItems = Collections.emptyList();

        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(orderItems);
        when(invoicePdfService.generateInvoicePdf(testInvoice, orderItems))
                .thenThrow(new RuntimeException("PDF rendering engine failed"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/invoices/orders/50/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)));
    }

    // 26. GET /api/invoices/orders/{orderId}/pdf: No mutation on any repository
    @Test
    @DisplayName("GET /api/invoices/orders/{orderId}/pdf performs no mutation operations")
    void downloadInvoicePdf_doesNotMutateRepositories() throws Exception {
        byte[] expectedPdfBytes = "%PDF-1.4 mock pdf data".getBytes(StandardCharsets.UTF_8);
        List<OrderItem> orderItems = Collections.emptyList();

        when(invoiceService.getInvoice(1L, 50L)).thenReturn(testInvoice);
        when(orderItemRepository.findByOrder(testOrder)).thenReturn(orderItems);
        when(invoicePdfService.generateInvoicePdf(testInvoice, orderItems)).thenReturn(expectedPdfBytes);

        mockMvc.perform(get("/api/invoices/orders/50/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());

        verify(orderItemRepository, never()).save(any());
        verify(orderItemRepository, never()).delete(any());
    }
}
