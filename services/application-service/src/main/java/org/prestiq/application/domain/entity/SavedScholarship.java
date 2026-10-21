package org.prestiq.application.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "saved_scholarships",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_student_saved_scholarship", columnNames = {"student_id", "scholarship_id"})
        }
)
public class SavedScholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "scholarship_id", nullable = false)
    private Long scholarshipId;

    @Column(name = "scholarship_title", length = 255)
    private String scholarshipTitle;

    @Column(name = "scholarship_provider", length = 255)
    private String scholarshipProvider;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "award_amount_max", precision = 12, scale = 2)
    private BigDecimal awardAmountMax;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SavedScholarship() {
    }

    public SavedScholarship(
            UUID studentId,
            Long scholarshipId,
            String scholarshipTitle,
            String scholarshipProvider,
            LocalDate applicationDeadline,
            BigDecimal awardAmountMax,
            String notes
    ) {
        this.studentId = studentId;
        this.scholarshipId = scholarshipId;
        this.scholarshipTitle = scholarshipTitle;
        this.scholarshipProvider = scholarshipProvider;
        this.applicationDeadline = applicationDeadline;
        this.awardAmountMax = awardAmountMax;
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public BigDecimal getAwardAmountMax() {
        return awardAmountMax;
    }

    public void setAwardAmountMax(BigDecimal awardAmountMax) {
        this.awardAmountMax = awardAmountMax;
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
}
