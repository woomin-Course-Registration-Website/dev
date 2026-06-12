package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.DimStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimStudentRepository extends JpaRepository<DimStudent, Long> {
}
