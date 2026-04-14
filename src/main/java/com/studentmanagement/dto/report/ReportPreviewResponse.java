package com.studentmanagement.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 보고서 미리보기 응답 DTO
 * columns: 동적 컬럼 정의 (프론트엔드에서 테이블 헤더로 사용)
 * rows: 각 행의 데이터 (key = column.key)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportPreviewResponse {

    private List<ColumnDef> columns;
    private List<Map<String, Object>> rows;
    private int totalCount;
    private String generatedAt;

    @Data
    @AllArgsConstructor
    public static class ColumnDef {
        private String key;
        private String label;
        private String align; // "left" | "right" | "center"
    }
}
