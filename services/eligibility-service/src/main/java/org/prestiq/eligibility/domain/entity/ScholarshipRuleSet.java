package org.prestiq.eligibility.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "scholarship_rule_sets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rule_version", columnNames = {"scholarship_id", "version"})
        }
)
public class ScholarshipRuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scholarship_id", nullable = false)
    private Long scholarshipId;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RuleSetStatus status = RuleSetStatus.ACTIVE;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<EligibilityCriterion> criteria = new ArrayList<>();

    public ScholarshipRuleSet() {
    }

    public ScholarshipRuleSet(Long scholarshipId, int version, RuleSetStatus status, UUID createdByUserId) {
        this.scholarshipId = scholarshipId;
        this.version = version;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    public void addCriterion(EligibilityCriterion criterion) {
        criteria.add(criterion);
        criterion.setRuleSet(this);
    }

    public void removeCriterion(EligibilityCriterion criterion) {
        criteria.remove(criterion);
        criterion.setRuleSet(null);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScholarshipId() {
        return scholarshipId;
    }

    public void setScholarshipId(Long scholarshipId) {
        this.scholarshipId = scholarshipId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public RuleSetStatus getStatus() {
        return status;
    }

    public void setStatus(RuleSetStatus status) {
        this.status = status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<EligibilityCriterion> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<EligibilityCriterion> criteria) {
        this.criteria = criteria;
    }
}
