package com.cloudstorage.controller;

import com.cloudstorage.dto.LoginRequest;
import com.cloudstorage.dto.LoginResponse;
import com.cloudstorage.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin("user");
        request.setPassword("password");

        LoginResponse response = new LoginResponse("test-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth-token").value("test-token"));
    }

    @Test
    void login_BadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setLogin("user");
        request.setPassword("wrong");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad credentials"))
                .andExpect(jsonPath("$.id").value(400));
    }

    @Test
    void logout_Success() throws Exception {
        doNothing().when(authService).logout("valid-token");

        mockMvc.perform(post("/logout")
                        .header("auth-token", "valid-token"))
                .andExpect(status().isOk());
    }
}