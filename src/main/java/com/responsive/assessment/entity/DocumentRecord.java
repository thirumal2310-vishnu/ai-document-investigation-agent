
package com.responsive.assessment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String ownerId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public DocumentRecord() {
    }

    public DocumentRecord(
            String fileName, String ownerId, String content) {
        this.fileName = fileName;
        this.ownerId = ownerId;
        this.content = content;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
