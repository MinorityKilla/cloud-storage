package com.cloudstorage.repository;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUser(User user);
    Optional<FileEntity> findByUserAndFilename(User user, String filename);
    void deleteByUserAndFilename(User user, String filename);
}