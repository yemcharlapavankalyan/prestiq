package org.prestiq.eligibility.service;

import org.prestiq.eligibility.dto.request.CreateRuleSetRequest;
import org.prestiq.eligibility.dto.response.RuleSetResponse;

import java.util.List;
import java.util.UUID;

public interface RuleSetAdminService {

    RuleSetResponse createRuleSet(CreateRuleSetRequest request, UUID adminUserId);

    RuleSetResponse activateRuleSet(Long ruleSetId);

    RuleSetResponse getRuleSetById(Long ruleSetId);

    List<RuleSetResponse> getRuleSetsByScholarshipId(Long scholarshipId);

    RuleSetResponse getActiveRuleSetForScholarship(Long scholarshipId);
}
