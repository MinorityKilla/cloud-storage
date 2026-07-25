package com.cloudstorage.controller;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.User;
import com.cloudstorage.service.AuthService;
import com.cloudstorage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cloud")
public class FileController {
    private final FileService fileService;
    private final AuthService authService;

    @GetMapping("/list")
    public ResponseEntity<?> getFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            User user = authService.getUserByToken(token);
            List<FileEntity> files = fileService.getFiles(user, limit);

            List<Map<String, Object>> response = files.stream()
                    .map(file -> Map.<String, Object>of(
                            "filename", file.getFilename(),
                            "size", file.getSize()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized", "id", 401));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {
        try {
            User user = authService.getUserByToken(token);
            fileService.uploadFile(user, filename, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "id", 500));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        try {
            User user = authService.getUserByToken(token);
            fileService.deleteFile(user, filename);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "id", 500));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        try {
            User user = authService.getUserByToken(token);
            FileEntity fileEntity = fileService.getFile(user, filename);

            Path filePath = Paths.get(fileEntity.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileEntity.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "id", 500));
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestBody Map<String, String> body) {
        try {
            User user = authService.getUserByToken(token);
            String newFilename = body.get("name");
            fileService.renameFile(user, filename, newFilename);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage(), "id", 500));
        }
    }
}