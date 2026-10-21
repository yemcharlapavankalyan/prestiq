package org.prestiq.student.service;

import org.prestiq.student.dto.request.StudentProfileRequest;
import org.prestiq.student.dto.response.StudentProfileResponse;
import org.prestiq.student.security.UserPrincipal;

import java.util.UUID;

public interface StudentProfileService {

    StudentProfileResponse getProfileByUserId(UUID userId);

    StudentProfileResponse createProfile(UUID userId, StudentProfileRequest request);

    StudentProfileResponse updateProfile(UUID userId, StudentProfileRequest request);

    StudentProfileResponse upsertProfile(UUID userId, StudentProfileRequest request);

    StudentProfileResponse getProfileById(UUID profileId, UserPrincipal principal);
}
