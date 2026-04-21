package com.studentmanagement.service;

import com.studentmanagement.domain.Grade;
import com.studentmanagement.domain.Notification;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.Subject;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.grade.GradeRequest;
import com.studentmanagement.dto.grade.GradeResponse;
import com.studentmanagement.dto.grade.GradeStatsItem;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.GradeRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 성적 관리 서비스
 *
 * 성적 CRUD 및 자동 계산 로직을 담당합니다.
 *
 * 성적 응답에 포함되는 자동 계산 값:
 * - average : 해당 과목·연도·학기의 전체 학생 평균 점수
 * - total   : 해당 학생의 해당 학기 전 과목 합산 점수
 */
@Service
@Transactional(readOnly = true)
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final NotificationService notificationService;
    private final StudentAccessService studentAccessService;

    public GradeService(GradeRepository gradeRepository, StudentRepository studentRepository,
                        SubjectRepository subjectRepository, NotificationService notificationService,
                        StudentAccessService studentAccessService) {
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.notificationService = notificationService;
        this.studentAccessService = studentAccessService;
    }

    /**
     * 학생 성적 목록 조회
     * STUDENT는 본인, PARENT는 연동된 자녀만 조회 가능합니다.
     */
    public List<GradeResponse> getGrades(Long studentId, Integer year, Integer semester, Long subjectId,
                                         String requesterEmail, User.Role role) {
        studentAccessService.check(studentId, requesterEmail, role);
        return gradeRepository.findByStudentAndFilters(studentId, year, semester, subjectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 성적 입력
     *
     * 성적을 저장하고 점수에 따라 gradeRank를 자동 계산합니다.
     * 동일한 (학생, 과목, 연도, 학기) 조합이 이미 존재하면 DB 유니크 제약에 의해 예외가 발생합니다.
     */
    @Transactional
    public GradeResponse create(Long studentId, GradeRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("과목을 찾을 수 없습니다."));

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setYear(request.getYear());
        grade.setSemester(request.getSemester());
        grade.setScore(request.getScore());
        grade.setGradeRank(calculateRank(request.getScore().doubleValue()));

        GradeResponse response = toResponse(gradeRepository.save(grade));
        notificationService.send(
                student.getUser(),
                Notification.Type.GRADE,
                subject.getName() + " 성적이 등록되었습니다. (" + response.getGradeRank() + "등급)"
        );
        return response;
    }

    /**
     * 성적 수정
     * 점수를 변경하면 gradeRank도 자동으로 재계산됩니다.
     */
    @Transactional
    public GradeResponse update(Long gradeId, GradeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("성적을 찾을 수 없습니다."));
        grade.setScore(request.getScore());
        grade.setGradeRank(calculateRank(request.getScore().doubleValue()));
        return toResponse(gradeRepository.save(grade));
    }

    /** 성적 삭제 */
    @Transactional
    public void delete(Long gradeId) {
        if (!gradeRepository.existsById(gradeId)) {
            throw new ResourceNotFoundException("성적을 찾을 수 없습니다.");
        }
        gradeRepository.deleteById(gradeId);
    }

    /**
     * 과목별 성적 입력 현황 (대시보드용)
     * 각 과목에 대해 해당 학급 학생 중 성적이 입력된 수와 전체 학생 수를 반환합니다.
     */
    public List<GradeStatsItem> getStats(Integer grade, Integer classNum, Integer year, Integer semester) {
        int y = (year     != null) ? year     : java.time.LocalDate.now().getYear();
        int s = (semester != null) ? semester : 1;

        List<Student> students = studentRepository.findByFilters(grade, classNum, null);
        List<Long> studentIds  = students.stream().map(Student::getId).toList();
        long studentCount      = studentIds.size();

        return subjectRepository.findAll().stream()
                .map(subject -> {
                    long count = studentIds.isEmpty() ? 0
                            : gradeRepository.countBySubjectYearSemesterAndStudents(
                                subject.getId(), y, s, studentIds);
                    return new GradeStatsItem(subject.getName(), count, studentCount);
                })
                .toList();
    }

    /**
     * Grade 엔티티를 GradeResponse DTO로 변환합니다.
     * average(과목 전체 평균)와 total(학생 학기 총점)을 추가로 조회합니다.
     */
    private GradeResponse toResponse(Grade grade) {
        Double average = gradeRepository.findAverageScore(
                grade.getSubject().getId(), grade.getYear(), grade.getSemester());
        Double total = gradeRepository.findTotalScore(
                grade.getStudent().getId(), grade.getYear(), grade.getSemester());
        return new GradeResponse(grade, average, total);
    }

    /**
     * 점수에 따른 등급 문자 반환
     * 90↑ A / 80↑ B / 70↑ C / 60↑ D / 60↓ F
     */
    private String calculateRank(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
