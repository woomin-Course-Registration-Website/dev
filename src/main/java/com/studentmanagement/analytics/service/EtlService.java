package com.studentmanagement.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.analytics.domain.*;
import com.studentmanagement.analytics.repository.*;
import com.studentmanagement.domain.*;
import com.studentmanagement.dto.analytics.EtlResult;
import com.studentmanagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 배치 ETL (US-08-02).
 *
 * 운영 DB(student_management)의 변경분을 분석 DB(student_analytics)의 스타 스키마로 적재한다.
 * - 차원(dim_student/dim_subject)·출결(fact_attendance)은 매 실행 전량 upsert (소량).
 * - 성적/제출/피드백 사실은 updatedAt 워터마크 기준 증분 적재.
 *
 * 개별 upsert 메서드는 Kafka CDC consumer(가점)에서도 재사용한다.
 * 운영 측 읽기는 @Primary(transactionManager), 분석 측 쓰기는 analyticsTransactionManager 를 사용한다.
 */
@Service
public class EtlService {

    private static final Logger log = LoggerFactory.getLogger(EtlService.class);
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 운영(읽기)
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final SubmissionRepository submissionRepository;

    // 분석(쓰기)
    private final DimStudentRepository dimStudentRepository;
    private final DimSubjectRepository dimSubjectRepository;
    private final DimDateRepository dimDateRepository;
    private final FactGradeRepository factGradeRepository;
    private final FactAttendanceRepository factAttendanceRepository;
    private final FactSubmissionRepository factSubmissionRepository;
    private final FactFeedbackRepository factFeedbackRepository;
    private final EtlCheckpointRepository checkpointRepository;

    public EtlService(StudentRepository studentRepository,
                      SubjectRepository subjectRepository,
                      GradeRepository gradeRepository,
                      StudentRecordRepository studentRecordRepository,
                      FeedbackRepository feedbackRepository,
                      SubmissionRepository submissionRepository,
                      DimStudentRepository dimStudentRepository,
                      DimSubjectRepository dimSubjectRepository,
                      DimDateRepository dimDateRepository,
                      FactGradeRepository factGradeRepository,
                      FactAttendanceRepository factAttendanceRepository,
                      FactSubmissionRepository factSubmissionRepository,
                      FactFeedbackRepository factFeedbackRepository,
                      EtlCheckpointRepository checkpointRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.studentRecordRepository = studentRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.submissionRepository = submissionRepository;
        this.dimStudentRepository = dimStudentRepository;
        this.dimSubjectRepository = dimSubjectRepository;
        this.dimDateRepository = dimDateRepository;
        this.factGradeRepository = factGradeRepository;
        this.factAttendanceRepository = factAttendanceRepository;
        this.factSubmissionRepository = factSubmissionRepository;
        this.factFeedbackRepository = factFeedbackRepository;
        this.checkpointRepository = checkpointRepository;
    }

    /** 스케줄러 진입점 (기본 매시 정각). */
    @Scheduled(cron = "${app.analytics.etl.cron}")
    public void scheduledEtl() {
        EtlResult result = runEtl();
        log.info("[ETL] scheduled run done: {}", result);
    }

    /**
     * ETL 1회 실행. 수동 트리거(API)와 스케줄러가 공유한다.
     * 운영 데이터를 먼저 읽어들인 뒤 분석 스키마에 upsert 한다.
     */
    public EtlResult runEtl() {
        LocalDateTime runStart = LocalDateTime.now();

        // 1) 차원: 전량 upsert (소량)
        List<Student> students = readStudents();
        students.forEach(s -> upsertDimStudent(s.getId(), s.getName(), s.getGrade(), s.getClassNum(), s.getStudentNum()));

        List<Subject> subjects = readSubjects();
        subjects.forEach(s -> upsertDimSubject(s.getId(), s.getName()));

        // 2) 출결: 전량 upsert (학생당 1행)
        List<StudentRecord> records = readRecords();
        records.forEach(this::upsertAttendanceFromRecord);

        // 3) 사실: 증분 적재 (updatedAt 워터마크)
        LocalDateTime gradeSince = watermark("grades");
        List<Grade> grades = readGradesSince(gradeSince);
        grades.forEach(g -> upsertFactGrade(g.getStudent().getId(), g.getSubject().getId(),
                g.getYear(), g.getSemester(), g.getScore(), g.getGradeRank()));

        LocalDateTime subSince = watermark("submissions");
        List<Submission> submissions = readSubmissionsSince(subSince);
        submissions.forEach(s -> upsertFactSubmission(
                s.getAssignment().getId(), s.getStudent().getId(), s.getAssignment().getSubject().getId(),
                s.getAssignment().getYear(), s.getAssignment().getSemester(),
                s.getStatus().name(), s.getAssignment().getDueDate()));

        LocalDateTime fbSince = watermark("feedbacks");
        List<Feedback> feedbacks = readFeedbackSince(fbSince);
        feedbacks.forEach(f -> upsertFactFeedback(f.getId(), f.getStudent().getId(), f.getCategory().name()));

        // 4) 워터마크 갱신
        saveCheckpoint("grades", runStart);
        saveCheckpoint("submissions", runStart);
        saveCheckpoint("feedbacks", runStart);

        EtlResult result = new EtlResult(students.size(), subjects.size(),
                grades.size(), records.size(), submissions.size(), feedbacks.size());
        log.info("[ETL] run done: {}", result);
        return result;
    }

    // ───────────────────────── 운영 측 읽기 (primary tx) ─────────────────────────

    @Transactional(readOnly = true)
    public List<Student> readStudents() { return studentRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<Subject> readSubjects() { return subjectRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<StudentRecord> readRecords() { return studentRecordRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<Grade> readGradesSince(LocalDateTime since) { return gradeRepository.findUpdatedSince(since); }

    @Transactional(readOnly = true)
    public List<Submission> readSubmissionsSince(LocalDateTime since) { return submissionRepository.findUpdatedSince(since); }

    @Transactional(readOnly = true)
    public List<Feedback> readFeedbackSince(LocalDateTime since) { return feedbackRepository.findUpdatedSince(since); }

    // ───────────────────────── 분석 측 upsert (analytics tx) — CDC 재사용 ─────────────────────────

    @Transactional("analyticsTransactionManager")
    public void upsertDimStudent(Long studentId, String name, int grade, int classNum, int studentNum) {
        DimStudent dim = dimStudentRepository.findById(studentId).orElseGet(DimStudent::new);
        dim.setStudentId(studentId);
        dim.setName(name);
        dim.setGrade(grade);
        dim.setClassNum(classNum);
        dim.setStudentNum(studentNum);
        dimStudentRepository.save(dim);
    }

    @Transactional("analyticsTransactionManager")
    public void upsertDimSubject(Long subjectId, String name) {
        DimSubject dim = dimSubjectRepository.findById(subjectId).orElseGet(DimSubject::new);
        dim.setSubjectId(subjectId);
        dim.setName(name);
        dimSubjectRepository.save(dim);
    }

    @Transactional("analyticsTransactionManager")
    public void upsertDimDate(int year, int semester) {
        Integer key = DimDate.keyOf(year, semester);
        if (!dimDateRepository.existsById(key)) {
            dimDateRepository.save(new DimDate(year, semester));
        }
    }

    @Transactional("analyticsTransactionManager")
    public void upsertFactGrade(Long studentId, Long subjectId, int year, int semester,
                                BigDecimal score, String gradeRank) {
        upsertDimDate(year, semester);
        FactGrade fact = factGradeRepository
                .findByStudentIdAndSubjectIdAndYearAndSemester(studentId, subjectId, year, semester)
                .orElseGet(FactGrade::new);
        fact.setStudentId(studentId);
        fact.setSubjectId(subjectId);
        fact.setYear(year);
        fact.setSemester(semester);
        fact.setDateKey(DimDate.keyOf(year, semester));
        fact.setScore(score);
        fact.setGradeRank(gradeRank);
        factGradeRepository.save(fact);
    }

    @Transactional("analyticsTransactionManager")
    public void upsertFactAttendance(Long studentId, int present, int absent, int late) {
        FactAttendance fact = factAttendanceRepository.findById(studentId).orElseGet(FactAttendance::new);
        fact.setStudentId(studentId);
        fact.setPresent(present);
        fact.setAbsent(absent);
        fact.setLate(late);
        factAttendanceRepository.save(fact);
    }

    @Transactional("analyticsTransactionManager")
    public void upsertFactSubmission(Long assignmentId, Long studentId, Long subjectId,
                                     int year, int semester, String status, LocalDate dueDate) {
        upsertDimDate(year, semester);
        FactSubmission fact = factSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(FactSubmission::new);
        fact.setAssignmentId(assignmentId);
        fact.setStudentId(studentId);
        fact.setSubjectId(subjectId);
        fact.setYear(year);
        fact.setSemester(semester);
        fact.setDateKey(DimDate.keyOf(year, semester));
        fact.setStatus(status);
        fact.setDueDate(dueDate);
        factSubmissionRepository.save(fact);
    }

    @Transactional("analyticsTransactionManager")
    public void upsertFactFeedback(Long feedbackId, Long studentId, String category) {
        FactFeedback fact = factFeedbackRepository.findByFeedbackId(feedbackId).orElseGet(FactFeedback::new);
        fact.setFeedbackId(feedbackId);
        fact.setStudentId(studentId);
        fact.setCategory(category);
        factFeedbackRepository.save(fact);
    }

    // ───────────────────────── 보조 ─────────────────────────

    /** student_records.attendance(JSON) → fact_attendance */
    private void upsertAttendanceFromRecord(StudentRecord record) {
        if (record.getStudent() == null) return;
        int present = 0, absent = 0, late = 0;
        try {
            if (record.getAttendance() != null && !record.getAttendance().isBlank()) {
                JsonNode node = objectMapper.readTree(record.getAttendance());
                present = node.path("present").asInt(0);
                absent = node.path("absent").asInt(0);
                late = node.path("late").asInt(0);
            }
        } catch (Exception e) {
            log.warn("[ETL] attendance JSON 파싱 실패 (studentId={}): {}",
                    record.getStudent().getId(), e.getMessage());
        }
        upsertFactAttendance(record.getStudent().getId(), present, absent, late);
    }

    @Transactional(value = "analyticsTransactionManager", readOnly = true)
    public LocalDateTime watermark(String sourceTable) {
        return checkpointRepository.findById(sourceTable)
                .map(EtlCheckpoint::getLastSyncedAt)
                .orElse(EPOCH);
    }

    @Transactional("analyticsTransactionManager")
    public void saveCheckpoint(String sourceTable, LocalDateTime at) {
        EtlCheckpoint cp = checkpointRepository.findById(sourceTable)
                .orElseGet(() -> new EtlCheckpoint(sourceTable, at));
        cp.setLastSyncedAt(at);
        checkpointRepository.save(cp);
    }
}
