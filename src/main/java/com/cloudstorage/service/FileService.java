package com.cloudstorage.service;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public void uploadFile(User user, String filename, MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir, user.getLogin());
        Files.createDirectories(uploadPath);

        // Удаляем старый файл если существует
        Optional<FileEntity> existingFile = fileRepository.findByUserAndFilename(user, filename);
        if (existingFile.isPresent()) {
            Path oldFile = Paths.get(existingFile.get().getFilePath());
            Files.deleteIfExists(oldFile);
            fileRepository.delete(existingFile.get());
        }

        // Сохраняем файл на диск
        String filePath = uploadPath.resolve(filename).toString();
        Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

        // Создаем запись в БД
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(filename);
        fileEntity.setOriginalFilename(file.getOriginalFilename());
        fileEntity.setSize(file.getSize());
        fileEntity.setFilePath(filePath);
        fileEntity.setContentType(file.getContentType());
        fileEntity.setUser(user);

        fileRepository.save(fileEntity);
    }

    public void deleteFile(User user, String filename) throws IOException {
        Optional<FileEntity> fileOpt = fileRepository.findByUserAndFilename(user, filename);
        if (fileOpt.isPresent()) {
            Path filePath = Paths.get(fileOpt.get().getFilePath());
            Files.deleteIfExists(filePath);
            fileRepository.delete(fileOpt.get());
        } else {
            throw new RuntimeException("File not found");
        }
    }

    public FileEntity getFile(User user, String filename) {
        return fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public void renameFile(User user, String filename, String newFilename) {
        Optional<FileEntity> fileOpt = fileRepository.findByUserAndFilename(user, filename);
        if (fileOpt.isPresent()) {
            FileEntity file = fileOpt.get();
            file.setFilename(newFilename);
            fileRepository.save(file);
        } else {
            throw new RuntimeException("File not found");
        }
    }

    public List<FileEntity> getFiles(User user, Integer limit) {
        List<FileEntity> files = fileRepository.findByUser(user);
        if (limit != null && limit > 0 && limit < files.size()) {
            return files.subList(0, limit);
        }
        return files;
    }
}