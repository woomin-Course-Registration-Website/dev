package com.studentmanagement.analytics.repository;

import com.studentmanagement.analytics.domain.EtlCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtlCheckpointRepository extends JpaRepository<EtlCheckpoint, String> {
}
