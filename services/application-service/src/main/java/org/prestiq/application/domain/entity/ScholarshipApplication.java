package org.prestiq.application.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scholarship_applications")
public class ScholarshipApplication {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "scholarship_id", nullable = false)
    private Long scholarshipId;

    @Column(name = "scholarship_title", nullable = false, length = 255)
    private String scholarshipTitle;

    @Column(name = "scholarship_provider", length = 255)
    private String scholarshipProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50)
    private ApplicationStatus currentStatus = ApplicationStatus.PLANNING;

    @Column(name = "external_portal_reference", length = 255)
    private String externalPortalReference;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDate submissionDeadline;

    @Column(name = "actual_submission_date")
    private LocalDate actualSubmissionDate;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "award_amount_received", precision = 12, scale = 2)
    private BigDecimal awardAmountReceived;

    @Column(name = "progress_percentage", nullable = false)
    private int progressPercentage = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ScholarshipApplication() {
    }

    public ScholarshipApplication(
            UUID id,
            UUID studentId,
            Long scholarshipId,
            String scholarshipTitle,
            String scholarshipProvider,
            ApplicationStatus currentStatus,
            String externalPortalReference,
            LocalDate submissionDeadline,
            LocalDate actualSubmissionDate,
            Instant submittedAt,
            BigDecimal awardAmountReceived,
            int progressPercentage,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.studentId = studentId;
        this.scholarshipId = scholarshipId;
        this.scholarshipTitle = scholarshipTitle;
        this.scholarshipProvider = scholarshipProvider;
        this.currentStatus = currentStatus != null ? currentStatus : ApplicationStatus.PLANNING;
        this.externalPortalReference = externalPortalReference;
        this.submissionDeadline = submissionDeadline;
        this.actualSubmissionDate = actualSubmissionDate;
        this.submittedAt = submittedAt;
        this.awardAmountReceived = awardAmountReceived;
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public Long getScholarshipId() {
        return scholarshipId;
    }

    public void setScholarshipId(Long scholarshipId) {
        this.scholarshipId = scholarshipId;
    }

    public String getScholarshipTitle() {
        return scholarshipTitle;
    }

    public void setScholarshipTitle(String scholarshipTitle) {
        this.scholarshipTitle = scholarshipTitle;
    }

    public String getScholarshipProvider() {
        return scholarshipProvider;
    }

    public void setScholarshipProvider(String scholarshipProvider) {
        this.scholarshipProvider = scholarshipProvider;
    }

    public ApplicationStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ApplicationStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getExternalPortalReference() {
        return externalPortalReference;
    }

    public void setExternalPortalReference(String externalPortalReference) {
        this.externalPortalReference = externalPortalReference;
    }

    public LocalDate getSubmissionDeadline() {
        return submissionDeadline;
    }

    public void setSubmissionDeadline(LocalDate submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    public LocalDate getActualSubmissionDate() {
        return actualSubmissionDate;
    }

    public void setActualSubmissionDate(LocalDate actualSubmissionDate) {
        this.actualSubmissionDate = actualSubmissionDate;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public BigDecimal getAwardAmountReceived() {
        return awardAmountReceived;
    }

    public void setAwardAmountReceived(BigDecimal awardAmountReceived) {
        this.awardAmountReceived = awardAmountReceived;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
