package com.studentmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// OpenPDF — 와일드카드 대신 명시적 임포트 (com.lowagie.text.List, Cell 이름 충돌 방지)
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.studentmanagement.domain.Feedback;
import com.studentmanagement.domain.Grade;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.StudentRecord;
import com.studentmanagement.dto.report.ReportPreviewResponse;
import com.studentmanagement.dto.report.ReportPreviewResponse.ColumnDef;
import com.studentmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Apache POI — 와일드카드 대신 명시적 임포트 (org.apache.poi.ss.usermodel.Cell 충돌 방지)
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 보고서 생성 서비스
 * - 미리보기: 데이터 집계 후 JSON 반환
 * - Excel: Apache POI 사용 (한글 완벽 지원)
 * - PDF: OpenPDF 사용 (Docker 환경의 NanumGothic 폰트 사용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentRepository       studentRepository;
    private final GradeRepository         gradeRepository;
    private final FeedbackRepository      feedbackRepository;
    private final CounselingRepository    counselingRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final ObjectMapper            objectMapper;

    // 보고서 타입별 한국어 이름
    private static final Map<String, String> TYPE_LABELS = Map.of(
        "grade-summary",    "성적 종합 보고서",
        "student-record",   "학생부 보고서",
        "feedback-report",  "피드백 현황 보고서",
        "counseling-report","상담 이력 보고서"
    );

    // ─────────────────────────────────────────────
    // 미리보기 (JSON)
    // ─────────────────────────────────────────────

    public ReportPreviewResponse getPreview(String type, Integer grade, Integer classNum,
                                             Integer year, Integer semester) {
        List<Student> students = studentRepository.findByFilters(grade, classNum, null);
        List<ColumnDef> columns = getColumns(type);
        List<Map<String, Object>> rows = buildRows(type, students, year, semester);

        return ReportPreviewResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalCount(rows.size())
                .generatedAt(LocalDate.now().toString())
                .build();
    }

    // ─────────────────────────────────────────────
    // Excel 다운로드
    // ─────────────────────────────────────────────

    public byte[] generateExcel(String type, Integer grade, Integer classNum,
                                 Integer year, Integer semester) {
        List<Student> students = studentRepository.findByFilters(grade, classNum, null);
        List<ColumnDef> columns = getColumns(type);
        List<Map<String, Object>> rows = buildRows(type, students, year, semester);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(TYPE_LABELS.getOrDefault(type, "보고서"));

            // 제목 스타일
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 데이터 스타일 (가운데/오른쪽)
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle rightStyle = workbook.createCellStyle();
            rightStyle.setAlignment(HorizontalAlignment.RIGHT);

            int rowIdx = 0;

            // 제목 행
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(24);
            Cell titleCell = titleRow.createCell(0);
            String condStr = buildConditionString(grade, classNum, year, semester);
            titleCell.setCellValue(TYPE_LABELS.getOrDefault(type, "보고서") + " — " + condStr);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.size() - 1));

            rowIdx++; // 빈 행

            // 헤더 행
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).getLabel());
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행
            for (Map<String, Object> row : rows) {
                Row dataRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.size(); i++) {
                    ColumnDef col = columns.get(i);
                    Cell cell = dataRow.createCell(i);
                    Object val = row.get(col.getKey());
                    setCellValue(cell, val);

                    if ("right".equals(col.getAlign())) cell.setCellStyle(rightStyle);
                    else if ("center".equals(col.getAlign())) cell.setCellStyle(centerStyle);
                }
            }

            // 열 너비 자동 조정
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 10000));
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }

    // ─────────────────────────────────────────────
    // PDF 다운로드
    // ─────────────────────────────────────────────

    public byte[] generatePdf(String type, Integer grade, Integer classNum,
                               Integer year, Integer semester) {
        List<Student> students = studentRepository.findByFilters(grade, classNum, null);
        List<ColumnDef> columns = getColumns(type);
        List<Map<String, Object>> rows = buildRows(type, students, year, semester);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 가로 A4
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bf = loadKoreanFont();

            Font titleFont  = new Font(bf, 14, Font.BOLD);
            Font headerFont = new Font(bf, 9,  Font.BOLD);
            Font bodyFont   = new Font(bf, 8);
            Font subFont    = new Font(bf, 9);

            // 제목
            String condStr = buildConditionString(grade, classNum, year, semester);
            Paragraph title = new Paragraph(TYPE_LABELS.getOrDefault(type, "보고서"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph cond = new Paragraph(condStr + " | 생성일: " + LocalDate.now(), subFont);
            cond.setAlignment(Element.ALIGN_CENTER);
            cond.setSpacingAfter(12);
            document.add(cond);

            // 테이블
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);

            // 컬럼 상대 너비 계산
            float[] widths = new float[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                String key = columns.get(i).getKey();
                widths[i] = key.equals("name") || key.equals("content") || key.equals("notes") ? 2.5f : 1f;
            }
            table.setWidths(widths);

            // 헤더
            for (ColumnDef col : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(col.getLabel(), headerFont));
                cell.setBackgroundColor(new Color(220, 220, 220));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // 데이터
            for (int r = 0; r < rows.size(); r++) {
                Map<String, Object> row = rows.get(r);
                Color bg = (r % 2 == 1) ? new Color(248, 248, 248) : Color.WHITE;
                for (ColumnDef col : columns) {
                    Object val = row.get(col.getKey());
                    String text = val == null ? "-" : val.toString();
                    PdfPCell cell = new PdfPCell(new Phrase(text, bodyFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(4);
                    if ("right".equals(col.getAlign())) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    } else if ("center".equals(col.getAlign())) {
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    }
                    table.addCell(cell);
                }
            }

            document.add(table);

            // 하단 요약
            Paragraph footer = new Paragraph("총 " + rows.size() + "명", subFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(8);
            document.add(footer);

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    // ─────────────────────────────────────────────
    // 내부 헬퍼: 컬럼 정의
    // ─────────────────────────────────────────────

    private List<ColumnDef> getColumns(String type) {
        return switch (type) {
            case "grade-summary" -> List.of(
                new ColumnDef("name",      "이름",   "left"),
                new ColumnDef("grade",     "학년",   "center"),
                new ColumnDef("classNum",  "반",     "center"),
                new ColumnDef("studentNum","번호",   "center"),
                new ColumnDef("avg",       "평균점수","right"),
                new ColumnDef("gradeRank", "등급",   "center"),
                new ColumnDef("total",     "총점",   "right")
            );
            case "student-record" -> List.of(
                new ColumnDef("name",            "이름",   "left"),
                new ColumnDef("grade",           "학년",   "center"),
                new ColumnDef("classNum",        "반",     "center"),
                new ColumnDef("present",         "출석",   "center"),
                new ColumnDef("absent",          "결석",   "center"),
                new ColumnDef("late",            "지각",   "center"),
                new ColumnDef("attendanceRate",  "출석률", "center"),
                new ColumnDef("specialNotes",    "특이사항","left")
            );
            case "feedback-report" -> List.of(
                new ColumnDef("name",       "이름",   "left"),
                new ColumnDef("grade",      "학년",   "center"),
                new ColumnDef("classNum",   "반",     "center"),
                new ColumnDef("total",      "전체",   "center"),
                new ColumnDef("publicCount","공개",   "center"),
                new ColumnDef("gradeCount", "성적",   "center"),
                new ColumnDef("behavior",   "행동",   "center"),
                new ColumnDef("attendance", "출결",   "center"),
                new ColumnDef("attitude",   "태도",   "center"),
                new ColumnDef("other",      "기타",   "center")
            );
            case "counseling-report" -> List.of(
                new ColumnDef("name",     "이름",       "left"),
                new ColumnDef("grade",    "학년",       "center"),
                new ColumnDef("classNum", "반",         "center"),
                new ColumnDef("count",    "상담 횟수",  "center"),
                new ColumnDef("lastDate", "최근 상담일","center"),
                new ColumnDef("content",  "최근 내용",  "left")
            );
            default -> List.of();
        };
    }

    // ─────────────────────────────────────────────
    // 내부 헬퍼: 타입별 행 데이터 생성
    // ─────────────────────────────────────────────

    private List<Map<String, Object>> buildRows(String type, List<Student> students,
                                                 Integer year, Integer semester) {
        return switch (type) {
            case "grade-summary"    -> buildGradeSummary(students, year, semester);
            case "student-record"   -> buildStudentRecord(students);
            case "feedback-report"  -> buildFeedbackReport(students);
            case "counseling-report"-> buildCounselingReport(students);
            default -> List.of();
        };
    }

    private List<Map<String, Object>> buildGradeSummary(List<Student> students,
                                                          Integer year, Integer semester) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int s = (semester != null) ? semester : 1;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Student st : students) {
            List<Grade> grades = gradeRepository.findByStudentAndFilters(st.getId(), y, s, null);
            double avg = grades.stream().mapToDouble(g -> g.getScore().doubleValue()).average().orElse(0.0);
            double total = grades.stream().mapToDouble(g -> g.getScore().doubleValue()).sum();
            String rank = avg >= 90 ? "A" : avg >= 80 ? "B" : avg >= 70 ? "C" : avg >= 60 ? "D" : "F";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",       st.getName());
            row.put("grade",      st.getGrade() + "학년");
            row.put("classNum",   st.getClassNum() + "반");
            row.put("studentNum", st.getStudentNum() + "번");
            row.put("avg",        grades.isEmpty() ? "-" : String.format("%.1f", avg));
            row.put("gradeRank",  grades.isEmpty() ? "-" : rank);
            row.put("total",      grades.isEmpty() ? "-" : String.format("%.1f", total));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildStudentRecord(List<Student> students) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student st : students) {
            Optional<StudentRecord> recOpt = studentRecordRepository.findByStudentId(st.getId());
            int present = 0, absent = 0, late = 0;
            String notes = "-";

            if (recOpt.isPresent()) {
                StudentRecord rec = recOpt.get();
                notes = rec.getSpecialNotes() != null ? rec.getSpecialNotes() : "-";
                try {
                    Map<String, Integer> att = objectMapper.readValue(
                            rec.getAttendance(), new TypeReference<>() {});
                    present = att.getOrDefault("present", 0);
                    absent  = att.getOrDefault("absent", 0);
                    late    = att.getOrDefault("late", 0);
                } catch (Exception ignored) {}
            }

            int total = present + absent + late;
            String rate = total > 0 ? String.format("%.1f%%", present * 100.0 / total) : "-";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",           st.getName());
            row.put("grade",          st.getGrade() + "학년");
            row.put("classNum",       st.getClassNum() + "반");
            row.put("present",        present);
            row.put("absent",         absent);
            row.put("late",           late);
            row.put("attendanceRate", rate);
            row.put("specialNotes",   notes);
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildFeedbackReport(List<Student> students) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student st : students) {
            List<Feedback> feedbacks = feedbackRepository.findByStudentIdOrderByCreatedAtDesc(st.getId());

            long publicCount  = feedbacks.stream().filter(Feedback::isPublic).count();
            long gradeCount   = feedbacks.stream().filter(f -> f.getCategory().name().equals("GRADE")).count();
            long behaviorCount= feedbacks.stream().filter(f -> f.getCategory().name().equals("BEHAVIOR")).count();
            long attCount     = feedbacks.stream().filter(f -> f.getCategory().name().equals("ATTENDANCE")).count();
            long attitudeCount= feedbacks.stream().filter(f -> f.getCategory().name().equals("ATTITUDE")).count();
            long otherCount   = feedbacks.stream().filter(f -> f.getCategory().name().equals("OTHER")).count();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",       st.getName());
            row.put("grade",      st.getGrade() + "학년");
            row.put("classNum",   st.getClassNum() + "반");
            row.put("total",      feedbacks.size());
            row.put("publicCount",publicCount);
            row.put("gradeCount", gradeCount);
            row.put("behavior",   behaviorCount);
            row.put("attendance", attCount);
            row.put("attitude",   attitudeCount);
            row.put("other",      otherCount);
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildCounselingReport(List<Student> students) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Student st : students) {
            var counselings = counselingRepository.findByFilters(st.getId(), null, null, null);

            String lastDate    = counselings.isEmpty() ? "-" : counselings.get(0).getDate().toString();
            String lastContent = counselings.isEmpty() ? "-"
                    : truncate(counselings.get(0).getContent(), 40);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name",     st.getName());
            row.put("grade",    st.getGrade() + "학년");
            row.put("classNum", st.getClassNum() + "반");
            row.put("count",    counselings.size());
            row.put("lastDate", lastDate);
            row.put("content",  lastContent);
            result.add(row);
        }
        return result;
    }

    // ─────────────────────────────────────────────
    // 유틸 메서드
    // ─────────────────────────────────────────────

    /** 한글 폰트 로드 (Docker 환경: NanumGothic, 로컬: Helvetica fallback) */
    private BaseFont loadKoreanFont() {
        String[] fontPaths = {
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum-gothic/NanumGothic.ttf",
        };
        for (String path : fontPaths) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }
        log.warn("한글 폰트를 찾을 수 없습니다. PDF에서 한글이 깨질 수 있습니다. (Docker 환경에서 실행하면 정상 출력됩니다)");
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
        } catch (Exception e) {
            throw new RuntimeException("폰트 로드 실패", e);
        }
    }

    /** POI 셀에 타입 자동 감지하여 값 설정 */
    private void setCellValue(Cell cell, Object val) {
        if (val == null) {
            cell.setCellValue("-");
        } else if (val instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(val.toString());
        }
    }

    /** 조건 문자열 생성 */
    private String buildConditionString(Integer grade, Integer classNum, Integer year, Integer semester) {
        List<String> parts = new ArrayList<>();
        if (year     != null) parts.add(year + "년");
        if (semester != null) parts.add(semester + "학기");
        if (grade    != null) parts.add(grade + "학년");
        if (classNum != null) parts.add(classNum + "반");
        return parts.isEmpty() ? "전체" : String.join(" ", parts);
    }

    private String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
