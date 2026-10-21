package org.prestiq.recommendation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.prestiq.recommendation.domain.converter.ScoreBreakdownConverter;
import org.prestiq.recommendation.dto.response.ScoreBreakdownDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_student_schol_rec", columnNames = {"student_id", "scholarship_id"})
        }
)
public class StudentRecommendation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "scholarship_id", nullable = false)
    private Long scholarshipId;

    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_verdict", nullable = false, length = 30)
    private EligibilityVerdict eligibilityVerdict;

    @Column(name = "eligibility_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal eligibilityScore;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition = 0;

    @Convert(converter = ScoreBreakdownConverter.class)
    @Column(name = "score_breakdown", nullable = false, columnDefinition = "TEXT")
    private ScoreBreakdownDto scoreBreakdown;

    @Column(name = "rule_set_version", nullable = false)
    private int ruleSetVersion = 1;

    @Column(name = "is_dismissed", nullable = false)
    private boolean isDismissed = false;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    public StudentRecommendation() {
    }

    public StudentRecommendation(
            UUID id,
            UUID studentId,
            Long scholarshipId,
            BigDecimal matchScore,
            EligibilityVerdict eligibilityVerdict,
            BigDecimal eligibilityScore,
            int rankPosition,
            ScoreBreakdownDto scoreBreakdown,
            int ruleSetVersion,
            boolean isDismissed,
            Instant calculatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.studentId = studentId;
        this.scholarshipId = scholarshipId;
        this.matchScore = matchScore;
        this.eligibilityVerdict = eligibilityVerdict;
        this.eligibilityScore = eligibilityScore;
        this.rankPosition = rankPosition;
        this.scoreBreakdown = scoreBreakdown;
        this.ruleSetVersion = ruleSetVersion;
        this.isDismissed = isDismissed;
        this.calculatedAt = calculatedAt != null ? calculatedAt : Instant.now();
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

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public EligibilityVerdict getEligibilityVerdict() {
        return eligibilityVerdict;
    }

    public void setEligibilityVerdict(EligibilityVerdict eligibilityVerdict) {
        this.eligibilityVerdict = eligibilityVerdict;
    }

    public BigDecimal getEligibilityScore() {
        return eligibilityScore;
    }

    public void setEligibilityScore(BigDecimal eligibilityScore) {
        this.eligibilityScore = eligibilityScore;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    public ScoreBreakdownDto getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(ScoreBreakdownDto scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public int getRuleSetVersion() {
        return ruleSetVersion;
    }

    public void setRuleSetVersion(int ruleSetVersion) {
        this.ruleSetVersion = ruleSetVersion;
    }

    public boolean isDismissed() {
        return isDismissed;
    }

    public void setDismissed(boolean dismissed) {
        isDismissed = dismissed;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
