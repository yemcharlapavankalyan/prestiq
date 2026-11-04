package org.prestiq.scholarship.domain.entity;

import jakarta.persistence.*;
import org.prestiq.scholarship.domain.converter.JsonListConverter;
import org.prestiq.scholarship.domain.converter.StringListConverter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scholarships")
public class Scholarship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "provider_name", nullable = false, length = 255)
    private String providerName;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "official_website_url", nullable = false, length = 1000)
    private String officialWebsiteUrl;

    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Column(name = "award_amount_min", precision = 12, scale = 2)
    private BigDecimal awardAmountMin;

    @Column(name = "award_amount_max", precision = 12, scale = 2)
    private BigDecimal awardAmountMax;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "award_structure", nullable = false, length = 50)
    private AwardStructure awardStructure = AwardStructure.ONE_TIME;

    @Column(name = "is_renewable", nullable = false)
    private boolean isRenewable = false;

    @Column(name = "application_start_date")
    private LocalDate applicationStartDate;

    @Column(name = "application_deadline", nullable = false)
    private LocalDate applicationDeadline;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Convert(converter = StringListConverter.class)
    @Column(name = "eligible_countries", nullable = false, columnDefinition = "TEXT")
    private List<String> eligibleCountries = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "eligible_states", nullable = false, columnDefinition = "TEXT")
    private List<String> eligibleStates = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "education_levels", nullable = false, columnDefinition = "TEXT")
    private List<String> educationLevels = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "eligible_institutions", nullable = false, columnDefinition = "TEXT")
    private List<String> eligibleInstitutions = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "eligible_fields_of_study", nullable = false, columnDefinition = "TEXT")
    private List<String> eligibleFieldsOfStudy = new ArrayList<>();

    @Column(name = "min_gpa", precision = 4, scale = 2)
    private BigDecimal minGpa;

    @Column(name = "gpa_scale", nullable = false, precision = 4, scale = 2)
    private BigDecimal gpaScale = new BigDecimal("4.00");

    @Column(name = "max_family_income", precision = 14, scale = 2)
    private BigDecimal maxFamilyIncome;

    @Column(name = "first_gen_only", nullable = false)
    private boolean firstGenOnly = false;

    @Column(name = "veteran_only", nullable = false)
    private boolean veteranOnly = false;

    @Column(name = "disability_only", nullable = false)
    private boolean disabilityOnly = false;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "eligibility_criteria_schema", nullable = false, columnDefinition = "TEXT")
    private List<Map<String, Object>> eligibilityCriteriaSchema = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.DRAFT;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 50)
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_reason", columnDefinition = "TEXT")
    private String archivedReason;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @OneToMany(mappedBy = "scholarship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScholarshipSource> sources = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Scholarship() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (currencyCode == null) {
            currencyCode = "USD";
        }
        if (gpaScale == null) {
            gpaScale = new BigDecimal("4.00");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return applicationDeadline != null && applicationDeadline.isBefore(LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOfficialWebsiteUrl() {
        return officialWebsiteUrl;
    }

    public void setOfficialWebsiteUrl(String officialWebsiteUrl) {
        this.officialWebsiteUrl = officialWebsiteUrl;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public BigDecimal getAwardAmountMin() {
        return awardAmountMin;
    }

    public void setAwardAmountMin(BigDecimal awardAmountMin) {
        this.awardAmountMin = awardAmountMin;
    }

    public BigDecimal getAwardAmountMax() {
        return awardAmountMax;
    }

    public void setAwardAmountMax(BigDecimal awardAmountMax) {
        this.awardAmountMax = awardAmountMax;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public AwardStructure getAwardStructure() {
        return awardStructure;
    }

    public void setAwardStructure(AwardStructure awardStructure) {
        this.awardStructure = awardStructure;
    }

    public boolean isRenewable() {
        return isRenewable;
    }

    public void setRenewable(boolean renewable) {
        isRenewable = renewable;
    }

    public LocalDate getApplicationStartDate() {
        return applicationStartDate;
    }

    public void setApplicationStartDate(LocalDate applicationStartDate) {
        this.applicationStartDate = applicationStartDate;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public List<String> getEligibleCountries() {
        return eligibleCountries;
    }

    public void setEligibleCountries(List<String> eligibleCountries) {
        this.eligibleCountries = eligibleCountries != null ? eligibleCountries : new ArrayList<>();
    }

    public List<String> getEligibleStates() {
        return eligibleStates;
    }

    public void setEligibleStates(List<String> eligibleStates) {
        this.eligibleStates = eligibleStates != null ? eligibleStates : new ArrayList<>();
    }

    public List<String> getEducationLevels() {
        return educationLevels;
    }

    public void setEducationLevels(List<String> educationLevels) {
        this.educationLevels = educationLevels != null ? educationLevels : new ArrayList<>();
    }

    public List<String> getEligibleInstitutions() {
        return eligibleInstitutions;
    }

    public void setEligibleInstitutions(List<String> eligibleInstitutions) {
        this.eligibleInstitutions = eligibleInstitutions != null ? eligibleInstitutions : new ArrayList<>();
    }

    public List<String> getEligibleFieldsOfStudy() {
        return eligibleFieldsOfStudy;
    }

    public void setEligibleFieldsOfStudy(List<String> eligibleFieldsOfStudy) {
        this.eligibleFieldsOfStudy = eligibleFieldsOfStudy != null ? eligibleFieldsOfStudy : new ArrayList<>();
    }

    public BigDecimal getMinGpa() {
        return minGpa;
    }

    public void setMinGpa(BigDecimal minGpa) {
        this.minGpa = minGpa;
    }

    public BigDecimal getGpaScale() {
        return gpaScale;
    }

    public void setGpaScale(BigDecimal gpaScale) {
        this.gpaScale = gpaScale;
    }

    public BigDecimal getMaxFamilyIncome() {
        return maxFamilyIncome;
    }

    public void setMaxFamilyIncome(BigDecimal maxFamilyIncome) {
        this.maxFamilyIncome = maxFamilyIncome;
    }

    public boolean isFirstGenOnly() {
        return firstGenOnly;
    }

    public void setFirstGenOnly(boolean firstGenOnly) {
        this.firstGenOnly = firstGenOnly;
    }

    public boolean isVeteranOnly() {
        return veteranOnly;
    }

    public void setVeteranOnly(boolean veteranOnly) {
        this.veteranOnly = veteranOnly;
    }

    public boolean isDisabilityOnly() {
        return disabilityOnly;
    }

    public void setDisabilityOnly(boolean disabilityOnly) {
        this.disabilityOnly = disabilityOnly;
    }

    public List<Map<String, Object>> getEligibilityCriteriaSchema() {
        return eligibilityCriteriaSchema;
    }

    public void setEligibilityCriteriaSchema(List<Map<String, Object>> eligibilityCriteriaSchema) {
        this.eligibilityCriteriaSchema = eligibilityCriteriaSchema != null ? eligibilityCriteriaSchema : new ArrayList<>();
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UUID getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(UUID verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public PublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(PublicationStatus publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivedReason() {
        return archivedReason;
    }

    public void setArchivedReason(String archivedReason) {
        this.archivedReason = archivedReason;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public List<ScholarshipSource> getSources() {
        return sources;
    }

    public void setSources(List<ScholarshipSource> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
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

