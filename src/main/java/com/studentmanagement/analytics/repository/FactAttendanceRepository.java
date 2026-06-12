package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.FactAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactAttendanceRepository extends JpaRepository<FactAttendance, Long> {
}
