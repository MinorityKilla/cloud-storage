package com.cloudstorage.service;

import com.cloudstorage.dto.LoginRequest;
import com.cloudstorage.dto.LoginResponse;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");
        testUser.setPassword("testpass");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("testuser");
        loginRequest.setPassword("testpass");
    }

    @Test
    void login_Success() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getAuthToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_BadCredentials_WrongPassword() {
        loginRequest.setPassword("wrongpass");
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_BadCredentials_UserNotFound() {
        when(userRepository.findByLogin("testuser")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }

    @Test
    void logout_Success() {
        testUser.setAuthToken("test-token");
        when(userRepository.findByAuthToken("test-token")).thenReturn(Optional.of(testUser));

        authService.logout("test-token");

        assertNull(testUser.getAuthToken());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getUserByToken_Success() {
        testUser.setAuthToken("valid-token");
        when(userRepository.findByAuthToken("valid-token")).thenReturn(Optional.of(testUser));

        User user = authService.getUserByToken("valid-token");

        assertNotNull(user);
        assertEquals("testuser", user.getLogin());
    }

    @Test
    void getUserByToken_Unauthorized() {
        when(userRepository.findByAuthToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserByToken("invalid-token"));
    }
}