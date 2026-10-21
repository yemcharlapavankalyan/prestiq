package org.prestiq.student.service;

import org.prestiq.student.domain.entity.StudentProfile;
import org.prestiq.student.dto.request.StudentProfileRequest;
import org.prestiq.student.dto.response.StudentProfileResponse;
import org.prestiq.student.exception.InvalidAcademicDataException;
import org.prestiq.student.exception.ProfileAlreadyExistsException;
import org.prestiq.student.exception.ProfileNotFoundException;
import org.prestiq.student.repository.StudentProfileRepository;
import org.prestiq.student.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileServiceImpl.class);

    private final StudentProfileRepository profileRepository;

    public StudentProfileServiceImpl(StudentProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileByUserId(UUID userId) {
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found for user: " + userId));
        return StudentProfileResponse.fromEntity(profile);
    }

    @Override
    public StudentProfileResponse createProfile(UUID userId, StudentProfileRequest request) {
        validateAcademicData(request);

        if (profileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("A student profile already exists for user: " + userId);
        }

        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        mapRequestToEntity(request, profile);

        StudentProfile saved = profileRepository.save(profile);
        log.info("Created new student profile {} for user {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    public StudentProfileResponse updateProfile(UUID userId, StudentProfileRequest request) {
        validateAcademicData(request);

        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found for user: " + userId));

        mapRequestToEntity(request, profile);
        StudentProfile saved = profileRepository.save(profile);
        log.info("Updated student profile {} for user {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    public StudentProfileResponse upsertProfile(UUID userId, StudentProfileRequest request) {
        validateAcademicData(request);

        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    StudentProfile newProfile = new StudentProfile();
                    newProfile.setId(UUID.randomUUID());
                    newProfile.setUserId(userId);
                    return newProfile;
                });

        mapRequestToEntity(request, profile);
        StudentProfile saved = profileRepository.save(profile);
        log.info("Upserted student profile {} for user {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileById(UUID profileId, UserPrincipal principal) {
        StudentProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found with ID: " + profileId));

        // Security check: Only the owning student or authorized administrative/internal roles can access this profile
        boolean isAdmin = principal != null && (principal.hasRole("ADMIN") || principal.isInternal());
        boolean isOwner = principal != null && profile.getUserId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Unauthorized access attempt: User {} attempted to access profile {} belonging to user {}",
                    principal != null ? principal.getId() : "anonymous", profileId, profile.getUserId());
            throw new AccessDeniedException("Access denied: You do not have permission to view another student's profile.");
        }

        return StudentProfileResponse.fromEntity(profile);
    }

    private void validateAcademicData(StudentProfileRequest request) {
        BigDecimal scale = request.gpaScale() != null ? request.gpaScale() : new BigDecimal("4.00");
        if (scale.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAcademicDataException("GPA scale must be strictly greater than 0.00");
        }

        if (request.cumulativeGpa() != null) {
            if (request.cumulativeGpa().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidAcademicDataException("Cumulative GPA cannot be negative");
            }
            if (request.cumulativeGpa().compareTo(scale) > 0) {
                throw new InvalidAcademicDataException(
                        String.format("Cumulative GPA (%s) cannot exceed the grading scale (%s)",
                                request.cumulativeGpa(), scale)
                );
            }
        }

        if (request.annualFamilyIncome() != null && request.annualFamilyIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAcademicDataException("Annual family income cannot be negative");
        }
    }

    private void mapRequestToEntity(StudentProfileRequest request, StudentProfile profile) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setCitizenshipCountry(request.citizenshipCountry());
        profile.setStateProvince(request.stateProvince());
        profile.setEducationLevel(request.educationLevel());
        profile.setInstitutionName(request.institutionName());
        profile.setMajorFieldOfStudy(request.majorFieldOfStudy());
        profile.setCurrentYearOrSemester(request.currentYearOrSemester());
        profile.setCumulativeGpa(request.cumulativeGpa());

        if (request.gpaScale() != null) {
            profile.setGpaScale(request.gpaScale());
        }
        profile.setAnnualFamilyIncome(request.annualFamilyIncome());

        if (request.currencyCode() != null) {
            profile.setCurrencyCode(request.currencyCode());
        }
        if (request.firstGenStudent() != null) {
            profile.setFirstGenStudent(request.firstGenStudent());
        }
        if (request.militaryVeteran() != null) {
            profile.setMilitaryVeteran(request.militaryVeteran());
        }
        if (request.disabilityStatus() != null) {
            profile.setDisabilityStatus(request.disabilityStatus());
        }
        if (request.additionalAttributes() != null) {
            profile.setAdditionalAttributes(request.additionalAttributes());
        }
    }
}
