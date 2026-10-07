package com.vasudev.shopsphere.controller;

import com.vasudev.shopsphere.dto.InvoiceEmailRequest;
import com.vasudev.shopsphere.dto.InvoiceResponse;
import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.OrderItem;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.ResourceNotFoundException;
import com.vasudev.shopsphere.repository.OrderItemRepository;
import com.vasudev.shopsphere.service.InvoicePdfService;
import com.vasudev.shopsphere.service.InvoiceService;
import com.vasudev.shopsphere.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice retrieval, PDF download, and email operations")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserService userService;
    private final InvoicePdfService invoicePdfService;
    private final OrderItemRepository orderItemRepository;

    public InvoiceController(
            InvoiceService invoiceService,
            UserService userService,
            InvoicePdfService invoicePdfService,
            OrderItemRepository orderItemRepository
    ) {
        this.invoiceService = invoiceService;
        this.userService = userService;
        this.invoicePdfService = invoicePdfService;
        this.orderItemRepository = orderItemRepository;
    }

    @PostMapping("/orders/{orderId}")
    @Operation(summary = "Create invoice", description = "Generates a new invoice for a paid order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Order not paid or invalid order state"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Invoice already exists for this order")
    })
    public ResponseEntity<InvoiceResponse> createInvoice(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Invoice invoice = invoiceService.createInvoice(user.getId(), orderId);
        return new ResponseEntity<>(InvoiceResponse.fromEntity(invoice), HttpStatus.CREATED);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get invoice by order ID", description = "Retrieves an invoice associated with a specific order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> getInvoice(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Invoice invoice = invoiceService.getInvoice(user.getId(), orderId);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves an invoice by its unique invoice ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> getInvoiceById(@Parameter(description = "Invoice ID", required = true) @PathVariable Long invoiceId) {
        User user = getAuthenticatedUser();
        Invoice invoice = invoiceService.getInvoiceById(user.getId(), invoiceId);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @PostMapping("/orders/{orderId}/email")
    @Operation(summary = "Send invoice email", description = "Sends the invoice PDF to the specified recipient email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email request data"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<Void> sendInvoiceEmail(
            @Parameter(description = "Order ID", required = true) @PathVariable Long orderId,
            @Valid @RequestBody InvoiceEmailRequest request
    ) {
        User user = getAuthenticatedUser();
        invoiceService.sendInvoiceEmail(user.getId(), orderId, request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/{orderId}/pdf")
    @Operation(summary = "Download invoice PDF", description = "Generates and downloads the invoice PDF for a specific order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice PDF downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<byte[]> downloadInvoicePdf(@Parameter(description = "Order ID", required = true) @PathVariable Long orderId) {
        User user = getAuthenticatedUser();
        Invoice invoice = invoiceService.getInvoice(user.getId(), orderId);
        List<OrderItem> orderItems = orderItemRepository.findByOrder(invoice.getOrder());
        byte[] pdfData = invoicePdfService.generateInvoicePdf(invoice, orderItems);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + invoice.getInvoiceNumber() + ".pdf\""
                )
                .body(pdfData);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        String email = authentication.getName();
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
