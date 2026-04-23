package com.studentmanagement.service;

import com.studentmanagement.domain.Feedback;
import com.studentmanagement.domain.Notification;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.feedback.FeedbackRequest;
import com.studentmanagement.dto.feedback.FeedbackResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.repository.FeedbackRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 피드백 관리 서비스
 *
 * 교사가 학생에게 남기는 피드백의 CRUD를 담당합니다.
 * 조회 시 요청자의 역할에 따라 반환되는 피드백 범위가 다릅니다.
 * - TEACHER  : 공개·비공개 피드백 전체 반환
 * - STUDENT / PARENT : isPublic=true 인 피드백만 반환
 */
@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StudentAccessService studentAccessService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           StudentRepository studentRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           StudentAccessService studentAccessService) {
        this.feedbackRepository = feedbackRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.studentAccessService = studentAccessService;
    }

    /**
     * 피드백 목록 조회
     * TEACHER는 공개+비공개 전체, STUDENT/PARENT는 공개 피드백만 반환합니다.
     * STUDENT/PARENT는 소유권(본인 또는 연동된 자녀) 검증 후 조회합니다.
     */
    public List<FeedbackResponse> getFeedbacks(Long studentId, String requesterEmail, User.Role role) {
        if (role == User.Role.TEACHER) {
            return feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                    .stream().map(FeedbackResponse::new).toList();
        }
        studentAccessService.check(studentId, requesterEmail, role);
        return feedbackRepository.findPublicByStudentId(studentId)
                .stream().map(FeedbackResponse::new).toList();
    }

    /**
     * 피드백 작성
     * teacherEmail은 JWT의 subject에서 추출한 현재 로그인 교사의 이메일입니다.
     */
    @Transactional
    public FeedbackResponse create(Long studentId, FeedbackRequest request, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        Feedback feedback = new Feedback();
        feedback.setTeacher(teacher);
        feedback.setStudent(student);
        feedback.setCategory(request.getCategory());
        feedback.setContent(request.getContent());
        feedback.setPublic(request.isPublic());

        FeedbackResponse response = new FeedbackResponse(feedbackRepository.save(feedback));
        if (request.isPublic() && student.getUser() != null) {
            notificationService.send(
                    student.getUser(),
                    Notification.Type.FEEDBACK,
                    teacher.getName() + " 선생님이 피드백을 남겼습니다."
            );
        }
        return response;
    }

    /**
     * 피드백 수정
     * 작성자 본인 여부를 이메일로 검증합니다.
     *
     * @throws UnauthorizedException 작성자가 아닌 경우
     */
    @Transactional
    public FeedbackResponse update(Long feedbackId, FeedbackRequest request, String teacherEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("피드백을 찾을 수 없습니다."));

        if (!feedback.getTeacher().getEmail().equals(teacherEmail)) {
            throw new UnauthorizedException("본인이 작성한 피드백만 수정할 수 있습니다.");
        }

        feedback.setCategory(request.getCategory());
        feedback.setContent(request.getContent());
        feedback.setPublic(request.isPublic());
        return new FeedbackResponse(feedbackRepository.save(feedback));
    }

    /**
     * 피드백 삭제
     * 작성자 본인 여부를 이메일로 검증합니다.
     *
     * @throws UnauthorizedException 작성자가 아닌 경우
     */
    @Transactional
    public void delete(Long feedbackId, String teacherEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("피드백을 찾을 수 없습니다."));

        if (!feedback.getTeacher().getEmail().equals(teacherEmail)) {
            throw new UnauthorizedException("본인이 작성한 피드백만 삭제할 수 있습니다.");
        }
        feedbackRepository.delete(feedback);
    }
}
