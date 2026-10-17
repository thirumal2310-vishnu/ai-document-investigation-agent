
package com.responsive.assessment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigations")
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentName;

    @Column(nullable = false)
    private String status;

    @Column(length = 5000)
    private String findings;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Investigation() {
    }

    public Investigation(String documentName, String status) {
        this.documentName = documentName;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
