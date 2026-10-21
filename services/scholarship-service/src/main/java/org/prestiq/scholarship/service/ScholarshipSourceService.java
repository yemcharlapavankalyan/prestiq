package org.prestiq.scholarship.service;

import org.prestiq.scholarship.dto.request.ScholarshipSourceRequest;
import org.prestiq.scholarship.dto.response.ScholarshipSourceResponse;

import java.util.List;

public interface ScholarshipSourceService {

    ScholarshipSourceResponse addSource(Long scholarshipId, ScholarshipSourceRequest request);

    List<ScholarshipSourceResponse> getSourcesByScholarshipId(Long scholarshipId);

    void removeSource(Long scholarshipId, Long sourceId);
}
