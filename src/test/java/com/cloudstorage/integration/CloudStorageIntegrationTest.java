package com.cloudstorage.integration;

import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CloudStorageIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Очищаем базу перед каждым тестом
        fileRepository.deleteAll();
        userRepository.deleteAll();

        // Создаём тестового пользователя
        User user = new User();
        user.setLogin("testuser");
        user.setPassword("testpass");
        userRepository.save(user);

        // Логинимся и получаем токен
        Map<String, String> loginRequest = Map.of("login", "testuser", "password", "testpass");

        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response).get("auth-token").asText();
    }

    @Test
    void fullFlow_UploadListDownloadDelete() throws Exception {
        // 1. Загружаем файл
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        mockMvc.perform(multipart("/cloud/file")
                        .file(file)
                        .header("auth-token", authToken)
                        .param("filename", "test.txt"))
                .andExpect(status().isOk());

        // 2. Проверяем список файлов
        mockMvc.perform(get("/cloud/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(11));

        // 3. Переименовываем файл
        Map<String, String> renameRequest = Map.of("name", "renamed.txt");
        mockMvc.perform(put("/cloud/file")
                        .header("auth-token", authToken)
                        .param("filename", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isOk());

        // 4. Скачиваем файл
        mockMvc.perform(get("/cloud/file")
                        .header("auth-token", authToken)
                        .param("filename", "renamed.txt"))
                .andExpect(status().isOk());

        // 5. Удаляем файл
        mockMvc.perform(delete("/cloud/file")
                        .header("auth-token", authToken)
                        .param("filename", "renamed.txt"))
                .andExpect(status().isOk());

        // 6. Проверяем что файлов нет
        mockMvc.perform(get("/cloud/list")
                        .header("auth-token", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void logout_InvalidatesToken() throws Exception {
        // Логаут
        mockMvc.perform(post("/logout")
                        .header("auth-token", authToken))
                .andExpect(status().isOk());

        // Попытка доступа с невалидным токеном
        mockMvc.perform(get("/cloud/list")
                        .header("auth-token", authToken))
                .andExpect(status().isUnauthorized());
    }
}