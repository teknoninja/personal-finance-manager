package com.financemanager.service;

import com.financemanager.dto.request.RegisterRequest;
import com.financemanager.entity.User;
import com.financemanager.exception.ConflictException;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("user@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("John Doe");
        registerRequest.setPhoneNumber("+1234567890");
    }

    @Test
    void register_savesNewUser_whenUsernameNotTaken() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = authService.register(registerRequest);

        assertEquals(1L, result.getId());
        assertEquals("hashed", result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("user@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }
}
