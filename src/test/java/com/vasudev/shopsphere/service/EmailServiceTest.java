package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    // --- sendEmail Tests ---

    @Test
    @DisplayName("sendEmail successfully sends email with expected to, subject, and text")
    void sendEmail_successful() {
        String to = "recipient@example.com";
        String subject = "Order Confirmation";
        String body = "Thank you for your order!";

        emailService.sendEmail(to, subject, body);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertArrayEquals(new String[]{"recipient@example.com"}, sentMessage.getTo());
        assertEquals("Order Confirmation", sentMessage.getSubject());
        assertEquals("Thank you for your order!", sentMessage.getText());
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'to' is null")
    void sendEmail_whenToIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail(null, "Subject", "Body"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'to' is blank")
    void sendEmail_whenToIsBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail("   ", "Subject", "Body"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'subject' is null")
    void sendEmail_whenSubjectIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail("recipient@example.com", null, "Body"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'subject' is blank")
    void sendEmail_whenSubjectIsBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail("recipient@example.com", "   ", "Body"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'body' is null")
    void sendEmail_whenBodyIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail("recipient@example.com", "Subject", null));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail throws IllegalArgumentException when 'body' is blank")
    void sendEmail_whenBodyIsBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmail("recipient@example.com", "Subject", "   "));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail propagates mail sender exceptions to caller")
    void sendEmail_whenMailSenderFails_propagatesException() {
        doThrow(new MailSendException("SMTP server connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class,
                () -> emailService.sendEmail("recipient@example.com", "Subject", "Body"));
    }

    // --- sendEmailWithAttachment Tests ---

    @Test
    @DisplayName("sendEmailWithAttachment successfully creates and sends MimeMessage with attachment")
    void sendEmailWithAttachment_successful() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        String to = "customer@example.com";
        String subject = "Invoice INV-1001";
        String body = "Please find attached your invoice PDF.";
        byte[] attachmentData = "%PDF-1.4 sample invoice data".getBytes(StandardCharsets.UTF_8);
        String attachmentFileName = "invoice-1001.pdf";

        emailService.sendEmailWithAttachment(to, subject, body, attachmentData, attachmentFileName);

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);

        assertNotNull(mimeMessage.getAllRecipients());
        assertEquals(1, mimeMessage.getAllRecipients().length);
        assertEquals("customer@example.com", mimeMessage.getAllRecipients()[0].toString());
        assertEquals("Invoice INV-1001", mimeMessage.getSubject());
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'to' is null")
    void sendEmailWithAttachment_whenToIsNull_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment(null, "Subject", "Body", attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'to' is blank")
    void sendEmailWithAttachment_whenToIsBlank_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("   ", "Subject", "Body", attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'subject' is null")
    void sendEmailWithAttachment_whenSubjectIsNull_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", null, "Body", attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'subject' is blank")
    void sendEmailWithAttachment_whenSubjectIsBlank_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "   ", "Body", attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'body' is null")
    void sendEmailWithAttachment_whenBodyIsNull_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", null, attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when 'body' is blank")
    void sendEmailWithAttachment_whenBodyIsBlank_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "   ", attachmentData, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when attachmentData is null")
    void sendEmailWithAttachment_whenAttachmentDataIsNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "Body", null, "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when attachmentData is empty")
    void sendEmailWithAttachment_whenAttachmentDataIsEmpty_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "Body", new byte[0], "invoice.pdf"));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when attachmentFileName is null")
    void sendEmailWithAttachment_whenAttachmentFileNameIsNull_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "Body", attachmentData, null));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment throws IllegalArgumentException when attachmentFileName is blank")
    void sendEmailWithAttachment_whenAttachmentFileNameIsBlank_throwsIllegalArgumentException() {
        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "Body", attachmentData, "   "));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailWithAttachment propagates mail sender exceptions to caller")
    void sendEmailWithAttachment_whenMailSenderFails_propagatesException() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server error"))
                .when(mailSender).send(any(MimeMessage.class));

        byte[] attachmentData = "data".getBytes(StandardCharsets.UTF_8);

        assertThrows(MailSendException.class,
                () -> emailService.sendEmailWithAttachment("customer@example.com", "Subject", "Body", attachmentData, "invoice.pdf"));
    }
}
