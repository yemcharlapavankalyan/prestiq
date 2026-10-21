package org.prestiq.eligibility.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.prestiq.eligibility.domain.converter.CriteriaBreakdownConverter;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "eligibility_evaluations")
public class EligibilityEvaluation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "scholarship_id", nullable = false)
    private Long scholarshipId;

    @Column(name = "rule_set_version", nullable = false)
    private int ruleSetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private Verdict verdict;

    @Column(name = "match_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchPercentage;

    @Convert(converter = CriteriaBreakdownConverter.class)
    @Column(name = "criteria_breakdown", nullable = false, columnDefinition = "TEXT")
    private List<CriterionResultResponse> criteriaBreakdown = new ArrayList<>();

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt = Instant.now();

    public EligibilityEvaluation() {
    }

    public EligibilityEvaluation(
            UUID id,
            UUID studentId,
            Long scholarshipId,
            int ruleSetVersion,
            Verdict verdict,
            BigDecimal matchPercentage,
            List<CriterionResultResponse> criteriaBreakdown,
            Instant evaluatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.studentId = studentId;
        this.scholarshipId = scholarshipId;
        this.ruleSetVersion = ruleSetVersion;
        this.verdict = verdict;
        this.matchPercentage = matchPercentage;
        this.criteriaBreakdown = criteriaBreakdown != null ? criteriaBreakdown : new ArrayList<>();
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
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

    public int getRuleSetVersion() {
        return ruleSetVersion;
    }

    public void setRuleSetVersion(int ruleSetVersion) {
        this.ruleSetVersion = ruleSetVersion;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public BigDecimal getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(BigDecimal matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public List<CriterionResultResponse> getCriteriaBreakdown() {
        return criteriaBreakdown;
    }

    public void setCriteriaBreakdown(List<CriterionResultResponse> criteriaBreakdown) {
        this.criteriaBreakdown = criteriaBreakdown;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
