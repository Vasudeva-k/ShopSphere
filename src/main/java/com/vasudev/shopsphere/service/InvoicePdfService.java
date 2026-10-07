package com.vasudev.shopsphere.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.vasudev.shopsphere.entity.Invoice;
import com.vasudev.shopsphere.entity.OrderItem;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateInvoicePdf(Invoice invoice, List<OrderItem> orderItems) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }
        if (orderItems == null) {
            throw new IllegalArgumentException("Order items cannot be null");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            try {
                PdfWriter.getInstance(document, out);
                document.open();

                // Fonts
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

                // Header
                Paragraph brand = new Paragraph("ShopSphere", titleFont);
                brand.setAlignment(Element.ALIGN_CENTER);
                document.add(brand);

                Paragraph invoiceTitle = new Paragraph("INVOICE", headerFont);
                invoiceTitle.setAlignment(Element.ALIGN_CENTER);
                invoiceTitle.setSpacingAfter(15f);
                document.add(invoiceTitle);

                // Invoice Information
                Long orderId = (invoice.getOrder() != null) ? invoice.getOrder().getId() : null;
                String issuedDate = (invoice.getIssuedAt() != null)
                        ? invoice.getIssuedAt().format(DATE_FORMATTER)
                        : "N/A";

                document.add(new Paragraph("Invoice Number: " + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "N/A"), boldFont));
                document.add(new Paragraph("Order ID: " + (orderId != null ? orderId : "N/A"), normalFont));
                document.add(new Paragraph("Issued Date: " + issuedDate, normalFont));

                Paragraph spacing = new Paragraph(" ");
                spacing.setSpacingAfter(10f);
                document.add(spacing);

                // Items Table
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{4f, 2f, 1.5f, 2f});

                // Table Header
                addHeaderCell(table, "Product Name", boldFont);
                addHeaderCell(table, "Unit Price", boldFont);
                addHeaderCell(table, "Quantity", boldFont);
                addHeaderCell(table, "Subtotal", boldFont);

                // Table Rows
                for (OrderItem item : orderItems) {
                    String productName = item.getProductName() != null ? item.getProductName() : "N/A";
                    String unitPrice = item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0.00";
                    String quantity = item.getQuantity() != null ? item.getQuantity().toString() : "0";
                    String itemSubtotal = item.getSubtotal() != null ? item.getSubtotal().toString() : "0.00";

                    table.addCell(new PdfPCell(new Phrase(productName, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(unitPrice, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(quantity, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(itemSubtotal, normalFont)));
                }

                document.add(table);

                // Totals
                Paragraph totalsSpacing = new Paragraph(" ");
                totalsSpacing.setSpacingAfter(10f);
                document.add(totalsSpacing);

                String subtotalStr = invoice.getSubtotal() != null ? invoice.getSubtotal().toString() : "0.00";
                String totalStr = invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : "0.00";

                Paragraph subtotalPara = new Paragraph("Subtotal: " + subtotalStr, boldFont);
                subtotalPara.setAlignment(Element.ALIGN_RIGHT);
                document.add(subtotalPara);

                Paragraph totalPara = new Paragraph("Total Amount: " + totalStr, boldFont);
                totalPara.setAlignment(Element.ALIGN_RIGHT);
                document.add(totalPara);

            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell headerCell = new PdfPCell(new Phrase(text, font));
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell);
    }
}
