package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.DimSubject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimSubjectRepository extends JpaRepository<DimSubject, Long> {
}
