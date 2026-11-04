package org.prestiq.scholarship.service;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional
public class ScholarshipServiceImpl implements ScholarshipService {

    private static final Logger log = LoggerFactory.getLogger(ScholarshipServiceImpl.class);
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
        Specification<Scholarship> spec = (root, cq, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            predicates.add(cb.equal(root.get("publicationStatus"), PublicationStatus.PUBLISHED));
            predicates.add(cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED));

            if (!includeExpired) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("applicationDeadline"), LocalDate.now()));
            }

            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                var titlePred = cb.like(cb.lower(root.get("title")), searchPattern);
                var providerPred = cb.like(cb.lower(root.get("providerName")), searchPattern);
                var descPred = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titlePred, providerPred, descPred));
            }

            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }

            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("awardAmountMin"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("awardAmountMax"), maxAmount));
            }

            if (country != null && !country.isBlank()) {
                predicates.add(cb.like(root.get("eligibleCountries"), "%\"" + country.trim() + "\"%"));
            }

            if (educationLevel != null && !educationLevel.isBlank() && !"ALL".equalsIgnoreCase(educationLevel)) {
                predicates.add(cb.like(root.get("educationLevels"), "%\"" + educationLevel.trim() + "\"%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = parseSort(sort, page, size);
        Page<Scholarship> paged = scholarshipRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(paged.map(ScholarshipSummaryResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ScholarshipResponse getPublishedScholarshipByIdOrSlug(String idOrSlug) {
        Scholarship scholarship = null;
        try {
            Long id = Long.parseLong(idOrSlug);
            scholarship = scholarshipRepository.findByIdAndVerificationStatusAndPublicationStatus(
                    id, VerificationStatus.VERIFIED, PublicationStatus.PUBLISHED
            ).orElse(null);
        } catch (NumberFormatException ignored) {
            // treat as slug
        }

        if (scholarship == null) {
            scholarship = scholarshipRepository.findBySlugAndVerificationStatusAndPublicationStatus(
                    idOrSlug, VerificationStatus.VERIFIED, PublicationStatus.PUBLISHED
            ).orElseThrow(() -> new ScholarshipNotFoundException("Published scholarship not found for identifier: " + idOrSlug));
        }

        return ScholarshipResponse.fromEntity(scholarship);
    }

    @Override
    public ScholarshipResponse createDraft(ScholarshipCreateRequest request, UserPrincipal principal) {
        String slug = request.slug();
        if (slug == null || slug.isBlank()) {
            slug = request.title().toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
        }

        if (scholarshipRepository.existsBySlug(slug)) {
            throw new DuplicateSlugException("Scholarship slug already exists: " + slug);
        }

        Scholarship s = new Scholarship();
        s.setSlug(slug);
        s.setTitle(request.title());
        s.setProviderName(request.providerName());
        s.setDescription(request.description());
        s.setOfficialWebsiteUrl(request.officialWebsiteUrl());
        s.setApplicationUrl(request.applicationUrl());
        s.setSourceType(request.sourceType());
        s.setAwardAmountMin(request.awardAmountMin());
        s.setAwardAmountMax(request.awardAmountMax());
        s.setCurrencyCode(request.currencyCode() != null ? request.currencyCode() : "USD");
        s.setAwardStructure(request.awardStructure() != null ? request.awardStructure() : org.prestiq.scholarship.domain.entity.AwardStructure.ONE_TIME);
        s.setRenewable(Boolean.TRUE.equals(request.isRenewable()));
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
        s.setFirstGenOnly(Boolean.TRUE.equals(request.firstGenOnly()));
        s.setVeteranOnly(Boolean.TRUE.equals(request.veteranOnly()));
        s.setDisabilityOnly(Boolean.TRUE.equals(request.disabilityOnly()));
        s.setEligibilityCriteriaSchema(request.eligibilityCriteriaSchema());
        s.setSourceName(request.sourceName());
        s.setVerificationStatus(VerificationStatus.DRAFT);
        s.setPublicationStatus(PublicationStatus.DRAFT);

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Created draft scholarship ID: {}, slug: {}", saved.getId(), saved.getSlug());
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse updateScholarship(Long id, ScholarshipUpdateRequest request) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));

        if (request.title() != null) s.setTitle(request.title());
        if (request.providerName() != null) s.setProviderName(request.providerName());
        if (request.description() != null) s.setDescription(request.description());
        if (request.officialWebsiteUrl() != null) s.setOfficialWebsiteUrl(request.officialWebsiteUrl());
        if (request.applicationUrl() != null) s.setApplicationUrl(request.applicationUrl());
        if (request.sourceType() != null) s.setSourceType(request.sourceType());
        if (request.awardAmountMin() != null) s.setAwardAmountMin(request.awardAmountMin());
        if (request.awardAmountMax() != null) s.setAwardAmountMax(request.awardAmountMax());
        if (request.currencyCode() != null) s.setCurrencyCode(request.currencyCode());
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

        Scholarship saved = scholarshipRepository.save(s);
        log.info("Updated scholarship ID: {}", saved.getId());
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse submitForReview(Long id) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
        s.setVerificationStatus(VerificationStatus.PENDING_REVIEW);
        Scholarship saved = scholarshipRepository.save(s);
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse verifyScholarship(Long id, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
        s.setVerificationStatus(VerificationStatus.VERIFIED);
        s.setPublicationStatus(PublicationStatus.PUBLISHED);
        s.setVerifiedAt(Instant.now());
        s.setPublishedAt(Instant.now());
        if (principal != null) {
            s.setVerifiedByUserId(principal.getId());
        }
        Scholarship saved = scholarshipRepository.save(s);
        log.info("Verified and published scholarship ID: {}", saved.getId());
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse rejectScholarship(Long id, ScholarshipRejectRequest request, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
        s.setVerificationStatus(VerificationStatus.REJECTED);
        s.setPublicationStatus(PublicationStatus.DRAFT);
        s.setRejectionReason(request.rejectionReason());
        if (principal != null) {
            s.setVerifiedByUserId(principal.getId());
        }
        Scholarship saved = scholarshipRepository.save(s);
        log.info("Rejected scholarship ID: {} with reason: {}", saved.getId(), request.rejectionReason());
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse publishScholarship(Long id, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
        s.setPublicationStatus(PublicationStatus.PUBLISHED);
        s.setPublishedAt(Instant.now());
        Scholarship saved = scholarshipRepository.save(s);
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    public ScholarshipResponse archiveScholarship(Long id, ScholarshipArchiveRequest request, UserPrincipal principal) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
        s.setPublicationStatus(PublicationStatus.ARCHIVED);
        s.setArchivedAt(Instant.now());
        if (request != null) {
            s.setArchivedReason(request.archivedReason());
        }
        Scholarship saved = scholarshipRepository.save(s);
        return ScholarshipResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScholarshipResponse getScholarshipByIdAdmin(Long id) {
        Scholarship s = scholarshipRepository.findById(id)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found: " + id));
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
        Specification<Scholarship> spec = (root, cq, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }

            if (publicationStatus != null) {
                predicates.add(cb.equal(root.get("publicationStatus"), publicationStatus));
            }

            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }

            if (query != null && !query.isBlank()) {
                String searchPattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                var titlePred = cb.like(cb.lower(root.get("title")), searchPattern);
                var providerPred = cb.like(cb.lower(root.get("providerName")), searchPattern);
                predicates.add(cb.or(titlePred, providerPred));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = parseSort(sort, page, size);
        Page<Scholarship> paged = scholarshipRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(paged.map(ScholarshipSummaryResponse::fromEntity));
    }

    private Pageable parseSort(String sort, int page, int size) {
        Sort s = Sort.by(Sort.Direction.ASC, "applicationDeadline");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;

            switch (field) {
                case "amount", "awardAmountMax" -> s = Sort.by(dir, "awardAmountMax");
                case "title" -> s = Sort.by(dir, "title");
                case "createdAt" -> s = Sort.by(dir, "createdAt");
                case "provider", "providerName" -> s = Sort.by(dir, "providerName");
                default -> s = Sort.by(dir, "applicationDeadline");
            }
        }
        return PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), s);
    }
}

