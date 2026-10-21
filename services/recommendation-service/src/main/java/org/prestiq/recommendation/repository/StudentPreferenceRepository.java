package org.prestiq.recommendation.repository;

import org.prestiq.recommendation.domain.entity.StudentPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentPreferenceRepository extends JpaRepository<StudentPreference, UUID> {

    Optional<StudentPreference> findByStudentId(UUID studentId);
}
