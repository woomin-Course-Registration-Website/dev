package com.studentmanagement.service;

import com.studentmanagement.domain.Assignment;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.Subject;
import com.studentmanagement.domain.Submission;
import com.studentmanagement.dto.assignment.AssignmentRequest;
import com.studentmanagement.dto.assignment.AssignmentResponse;
import com.studentmanagement.dto.assignment.SubmissionRequest;
import com.studentmanagement.dto.assignment.SubmissionResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.AssignmentRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.SubjectRepository;
import com.studentmanagement.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 과제 관리 서비스
 *
 * 교사가 과목별 과제를 등록·조회하고, 학생별 제출 상태를 기록·갱신합니다.
 * 이 데이터는 학습 분석(EP-08)의 "과제 제출률" 지표 원천이 됩니다.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SubmissionRepository submissionRepository,
                             SubjectRepository subjectRepository,
                             StudentRepository studentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.subjectRepository = subjectRepository;
        this.studentRepository = studentRepository;
    }

    /** 과목별 과제 목록 */
    public List<AssignmentResponse> getBySubject(Long subjectId) {
        return assignmentRepository.findBySubjectId(subjectId)
                .stream().map(AssignmentResponse::new).toList();
    }

    /** 과제 등록 */
    @Transactional
    public AssignmentResponse create(Long subjectId, AssignmentRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("과목을 찾을 수 없습니다."));

        Assignment assignment = new Assignment();
        assignment.setSubject(subject);
        assignment.setTitle(request.getTitle());
        assignment.setDueDate(request.getDueDate());
        assignment.setYear(request.getYear());
        assignment.setSemester(request.getSemester());
        return new AssignmentResponse(assignmentRepository.save(assignment));
    }

    /** 특정 과제의 제출 목록 */
    public List<SubmissionResponse> getSubmissions(Long assignmentId) {
        ensureAssignment(assignmentId);
        return submissionRepository.findByAssignmentId(assignmentId)
                .stream().map(SubmissionResponse::new).toList();
    }

    /**
     * 학생별 제출 상태 기록/갱신 (upsert).
     * 동일 (과제, 학생) 조합이 있으면 상태만 갱신, 없으면 새로 생성합니다.
     */
    @Transactional
    public SubmissionResponse recordSubmission(Long assignmentId, SubmissionRequest request) {
        Assignment assignment = ensureAssignment(assignmentId);
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, request.getStudentId())
                .orElseGet(() -> {
                    Submission s = new Submission();
                    s.setAssignment(assignment);
                    s.setStudent(student);
                    return s;
                });

        submission.setStatus(request.getStatus());
        submission.setSubmittedAt(
                request.getStatus() == Submission.Status.NOT_SUBMITTED ? null : LocalDateTime.now());
        return new SubmissionResponse(submissionRepository.save(submission));
    }

    private Assignment ensureAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("과제를 찾을 수 없습니다."));
    }
}
