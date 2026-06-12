package com.studentmanagement.analytics.service;

import com.studentmanagement.analytics.domain.DimStudent;
import com.studentmanagement.analytics.domain.DimSubject;
import com.studentmanagement.analytics.domain.FactAttendance;
import com.studentmanagement.analytics.repository.*;
import com.studentmanagement.analytics.repository.projection.CategoryCount;
import com.studentmanagement.analytics.repository.projection.TermAverage;
import com.studentmanagement.dto.analytics.AnalyticsOverviewResponse;
import com.studentmanagement.dto.analytics.StudentSummaryResponse;
import com.studentmanagement.dto.analytics.SubjectDistributionResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 학습 현황 집계 서비스 (US-08-03/04/05).
 *
 * 분석 DB(student_analytics)의 스타 스키마만 읽어 학생별·과목별 지표를 집계한다.
 * 모든 읽기는 analyticsTransactionManager(read-only)로 수행한다.
 */
@Service
@Transactional(value = "analyticsTransactionManager", readOnly = true)
public class AnalyticsService {

    /** 제출로 간주하는 상태 (제출률 분자) */
    private static final Set<String> SUBMITTED_STATUSES = Set.of("SUBMITTED", "LATE");

    private final DimStudentRepository dimStudentRepository;
    private final DimSubjectRepository dimSubjectRepository;
    private final FactGradeRepository factGradeRepository;
    private final FactAttendanceRepository factAttendanceRepository;
    private final FactSubmissionRepository factSubmissionRepository;
    private final FactFeedbackRepository factFeedbackRepository;

    public AnalyticsService(DimStudentRepository dimStudentRepository,
                            DimSubjectRepository dimSubjectRepository,
                            FactGradeRepository factGradeRepository,
                            FactAttendanceRepository factAttendanceRepository,
                            FactSubmissionRepository factSubmissionRepository,
                            FactFeedbackRepository factFeedbackRepository) {
        this.dimStudentRepository = dimStudentRepository;
        this.dimSubjectRepository = dimSubjectRepository;
        this.factGradeRepository = factGradeRepository;
        this.factAttendanceRepository = factAttendanceRepository;
        this.factSubmissionRepository = factSubmissionRepository;
        this.factFeedbackRepository = factFeedbackRepository;
    }

    /** 대시보드 진입용 개요 — 적재된 학생·과목 목록 */
    public AnalyticsOverviewResponse getOverview() {
        List<AnalyticsOverviewResponse.StudentRef> students = dimStudentRepository.findAll().stream()
                .sorted((a, b) -> {
                    int c = Integer.compare(a.getGrade(), b.getGrade());
                    if (c != 0) return c;
                    c = Integer.compare(a.getClassNum(), b.getClassNum());
                    return c != 0 ? c : Integer.compare(a.getStudentNum(), b.getStudentNum());
                })
                .map(s -> new AnalyticsOverviewResponse.StudentRef(
                        s.getStudentId(), s.getName(), s.getGrade(), s.getClassNum()))
                .toList();

        List<AnalyticsOverviewResponse.SubjectRef> subjects = dimSubjectRepository.findAll().stream()
                .map(s -> new AnalyticsOverviewResponse.SubjectRef(s.getSubjectId(), s.getName()))
                .toList();

        return new AnalyticsOverviewResponse(students, subjects);
    }

    /** 학생별 학습 현황 집계 */
    public StudentSummaryResponse getStudentSummary(Long studentId) {
        DimStudent student = dimStudentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("분석 데이터에 학생이 없습니다. ETL을 먼저 실행하세요."));

        List<StudentSummaryResponse.TermPoint> trend = factGradeRepository.findStudentTermAverages(studentId)
                .stream()
                .map(this::toTermPoint)
                .toList();

        FactAttendance att = factAttendanceRepository.findById(studentId).orElse(null);
        StudentSummaryResponse.Attendance attendance = att == null
                ? new StudentSummaryResponse.Attendance(0, 0, 0, 0.0)
                : new StudentSummaryResponse.Attendance(att.getPresent(), att.getAbsent(), att.getLate(),
                        round(att.attendanceRate()));

        long total = factSubmissionRepository.countByStudentId(studentId);
        long submitted = factSubmissionRepository.countByStudentIdAndStatusIn(studentId, SUBMITTED_STATUSES);
        StudentSummaryResponse.SubmissionRate submission =
                new StudentSummaryResponse.SubmissionRate(total, submitted, rate(submitted, total));

        List<StudentSummaryResponse.CategoryCount> feedback = factFeedbackRepository.findCategoryCounts(studentId)
                .stream()
                .map(c -> new StudentSummaryResponse.CategoryCount(c.getCategory(), c.getCnt()))
                .toList();

        // 과목별 평균 점수 (어느 과목이 강하고 약한지 — 맞춤 학습 조언용)
        Map<Long, String> subjectNames = dimSubjectRepository.findAll().stream()
                .collect(Collectors.toMap(DimSubject::getSubjectId, DimSubject::getName));
        List<StudentSummaryResponse.SubjectScore> subjectScores =
                factGradeRepository.findStudentSubjectAverages(studentId).stream()
                        .map(sa -> new StudentSummaryResponse.SubjectScore(
                                sa.getSubjectId(),
                                subjectNames.getOrDefault(sa.getSubjectId(), "?"),
                                sa.getAvgScore() == null ? 0.0 : round(sa.getAvgScore())))
                        .sorted((a, b) -> Double.compare(a.avgScore(), b.avgScore()))
                        .toList();

        return new StudentSummaryResponse(student.getStudentId(), student.getName(),
                trend, subjectScores, attendance, submission, feedback);
    }

    /** 과목별 학습 현황 집계 (평균·분포·제출률) */
    public SubjectDistributionResponse getSubjectDistribution(Long subjectId) {
        DimSubject subject = dimSubjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("분석 데이터에 과목이 없습니다. ETL을 먼저 실행하세요."));

        Double avg = factGradeRepository.findSubjectAverage(subjectId);
        double average = avg == null ? 0.0 : round(avg);

        List<SubjectDistributionResponse.Bucket> distribution =
                bucketize(factGradeRepository.findScoresBySubjectId(subjectId));

        long total = factSubmissionRepository.countBySubjectId(subjectId);
        long submitted = factSubmissionRepository.countBySubjectIdAndStatusIn(subjectId, SUBMITTED_STATUSES);
        SubjectDistributionResponse.SubmissionRate submission =
                new SubjectDistributionResponse.SubmissionRate(total, submitted, rate(submitted, total));

        return new SubjectDistributionResponse(subject.getSubjectId(), subject.getName(),
                average, distribution, submission);
    }

    // ───────────────────────── helpers ─────────────────────────

    private StudentSummaryResponse.TermPoint toTermPoint(TermAverage t) {
        double avg = t.getAvgScore() == null ? 0.0 : round(t.getAvgScore());
        return new StudentSummaryResponse.TermPoint(t.getYear(), t.getSemester(), avg);
    }

    /** 점수 목록을 5개 구간(0-59/60-69/70-79/80-89/90-100)으로 집계 */
    private List<SubjectDistributionResponse.Bucket> bucketize(List<BigDecimal> scores) {
        long b0 = 0, b60 = 0, b70 = 0, b80 = 0, b90 = 0;
        for (BigDecimal s : scores) {
            if (s == null) continue;
            double v = s.doubleValue();
            if (v < 60) b0++;
            else if (v < 70) b60++;
            else if (v < 80) b70++;
            else if (v < 90) b80++;
            else b90++;
        }
        return List.of(
                new SubjectDistributionResponse.Bucket("0-59", b0),
                new SubjectDistributionResponse.Bucket("60-69", b60),
                new SubjectDistributionResponse.Bucket("70-79", b70),
                new SubjectDistributionResponse.Bucket("80-89", b80),
                new SubjectDistributionResponse.Bucket("90-100", b90)
        );
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : round((double) numerator / denominator);
    }

    /** 소수 4자리 반올림 */
    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
