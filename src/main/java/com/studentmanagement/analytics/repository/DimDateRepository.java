package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.DimDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DimDateRepository extends JpaRepository<DimDate, Integer> {
}
