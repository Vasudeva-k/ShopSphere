package com.vasudev.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vasudev.shopsphere.dto.UserUpdateRequest;
import com.vasudev.shopsphere.entity.User;
import com.vasudev.shopsphere.exception.DuplicateUserException;
import com.vasudev.shopsphere.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        encoder = new BCryptPasswordEncoder();
    }

    // ==========================================
    // 1. Password Verification Tests
    // ==========================================

    @Test
    void verifyPassword_withCorrectPassword_returnsTrue() {
        String rawPassword = "Password123";
        String encodedPassword = encoder.encode(rawPassword);

        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        assertTrue(result);
    }

    @Test
    void verifyPassword_withIncorrectPassword_returnsFalse() {
        String rawPassword = "Password123";
        String encodedPassword = encoder.encode(rawPassword);

        boolean result = userService.verifyPassword("WrongPassword123", encodedPassword);

        assertFalse(result);
    }

    @Test
    void verifyPassword_withDifferentPassword_returnsTrue() {
        String rawPassword = "NewPassword123";
        String encodedPassword = encoder.encode(rawPassword);

        boolean result = userService.verifyPassword(rawPassword, encodedPassword);

        assertTrue(result);
    }

    // ==========================================
    // 2. Authentication Tests
    // ==========================================

    @Test
    void authenticate_withValidCredentials_returnsUser() {
        String rawPassword = "Password123";
        String encodedPassword = encoder.encode(rawPassword);
        User user = new User("Jane", "Doe", "jane@example.com", encodedPassword, "1234567890");
        user.setId(1L);
        user.setEnabled(true);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate("jane@example.com", rawPassword);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void authenticate_whenUserNotFound_returnsEmpty() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.authenticate("unknown@example.com", "Password123");

        assertTrue(result.isEmpty());
    }

    @Test
    void authenticate_whenUserDisabled_returnsEmpty() {
        String rawPassword = "Password123";
        String encodedPassword = encoder.encode(rawPassword);
        User user = new User("Jane", "Doe", "jane@example.com", encodedPassword, "1234567890");
        user.setId(1L);
        user.setEnabled(false);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate("jane@example.com", rawPassword);

        assertTrue(result.isEmpty());
    }

    @Test
    void authenticate_whenPasswordMismatch_returnsEmpty() {
        String encodedPassword = encoder.encode("CorrectPassword123");
        User user = new User("Jane", "Doe", "jane@example.com", encodedPassword, "1234567890");
        user.setId(1L);
        user.setEnabled(true);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate("jane@example.com", "WrongPassword123");

        assertTrue(result.isEmpty());
    }

    // ==========================================
    // 3. User Creation Tests
    // ==========================================

    @Test
    void saveUser_withUniqueEmail_encodesPasswordAndSaves() {
        User inputUser = new User("Jane", "Doe", "jane@example.com", "RawPassword123", "1234567890");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.saveUser(inputUser);

        assertNotNull(savedUser);
        assertFalse(savedUser.getPassword().equals("RawPassword123"));
        assertTrue(encoder.matches("RawPassword123", savedUser.getPassword()));
        verify(userRepository).findByEmail("jane@example.com");
        verify(userRepository).save(inputUser);
    }

    @Test
    void saveUser_withDuplicateEmail_throwsDuplicateUserException() {
        User existingUser = new User("Existing", "User", "jane@example.com", "EncPassword", "1234567890");
        User newUser = new User("Jane", "Doe", "jane@example.com", "RawPassword123", "1234567890");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(DuplicateUserException.class, () -> userService.saveUser(newUser));

        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // 4. User Lookup / Update Tests
    // ==========================================

    @Test
    void updateUser_whenUserNotFound_returnsEmpty() {
        UserUpdateRequest request = new UserUpdateRequest("Jane", "Doe", "jane@example.com", "NewPassword123", "1234567890");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateUser(999L, request);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_withDuplicateEmail_throwsDuplicateUserException() {
        User existingUser = new User("Jane", "Doe", "jane@example.com", "EncPassword", "1234567890");
        existingUser.setId(1L);

        User otherUser = new User("Other", "User", "other@example.com", "EncPassword", "0987654321");
        otherUser.setId(2L);

        UserUpdateRequest request = new UserUpdateRequest("Jane", "Doe", "other@example.com", null, "1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateUserException.class, () -> userService.updateUser(1L, request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_withNewPassword_encodesPasswordAndSaves() {
        String oldEncodedPassword = encoder.encode("OldPassword123");
        User existingUser = new User("Jane", "Doe", "jane@example.com", oldEncodedPassword, "1234567890");
        existingUser.setId(1L);

        UserUpdateRequest request = new UserUpdateRequest("Janet", "Smith", "janet.smith@example.com", "NewPassword123", "9999999999");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("janet.smith@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<User> result = userService.updateUser(1L, request);

        assertTrue(result.isPresent());
        User updatedUser = result.get();
        assertEquals("Janet", updatedUser.getFirstName());
        assertEquals("Smith", updatedUser.getLastName());
        assertEquals("janet.smith@example.com", updatedUser.getEmail());
        assertEquals("9999999999", updatedUser.getPhone());
        assertTrue(encoder.matches("NewPassword123", updatedUser.getPassword()));
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_withoutPasswordChange_preservesExistingPasswordAndSaves() {
        String oldEncodedPassword = encoder.encode("OldPassword123");
        User existingUser = new User("Jane", "Doe", "jane@example.com", oldEncodedPassword, "1234567890");
        existingUser.setId(1L);

        UserUpdateRequest requestWithNullPass = new UserUpdateRequest("Janet", "Smith", "jane@example.com", null, "9999999999");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<User> result = userService.updateUser(1L, requestWithNullPass);

        assertTrue(result.isPresent());
        User updatedUser = result.get();
        assertEquals("Janet", updatedUser.getFirstName());
        assertEquals("Smith", updatedUser.getLastName());
        assertEquals(oldEncodedPassword, updatedUser.getPassword());
        verify(userRepository).save(existingUser);
    }

    // ==========================================
    // 5. Delete User Tests
    // ==========================================

    @Test
    void deleteUser_whenUserExists_returnsTrue() {
        when(userRepository.existsById(1L)).thenReturn(true);

        boolean result = userService.deleteUser(1L);

        assertTrue(result);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_whenUserDoesNotExist_returnsFalse() {
        when(userRepository.existsById(999L)).thenReturn(false);

        boolean result = userService.deleteUser(999L);

        assertFalse(result);
        verify(userRepository, never()).deleteById(any());
    }

    // ==========================================
    // 6. Repository Delegation Lookup Tests
    // ==========================================

    @Test
    void getUserById_delegatesToRepository() {
        User user = new User("Jane", "Doe", "jane@example.com", "Password123", "1234567890");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserByEmail_delegatesToRepository() {
        User user = new User("Jane", "Doe", "jane@example.com", "Password123", "1234567890");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("jane@example.com");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByEmail("jane@example.com");
    }

    @Test
    void getAllUsers_delegatesToRepository() {
        User user1 = new User("Jane", "Doe", "jane@example.com", "Password123", "1234567890");
        User user2 = new User("John", "Smith", "john@example.com", "Password123", "0987654321");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }
}

