package org.prestiq.eligibility.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "eligibility_criteria")
public class EligibilityCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private ScholarshipRuleSet ruleSet;

    @Column(name = "criterion_code", nullable = false, length = 50)
    private String criterionCode;

    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 30)
    private CriterionOperator operator;

    @Column(name = "target_value", nullable = false, columnDefinition = "TEXT")
    private String targetValue;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory = true;

    @Column(name = "weight", nullable = false)
    private int weight = 0;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "pass_explanation_template", length = 500)
    private String passExplanationTemplate;

    @Column(name = "fail_explanation_template", length = 500)
    private String failExplanationTemplate;

    public EligibilityCriterion() {
    }

    public EligibilityCriterion(
            String criterionCode,
            String targetField,
            CriterionOperator operator,
            String targetValue,
            boolean isMandatory,
            int weight,
            String displayName,
            String passExplanationTemplate,
            String failExplanationTemplate
    ) {
        this.criterionCode = criterionCode;
        this.targetField = targetField;
        this.operator = operator;
        this.targetValue = targetValue;
        this.isMandatory = isMandatory;
        this.weight = weight;
        this.displayName = displayName;
        this.passExplanationTemplate = passExplanationTemplate;
        this.failExplanationTemplate = failExplanationTemplate;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScholarshipRuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(ScholarshipRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public String getCriterionCode() {
        return criterionCode;
    }

    public void setCriterionCode(String criterionCode) {
        this.criterionCode = criterionCode;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public CriterionOperator getOperator() {
        return operator;
    }

    public void setOperator(CriterionOperator operator) {
        this.operator = operator;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassExplanationTemplate() {
        return passExplanationTemplate;
    }

    public void setPassExplanationTemplate(String passExplanationTemplate) {
        this.passExplanationTemplate = passExplanationTemplate;
    }

    public String getFailExplanationTemplate() {
        return failExplanationTemplate;
    }

    public void setFailExplanationTemplate(String failExplanationTemplate) {
        this.failExplanationTemplate = failExplanationTemplate;
    }
}
