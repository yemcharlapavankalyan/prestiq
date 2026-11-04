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
import java.util.HashMap;
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
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found for user ID: " + userId));
        return StudentProfileResponse.fromEntity(profile);
    }

    @Override
    public StudentProfileResponse createProfile(UUID userId, StudentProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("Student profile already exists for user ID: " + userId);
        }

        validateAcademicData(request);

        StudentProfile profile = new StudentProfile(
                userId,
                request.firstName(),
                request.lastName(),
                request.citizenshipCountry(),
                request.educationLevel()
        );
        mapRequestToEntity(request, profile);

        StudentProfile saved = profileRepository.save(profile);
        log.info("Created student profile ID: {} for user ID: {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    public StudentProfileResponse updateProfile(UUID userId, StudentProfileRequest request) {
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found for user ID: " + userId));

        validateAcademicData(request);
        mapRequestToEntity(request, profile);

        StudentProfile saved = profileRepository.save(profile);
        log.info("Updated student profile ID: {} for user ID: {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    public StudentProfileResponse upsertProfile(UUID userId, StudentProfileRequest request) {
        validateAcademicData(request);

        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new StudentProfile(
                        userId,
                        request.firstName(),
                        request.lastName(),
                        request.citizenshipCountry(),
                        request.educationLevel()
                ));

        mapRequestToEntity(request, profile);
        StudentProfile saved = profileRepository.save(profile);
        log.info("Upserted student profile ID: {} for user ID: {}", saved.getId(), userId);
        return StudentProfileResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileById(UUID profileId, UserPrincipal principal) {
        StudentProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Student profile not found for ID: " + profileId));

        if (principal != null && !principal.isInternal() && !principal.hasRole("ADMIN") && !profile.getUserId().equals(principal.getId())) {
            throw new AccessDeniedException("Unauthorized to access student profile: " + profileId);
        }

        return StudentProfileResponse.fromEntity(profile);
    }

    private void validateAcademicData(StudentProfileRequest request) {
        if (request.cumulativeGpa() != null && request.gpaScale() != null) {
            if (request.cumulativeGpa().compareTo(request.gpaScale()) > 0) {
                throw new InvalidAcademicDataException("Cumulative GPA (" + request.cumulativeGpa()
                        + ") cannot exceed GPA scale (" + request.gpaScale() + ")");
            }
        }
    }

    private void mapRequestToEntity(StudentProfileRequest request, StudentProfile profile) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        if (request.dateOfBirth() != null) {
            profile.setDateOfBirth(request.dateOfBirth());
        }
        if (request.citizenshipCountry() != null) {
            profile.setCitizenshipCountry(request.citizenshipCountry());
        }
        if (request.stateProvince() != null) {
            profile.setStateProvince(request.stateProvince());
        }
        if (request.educationLevel() != null) {
            profile.setEducationLevel(request.educationLevel());
        }
        profile.setInstitutionName(request.institutionName());
        profile.setMajorFieldOfStudy(request.majorFieldOfStudy());
        profile.setCurrentYearOrSemester(request.currentYearOrSemester());
        profile.setCumulativeGpa(request.cumulativeGpa());

        if (request.gpaScale() != null) {
            profile.setGpaScale(request.gpaScale());
        } else if (profile.getGpaScale() == null) {
            profile.setGpaScale(new BigDecimal("4.00"));
        }

        profile.setAnnualFamilyIncome(request.annualFamilyIncome());

        if (request.currencyCode() != null) {
            profile.setCurrencyCode(request.currencyCode());
        } else if (profile.getCurrencyCode() == null) {
            profile.setCurrencyCode("USD");
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
            profile.setAdditionalAttributes(new HashMap<>(request.additionalAttributes()));
        }
    }
}

