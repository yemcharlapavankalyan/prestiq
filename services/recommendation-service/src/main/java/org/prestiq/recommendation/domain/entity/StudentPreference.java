package org.prestiq.recommendation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.prestiq.recommendation.domain.converter.JsonListConverter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "student_preferences")
public class StudentPreference {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, unique = true)
    private UUID studentId;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "preferred_fields", nullable = false, columnDefinition = "TEXT")
    private List<String> preferredFields = new ArrayList<>();

    @Convert(converter = JsonListConverter.class)
    @Column(name = "preferred_locations", nullable = false, columnDefinition = "TEXT")
    private List<String> preferredLocations = new ArrayList<>();

    @Column(name = "min_award_preference", precision = 12, scale = 2)
    private BigDecimal minAwardPreference;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public StudentPreference() {
    }

    public StudentPreference(
            UUID id,
            UUID studentId,
            List<String> preferredFields,
            List<String> preferredLocations,
            BigDecimal minAwardPreference,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.studentId = studentId;
        this.preferredFields = preferredFields != null ? preferredFields : new ArrayList<>();
        this.preferredLocations = preferredLocations != null ? preferredLocations : new ArrayList<>();
        this.minAwardPreference = minAwardPreference;
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

    public List<String> getPreferredFields() {
        return preferredFields;
    }

    public void setPreferredFields(List<String> preferredFields) {
        this.preferredFields = preferredFields;
    }

    public List<String> getPreferredLocations() {
        return preferredLocations;
    }

    public void setPreferredLocations(List<String> preferredLocations) {
        this.preferredLocations = preferredLocations;
    }

    public BigDecimal getMinAwardPreference() {
        return minAwardPreference;
    }

    public void setMinAwardPreference(BigDecimal minAwardPreference) {
        this.minAwardPreference = minAwardPreference;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
