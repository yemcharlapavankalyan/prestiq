package org.prestiq.scholarship.repository;

import org.prestiq.scholarship.domain.entity.ScholarshipSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarshipSourceRepository extends JpaRepository<ScholarshipSource, Long> {

    List<ScholarshipSource> findByScholarshipId(Long scholarshipId);
}
