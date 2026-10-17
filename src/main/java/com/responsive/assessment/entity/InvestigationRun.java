
package com.responsive.assessment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_runs")
public class InvestigationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ownerId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, length = 2000)
    private String conclusion;

    @Lob
    private String evidenceJson;

    @Lob
    private String conflictsJson;

    @Lob
    private String missingInformationJson;

    @Column(length = 1000)
    private String nextAction;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public InvestigationRun() {
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getConflictsJson() {
        return conflictsJson;
    }

    public void setConflictsJson(String conflictsJson) {
        this.conflictsJson = conflictsJson;
    }

    public String getMissingInformationJson() {
        return missingInformationJson;
    }

    public void setMissingInformationJson(String missingInformationJson) {
        this.missingInformationJson = missingInformationJson;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
