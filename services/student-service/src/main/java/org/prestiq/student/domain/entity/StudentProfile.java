package org.prestiq.student.domain.entity;

import jakarta.persistence.*;
import org.prestiq.student.domain.converter.JsonMapConverter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "citizenship_country", nullable = false, length = 2)
    private String citizenshipCountry;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", nullable = false, length = 50)
    private EducationLevel educationLevel;

    @Column(name = "institution_name", length = 255)
    private String institutionName;

    @Column(name = "major_field_of_study", length = 150)
    private String majorFieldOfStudy;

    @Column(name = "current_year_or_semester", length = 50)
    private String currentYearOrSemester;

    @Column(name = "cumulative_gpa", precision = 4, scale = 2)
    private BigDecimal cumulativeGpa;

    @Column(name = "gpa_scale", nullable = false, precision = 4, scale = 2)
    private BigDecimal gpaScale = new BigDecimal("4.00");

    @Column(name = "annual_family_income", precision = 14, scale = 2)
    private BigDecimal annualFamilyIncome;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "first_gen_student", nullable = false)
    private boolean firstGenStudent = false;

    @Column(name = "military_veteran", nullable = false)
    private boolean militaryVeteran = false;

    @Column(name = "disability_status", nullable = false)
    private boolean disabilityStatus = false;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "additional_attributes", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> additionalAttributes = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StudentProfile() {
    }

    public StudentProfile(UUID userId, String firstName, String lastName, String citizenshipCountry, EducationLevel educationLevel) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.citizenshipCountry = citizenshipCountry;
        this.educationLevel = educationLevel;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.gpaScale == null) {
            this.gpaScale = new BigDecimal("4.00");
        }
        if (this.currencyCode == null) {
            this.currencyCode = "USD";
        }
        if (this.additionalAttributes == null) {
            this.additionalAttributes = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        if (firstName == null) return lastName != null ? lastName : "";
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCitizenshipCountry() {
        return citizenshipCountry;
    }

    public void setCitizenshipCountry(String citizenshipCountry) {
        this.citizenshipCountry = citizenshipCountry;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public EducationLevel getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(EducationLevel educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getMajorFieldOfStudy() {
        return majorFieldOfStudy;
    }

    public void setMajorFieldOfStudy(String majorFieldOfStudy) {
        this.majorFieldOfStudy = majorFieldOfStudy;
    }

    public String getCurrentYearOrSemester() {
        return currentYearOrSemester;
    }

    public void setCurrentYearOrSemester(String currentYearOrSemester) {
        this.currentYearOrSemester = currentYearOrSemester;
    }

    public BigDecimal getCumulativeGpa() {
        return cumulativeGpa;
    }

    public void setCumulativeGpa(BigDecimal cumulativeGpa) {
        this.cumulativeGpa = cumulativeGpa;
    }

    public BigDecimal getGpaScale() {
        return gpaScale;
    }

    public void setGpaScale(BigDecimal gpaScale) {
        this.gpaScale = gpaScale;
    }

    public BigDecimal getAnnualFamilyIncome() {
        return annualFamilyIncome;
    }

    public void setAnnualFamilyIncome(BigDecimal annualFamilyIncome) {
        this.annualFamilyIncome = annualFamilyIncome;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isFirstGenStudent() {
        return firstGenStudent;
    }

    public void setFirstGenStudent(boolean firstGenStudent) {
        this.firstGenStudent = firstGenStudent;
    }

    public boolean isMilitaryVeteran() {
        return militaryVeteran;
    }

    public void setMilitaryVeteran(boolean militaryVeteran) {
        this.militaryVeteran = militaryVeteran;
    }

    public boolean isDisabilityStatus() {
        return disabilityStatus;
    }

    public void setDisabilityStatus(boolean disabilityStatus) {
        this.disabilityStatus = disabilityStatus;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(Map<String, Object> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
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

