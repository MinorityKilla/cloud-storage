package com.cloudstorage.service;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private User testUser;
    private FileEntity testFile;

    @BeforeEach
    void setUp() {
        // Устанавливаем upload-dir для тестов
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());

        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");

        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setFilename("test.txt");
        testFile.setSize(100L);
        testFile.setFilePath(tempDir.resolve("testuser/test.txt").toString());
        testFile.setUser(testUser);
    }

    @Test
    void uploadFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFile);

        fileService.uploadFile(testUser, "test.txt", file);

        verify(fileRepository, times(1)).save(any(FileEntity.class));
    }

    @Test
    void deleteFile_Success() throws IOException {
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.of(testFile));
        doNothing().when(fileRepository).delete(testFile);

        fileService.deleteFile(testUser, "test.txt");

        verify(fileRepository, times(1)).delete(testFile);
    }

    @Test
    void deleteFile_NotFound() {
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.deleteFile(testUser, "test.txt"));
    }

    @Test
    void getFile_Success() {
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.of(testFile));

        FileEntity result = fileService.getFile(testUser, "test.txt");

        assertNotNull(result);
        assertEquals("test.txt", result.getFilename());
    }

    @Test
    void getFile_NotFound() {
        when(fileRepository.findByUserAndFilename(testUser, "test.txt")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.getFile(testUser, "test.txt"));
    }

    @Test
    void renameFile_Success() {
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.of(testFile));
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFile);

        fileService.renameFile(testUser, "old.txt", "new.txt");

        assertEquals("new.txt", testFile.getFilename());
        verify(fileRepository, times(1)).save(testFile);
    }

    @Test
    void renameFile_NotFound() {
        when(fileRepository.findByUserAndFilename(testUser, "old.txt")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.renameFile(testUser, "old.txt", "new.txt"));
    }

    @Test
    void getFiles_WithLimit() {
        FileEntity file1 = new FileEntity();
        file1.setFilename("file1.txt");
        file1.setSize(100L);

        FileEntity file2 = new FileEntity();
        file2.setFilename("file2.txt");
        file2.setSize(200L);

        List<FileEntity> allFiles = Arrays.asList(file1, file2);
        when(fileRepository.findByUser(testUser)).thenReturn(allFiles);

        List<FileEntity> result = fileService.getFiles(testUser, 1);

        assertEquals(1, result.size());
        assertEquals("file1.txt", result.get(0).getFilename());
    }

    @Test
    void getFiles_NoLimit() {
        FileEntity file1 = new FileEntity();
        file1.setFilename("file1.txt");
        file1.setSize(100L);

        FileEntity file2 = new FileEntity();
        file2.setFilename("file2.txt");
        file2.setSize(200L);

        List<FileEntity> allFiles = Arrays.asList(file1, file2);
        when(fileRepository.findByUser(testUser)).thenReturn(allFiles);

        List<FileEntity> result = fileService.getFiles(testUser, null);

        assertEquals(2, result.size());
    }
}