package com.vasudev.shopsphere.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test-path");
    }

    @Test
    @DisplayName("DuplicateUserException returns HTTP 409 Conflict with message")
    void handleDuplicateUserException_returnsConflict() {
        DuplicateUserException ex = new DuplicateUserException("User with email already exists");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleDuplicateUserException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User with email already exists", response.getBody().get("message"));
    }

    @Test
    @DisplayName("ResourceNotFoundException returns HTTP 404 Not Found with message")
    void handleResourceNotFoundException_returnsNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product with ID 42 not found");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Product with ID 42 not found", response.getBody().get("message"));
    }

    @Test
    @DisplayName("IllegalArgumentException returns HTTP 400 Bad Request with message")
    void handleIllegalArgumentException_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid status provided");

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid status provided", response.getBody().get("message"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns HTTP 400 Bad Request with field details")
    void handleMethodArgumentNotValidException_withFieldErrors_returnsBadRequestWithFieldDetails() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "name", "Name is required"));
        bindingResult.addError(new FieldError("testObject", "price", "Price must be greater than 0"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class),
                0
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMethodArgumentNotValidException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("/api/test-path", response.getBody().get("path"));
        assertNotNull(response.getBody().get("timestamp"));

        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("name: Name is required"));
        assertTrue(message.contains("price: Price must be greater than 0"));
        assertTrue(message.startsWith("Validation failed: "));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException with no field errors returns fallback message")
    void handleMethodArgumentNotValidException_withoutFieldErrors_returnsFallbackMessage() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class),
                0
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMethodArgumentNotValidException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Generic unexpected Exception returns HTTP 500 with fixed safe message")
    void handleGenericException_returnsInternalServerErrorWithSanitizedMessage() {
        Exception ex = new RuntimeException("Sensitive database syntax error on table users_internal");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
        assertEquals("/api/test-path", response.getBody().get("path"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("Generic 500 response does NOT expose internal exception message or stack trace")
    void handleGenericException_doesNotExposeInternalMessageOrStackDetails() {
        String sensitiveMessage = "SELECT * FROM users WHERE password_hash = 'secret123'";
        Exception ex = new IllegalStateException(sensitiveMessage, new NullPointerException("nested trace"));

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(ex, request);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.containsKey("stackTrace"));
        assertFalse(body.containsKey("exception"));
        assertFalse(body.containsKey("trace"));
        assertFalse(body.values().stream().map(Object::toString).anyMatch(val -> val.contains("secret123")));
        assertFalse(body.values().stream().map(Object::toString).anyMatch(val -> val.contains("IllegalStateException")));
        assertFalse(body.values().stream().map(Object::toString).anyMatch(val -> val.contains("NullPointerException")));
        assertEquals("An unexpected error occurred", body.get("message"));
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
    }
}
