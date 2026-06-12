package com.studentmanagement.analytics.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ETL 워터마크 — sourceTable별 마지막 적재 시점을 기록한다.
 * 증분 ETL은 이 시각 이후 updatedAt을 가진 운영 row만 조회·적재한다.
 */
@Entity
@Table(name = "etl_checkpoint")
@Getter @Setter @NoArgsConstructor
public class EtlCheckpoint {

    @Id
    @Column(name = "source_table", length = 64)
    private String sourceTable;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    public EtlCheckpoint(String sourceTable, LocalDateTime lastSyncedAt) {
        this.sourceTable = sourceTable;
        this.lastSyncedAt = lastSyncedAt;
    }
}
