package com.cloudstorage.controller;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.User;
import com.cloudstorage.service.AuthService;
import com.cloudstorage.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getFiles_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        FileEntity file1 = new FileEntity();
        file1.setFilename("file1.txt");
        file1.setSize(100L);

        FileEntity file2 = new FileEntity();
        file2.setFilename("file2.txt");
        file2.setSize(200L);

        List<FileEntity> files = Arrays.asList(file1, file2);

        when(authService.getUserByToken("valid-token")).thenReturn(user);
        when(fileService.getFiles(eq(user), any())).thenReturn(files);

        mockMvc.perform(get("/cloud/list")
                        .header("auth-token", "valid-token")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("file1.txt"))
                .andExpect(jsonPath("$[0].size").value(100))
                .andExpect(jsonPath("$[1].filename").value("file2.txt"))
                .andExpect(jsonPath("$[1].size").value(200));
    }

    @Test
    void uploadFile_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        when(authService.getUserByToken("valid-token")).thenReturn(user);
        doNothing().when(fileService).uploadFile(any(User.class), anyString(), any());

        mockMvc.perform(multipart("/cloud/file")
                        .file(file)
                        .header("auth-token", "valid-token")
                        .param("filename", "test.txt"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteFile_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        when(authService.getUserByToken("valid-token")).thenReturn(user);
        doNothing().when(fileService).deleteFile(user, "test.txt");

        mockMvc.perform(delete("/cloud/file")
                        .header("auth-token", "valid-token")
                        .param("filename", "test.txt"))
                .andExpect(status().isOk());
    }

    @Test
    void renameFile_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        Map<String, String> requestBody = Map.of("name", "newfile.txt");

        when(authService.getUserByToken("valid-token")).thenReturn(user);
        doNothing().when(fileService).renameFile(user, "oldfile.txt", "newfile.txt");

        mockMvc.perform(put("/cloud/file")
                        .header("auth-token", "valid-token")
                        .param("filename", "oldfile.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    @Test
    void unauthorized_Access() throws Exception {
        when(authService.getUserByToken("invalid-token"))
                .thenThrow(new RuntimeException("Unauthorized"));

        mockMvc.perform(get("/cloud/list")
                        .header("auth-token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }
}