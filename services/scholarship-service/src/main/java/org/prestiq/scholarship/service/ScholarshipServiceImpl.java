package org.prestiq.scholarship.service;

import jakarta.persistence.criteria.Predicate;
import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.Scholarship;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;
import org.prestiq.scholarship.dto.request.ScholarshipArchiveRequest;
import org.prestiq.scholarship.dto.request.ScholarshipCreateRequest;
import org.prestiq.scholarship.dto.request.ScholarshipRejectRequest;
import org.prestiq.scholarship.dto.request.ScholarshipUpdateRequest;
import org.prestiq.scholarship.dto.response.PagedResponse;
import org.prestiq.scholarship.dto.response.ScholarshipResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSummaryResponse;
import org.prestiq.scholarship.exception.DuplicateSlugException;
import org.prestiq.scholarship.exception.IllegalLifecycleTransitionException;
import org.prestiq.scholarship.exception.InvalidScholarshipDataException;
import org.prestiq.scholarship.exception.InvalidSortException;
import org.prestiq.scholarship.exception.ScholarshipNotFoundException;
import org.prestiq.scholarship.repository.ScholarshipRepository;
import org.prestiq.scholarship.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ScholarshipServiceImpl implements ScholarshipService {

    private static final Logger log = LoggerFactory.getLogger(ScholarshipServiceImpl.class);

    private static final Map<String, String> SORT_ALLOWLIST = Map.of(
            "deadline", "applicationDeadline",
            "applicationdeadline", "applicationDeadline",
            "amount", "awardAmountMax",
            "awardamount", "awardAmountMax",
            "title", "title",
            "createdat", "createdAt",
            "created_at", "createdAt",
            "provider", "providerName",
            "providername", "providerName"
    );

    private final ScholarshipRepository scholarshipRepository;

    public ScholarshipServiceImpl(ScholarshipRepository scholarshipRepository) {
        this.scholarshipRepository = scholarshipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ScholarshipSummaryResponse> searchPublishedScholarships(
            String query,
            String educationLevel,
            String country,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            SourceType sourceType,
            boolean includeExpired,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = createPageable(page, size, sort, "deadline", Sort.Direction.ASC);

        Specification<Scholarship> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule: Public listing ONLY exposes PUBLISHED and VERIFIED records
            predicates.add(cb.equal(root.get("publicationStatus"), PublicationStatus.PUBLISHED));
            predicates.add(cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));

            // Deadline filtering: by default, exclude past deadlines
            if (!includeExpired) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("applicationDeadline"), LocalDate.now()));
            }

            // Keyword query: searches title, provider name, and description
            if (StringUtils.hasText(query)) {
                String term = "%" + query.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), term);
                Predicate providerPred = cb.like(cb.lower(root.get("providerName")), term);
                Predicate descPred = cb.like(cb.lower(root.get("description")), term);
                predicates.add(cb.or(titlePred, providerPred, descPred));
            }

            // Education level filter
            if (StringUtils.hasText(educationLevel)) {
                String levelTerm = "%" + educationLevel.trim().toUpperCase() + "%";
                predicates.add(cb.like(root.get("educationLevels"), levelTerm));
            }

            // Country restriction filter
            if (StringUtils.hasText(country)) {
                String countryTerm = "%" + country.trim().toUpperCase() + "%";
                predicates.add(cb.like(root.get("eligibleCountries"), countryTerm));
            }

            // Award amount filters
            if (minAmount != null) {
                Predicate maxAmountGte = cb.greaterThanOrEqualTo(root.get("awardAmountMax"), minAmount);
                Predicate minAmountGte = cb.and(
                        cb.isNull(root.get("awardAmountMax")),
                        cb.greaterThanOrEqualTo(root.get("awardAmountMin"), minAmount)
                );
                predicates.add(cb.or(maxAmountGte, minAmountGte));
            }

            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("awardAmountMin"), maxAmount));
            }

            // Source type filter
            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScholarshipSummaryResponse> resultPage = scholarshipRepository.findAll(spec, pageable)
                .map(ScholarshipSummaryResponse::fromEntity);

        return PagedResponse.fromPage(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ScholarshipResponse getPublishedScholarshipByIdOrSlug(String idOrSlug) {
        Scholarship scholarship = null;

        // Try numeric ID first
        if (idOrSlug.matches("^\\d+$")) {
            Long id = Long.parseLong(idOrSlug);
            scholarship = scholarshipRepository.findByIdAndVerificationStatusAndPublicationStatus(
                    id, VerificationStatus.VERIFIED, PublicationStatus.PUBLISHED
            ).orElse(null);
        }

        // Try slug if not found by numeric ID
        if (scholarship == null) {
            scholarship = scholarshipRepository.findBySlugAndVerificationStatusAndPublicationStatus(
                    idOrSlug, VerificationStatus.VERIFIED, PublicationStatus.PUBLISHED
            ).orElseThrow(() -> new ScholarshipNotFoundException(
                    "Published scholarship not found with identifier: " + idOrSlug
            ));
        }

        return ScholarshipResponse.fromEntity(scholarship);
    }

    @Override
    public ScholarshipResponse createDraft(ScholarshipCreateRequest request, UserPrincipal principal) {
        validateScholarshipData(
                request.awardAmountMin(),
                request.awardAmountMax(),
                request.applicationStartDate(),
                request.applicationDeadline(),
                request.minGpa(),
                request.gpaScale()
        );

        String slug = resolveSlug(request.slug(), request.title(), null);

        Scholarship s = new Scholarship();
        s.setTitle(request.title().trim());
        s.setSlug(slug);
        s.setProviderName(request.providerName().trim());
        s.setDescription(request.description().trim());
        s.setOfficialWebsiteUrl(request.officialWebsiteUrl().trim());
        s.setApplicationUrl(StringUtils.hasText(request.applicationUrl()) ? request.applicationUrl().trim() : null);
        s.setSourceType(request.sourceType());
        s.setAwardAmountMin(request.awardAmountMin());
        s.setAwardAmountMax(request.awardAmountMax());
        s.setCurrencyCode(StringUtils.hasText(request.currencyCode()) ? request.currencyCode().trim() : "USD");
        s.setAwardStructure(request.awardStructure() != null ? request.awardStructure() : org.prestiq.scholarship.domain.entity.AwardStructure.ONE_TIME);
        s.setRenewable(request.isRenewable() != null ? request.isRenewable() : false);
        s.setApplicationStartDate(request.applicationStartDate());
        s.setApplicationDeadline(request.applicationDeadline());
        s.setAcademicYear(request.academicYear());
        s.setEligibleCountries(request.eligibleCountries());
        s.setEligibleStates(request.eligibleStates());
        s.setEducationLevels(request.educationLevels());
        s.setEligibleInstitutions(request.eligibleInstitutions());
        s.setEligibleFieldsOfStudy(request.eligibleFieldsOfStudy());
        s.setMinGpa(request.minGpa());
        s.setGpaScale(request.gpaScale() != null ? request.gpaScale() : new BigDecimal("4.00"));
        s.setMaxFamilyIncome(request.maxFamilyIncome());
        s.setFirstGenOnly(request.firstGenOnly() != null ? request.firstGenOnly() : false);
        s.setVeteranOnly(request.veteranOnly() != null ? request.veteranOnly() : false);
        s.setDisabilityOnly(request.disabilityOnly() != null ? request.disabilityOnly() : false);
        s.setEligibilityCriteriaSchema(request.eligibilityCriteriaSchema());
        s.setSourceName(request.sourceName());

        // Initial lifecycle state: strictly DRAFT
        s.setVerificationStatus(VerificationStatus.DRAFT);
        s.setPublicationStatus(PublicationStatus.DRAFT);

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Created draft scholarship ID {} (slug: {}) by user {}", saved.getId(), saved.getSlug(),
                principal != null ? principal.getName() : "internal");
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse updateScholarship(Long id, ScholarshipUpdateRequest request) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        LocalDate startDate = request.applicationStartDate() != null ? request.applicationStartDate() : s.getApplicationStartDate();
        LocalDate deadline = request.applicationDeadline() != null ? request.applicationDeadline() : s.getApplicationDeadline();
        BigDecimal minAmt = request.awardAmountMin() != null ? request.awardAmountMin() : s.getAwardAmountMin();
        BigDecimal maxAmt = request.awardAmountMax() != null ? request.awardAmountMax() : s.getAwardAmountMax();
        BigDecimal minGpa = request.minGpa() != null ? request.minGpa() : s.getMinGpa();
        BigDecimal scale = request.gpaScale() != null ? request.gpaScale() : s.getGpaScale();

        validateScholarshipData(minAmt, maxAmt, startDate, deadline, minGpa, scale);

        if (StringUtils.hasText(request.title())) s.setTitle(request.title().trim());
        if (StringUtils.hasText(request.slug()) && !request.slug().equals(s.getSlug())) {
            s.setSlug(resolveSlug(request.slug(), s.getTitle(), id));
        }
        if (StringUtils.hasText(request.providerName())) s.setProviderName(request.providerName().trim());
        if (StringUtils.hasText(request.description())) s.setDescription(request.description().trim());
        if (StringUtils.hasText(request.officialWebsiteUrl())) s.setOfficialWebsiteUrl(request.officialWebsiteUrl().trim());
        if (request.applicationUrl() != null) s.setApplicationUrl(request.applicationUrl().trim());
        if (request.sourceType() != null) s.setSourceType(request.sourceType());
        if (request.awardAmountMin() != null) s.setAwardAmountMin(request.awardAmountMin());
        if (request.awardAmountMax() != null) s.setAwardAmountMax(request.awardAmountMax());
        if (StringUtils.hasText(request.currencyCode())) s.setCurrencyCode(request.currencyCode().trim());
        if (request.awardStructure() != null) s.setAwardStructure(request.awardStructure());
        if (request.isRenewable() != null) s.setRenewable(request.isRenewable());
        if (request.applicationStartDate() != null) s.setApplicationStartDate(request.applicationStartDate());
        if (request.applicationDeadline() != null) s.setApplicationDeadline(request.applicationDeadline());
        if (request.academicYear() != null) s.setAcademicYear(request.academicYear());
        if (request.eligibleCountries() != null) s.setEligibleCountries(request.eligibleCountries());
        if (request.eligibleStates() != null) s.setEligibleStates(request.eligibleStates());
        if (request.educationLevels() != null) s.setEducationLevels(request.educationLevels());
        if (request.eligibleInstitutions() != null) s.setEligibleInstitutions(request.eligibleInstitutions());
        if (request.eligibleFieldsOfStudy() != null) s.setEligibleFieldsOfStudy(request.eligibleFieldsOfStudy());
        if (request.minGpa() != null) s.setMinGpa(request.minGpa());
        if (request.gpaScale() != null) s.setGpaScale(request.gpaScale());
        if (request.maxFamilyIncome() != null) s.setMaxFamilyIncome(request.maxFamilyIncome());
        if (request.firstGenOnly() != null) s.setFirstGenOnly(request.firstGenOnly());
        if (request.veteranOnly() != null) s.setVeteranOnly(request.veteranOnly());
        if (request.disabilityOnly() != null) s.setDisabilityOnly(request.disabilityOnly());
        if (request.eligibilityCriteriaSchema() != null) s.setEligibilityCriteriaSchema(request.eligibilityCriteriaSchema());
        if (request.sourceName() != null) s.setSourceName(request.sourceName());

        Scholarship updated = scholarshipRepository.save(s);
        log.info("Updated scholarship ID {}", updated.getId());
        return ScholarshipResponse.fromEntity(updated);
    }

    @Override
    public ScholarshipResponse submitForReview(Long id) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        if (s.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new IllegalLifecycleTransitionException("Scholarship is already verified");
        }

        s.setVerificationStatus(VerificationStatus.PENDING_REVIEW);
        Scholarship saved = scholarshipRepository.save(s);
        log.info("Scholarship {} submitted for review", id);
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse verifyScholarship(Long id, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        s.setVerificationStatus(VerificationStatus.VERIFIED);
        s.setVerifiedByUserId(principal != null ? principal.getId() : null);
        Instant now = Instant.now();
        s.setVerifiedAt(now);
        s.setLastVerifiedAt(now);
        s.setRejectionReason(null);

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Scholarship {} verified by user {}", id, principal != null ? principal.getName() : "system");
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse rejectScholarship(Long id, ScholarshipRejectRequest request, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        s.setVerificationStatus(VerificationStatus.REJECTED);
        s.setRejectionReason(request.rejectionReason());

        // Unpublish if it was published
        if (s.getPublicationStatus() == PublicationStatus.PUBLISHED) {
            s.setPublicationStatus(PublicationStatus.DRAFT);
        }

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Scholarship {} rejected. Reason: {}", id, request.rejectionReason());
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse publishScholarship(Long id, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        // Rule: A scholarship MUST be VERIFIED before it can be published!
        if (s.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalLifecycleTransitionException(
                    "Cannot publish scholarship: Status must be VERIFIED before publication (current status: "
                            + s.getVerificationStatus() + ")"
            );
        }

        s.setPublicationStatus(PublicationStatus.PUBLISHED);
        s.setPublishedAt(Instant.now());
        s.setArchivedAt(null);
        s.setArchivedReason(null);

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Scholarship {} published by user {}", id, principal != null ? principal.getName() : "system");
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse archiveScholarship(Long id, ScholarshipArchiveRequest request, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));

        s.setPublicationStatus(PublicationStatus.ARCHIVED);
        s.setArchivedAt(Instant.now());
        s.setArchivedReason(request != null ? request.archivedReason() : null);

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Scholarship {} archived by user {}", id, principal != null ? principal.getName() : "system");
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScholarshipResponse getScholarshipByIdAdmin(Long id) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + id));
        return ScholarshipResponse.fromEntity(s);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ScholarshipSummaryResponse> searchAdminScholarships(
            String query,
            VerificationStatus verificationStatus,
            PublicationStatus publicationStatus,
            SourceType sourceType,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = createPageable(page, size, sort, "createdat", Sort.Direction.DESC);

        Specification<Scholarship> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }
            if (publicationStatus != null) {
                predicates.add(cb.equal(root.get("publicationStatus"), publicationStatus));
            }
            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }
            if (StringUtils.hasText(query)) {
                String term = "%" + query.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), term);
                Predicate providerPred = cb.like(cb.lower(root.get("providerName")), term);
                predicates.add(cb.or(titlePred, providerPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScholarshipSummaryResponse> resultPage = scholarshipRepository.findAll(spec, pageable)
                .map(ScholarshipSummaryResponse::fromEntity);

        return PagedResponse.fromPage(resultPage);
    }

    private Pageable createPageable(int page, int size, String sortParam, String defaultSortProperty, Sort.Direction defaultDirection) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0) ? 20 : Math.min(size, 100);

        String propertyKey = defaultSortProperty.toLowerCase();
        Sort.Direction direction = defaultDirection;

        if (StringUtils.hasText(sortParam)) {
            String[] parts = sortParam.split(",");
            String requestedField = parts[0].trim().toLowerCase();

            if (!SORT_ALLOWLIST.containsKey(requestedField)) {
                throw new InvalidSortException(
                        "Sort field '" + parts[0].trim() + "' is not permitted. Allowed sort fields are: "
                                + String.join(", ", "deadline", "amount", "title", "createdAt", "provider")
                );
            }

            propertyKey = requestedField;
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.ASC;
            }
        }

        String entityProperty = SORT_ALLOWLIST.get(propertyKey);
        return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, entityProperty));
    }

    private void validateScholarshipData(
            BigDecimal awardAmountMin,
            BigDecimal awardAmountMax,
            LocalDate startDate,
            LocalDate deadline,
            BigDecimal minGpa,
            BigDecimal gpaScale
    ) {
        if (startDate != null && deadline != null && startDate.isAfter(deadline)) {
            throw new InvalidScholarshipDataException(
                    "Application opening date (" + startDate + ") cannot be after deadline (" + deadline + ")"
            );
        }

        if (awardAmountMin != null && awardAmountMax != null && awardAmountMin.compareTo(awardAmountMax) > 0) {
            throw new InvalidScholarshipDataException(
                    "Minimum award amount (" + awardAmountMin + ") cannot exceed maximum award amount (" + awardAmountMax + ")"
            );
        }

        BigDecimal scale = (gpaScale != null) ? gpaScale : new BigDecimal("4.00");
        if (scale.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidScholarshipDataException("GPA scale must be strictly greater than 0.00");
        }

        if (minGpa != null) {
            if (minGpa.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidScholarshipDataException("Minimum GPA cannot be negative");
            }
            if (minGpa.compareTo(scale) > 0) {
                throw new InvalidScholarshipDataException(
                        "Minimum GPA (" + minGpa + ") cannot exceed GPA scale (" + scale + ")"
                );
            }
        }
    }

    private String resolveSlug(String providedSlug, String title, Long currentId) {
        String baseSlug;
        if (StringUtils.hasText(providedSlug)) {
            baseSlug = providedSlug.trim().toLowerCase();
        } else {
            baseSlug = title.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            if (baseSlug.isBlank()) {
                baseSlug = "scholarship-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }

        boolean exists = scholarshipRepository.existsBySlug(baseSlug);
        if (exists) {
            if (currentId != null) {
                Scholarship existing = scholarshipRepository.findBySlug(baseSlug).orElse(null);
                if (existing != null && existing.getId().equals(currentId)) {
                    return baseSlug;
                }
            }
            if (StringUtils.hasText(providedSlug)) {
                throw new DuplicateSlugException("A scholarship with slug '" + baseSlug + "' already exists");
            } else {
                baseSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
        }

        return baseSlug;
    }
}
