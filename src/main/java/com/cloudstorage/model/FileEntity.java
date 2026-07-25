package com.cloudstorage.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(nullable = false)
    private Long size;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "content_type")
    private String contentType;

    @Column
    private String hash;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @PrePersist
    public void prePersist() {
        uploadDate = LocalDateTime.now();
    }
}