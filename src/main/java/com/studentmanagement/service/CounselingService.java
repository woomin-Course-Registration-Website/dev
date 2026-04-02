package com.studentmanagement.service;

import com.studentmanagement.domain.Counseling;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.counseling.CounselingRequest;
import com.studentmanagement.dto.counseling.CounselingResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.repository.CounselingRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 상담 관리 서비스
 *
 * 교사가 학생과 진행한 상담 내역의 CRUD를 담당합니다.
 * 수정/삭제는 작성한 교사 본인만 가능합니다 (이메일로 본인 여부 검증).
 */
@Service
@Transactional(readOnly = true)
public class CounselingService {

    private final CounselingRepository counselingRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public CounselingService(CounselingRepository counselingRepository,
                             StudentRepository studentRepository,
                             UserRepository userRepository) {
        this.counselingRepository = counselingRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 상담 목록 조회
     * studentId / teacherId / 날짜 범위로 복합 필터링합니다.
     * 파라미터가 null이면 해당 조건은 무시됩니다.
     */
    public List<CounselingResponse> getAll(Long studentId, Long teacherId, LocalDate from, LocalDate to) {
        return counselingRepository.findByFilters(studentId, teacherId, from, to)
                .stream().map(CounselingResponse::new).toList();
    }

    /** 상담 상세 조회 */
    public CounselingResponse getById(Long id) {
        return new CounselingResponse(findCounseling(id));
    }

    /**
     * 상담 등록
     * teacherEmail은 JWT subject에서 추출한 현재 로그인 교사의 이메일입니다.
     * shareScope 미입력 시 기본값 ALL이 적용됩니다.
     */
    @Transactional
    public CounselingResponse create(CounselingRequest request, String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));

        Counseling counseling = new Counseling();
        counseling.setTeacher(teacher);
        counseling.setStudent(student);
        counseling.setDate(request.getDate());
        counseling.setContent(request.getContent());
        counseling.setNextPlan(request.getNextPlan());
        counseling.setShareScope(request.getShareScope() != null ? request.getShareScope() : Counseling.ShareScope.ALL);

        return new CounselingResponse(counselingRepository.save(counseling));
    }

    /**
     * 상담 수정
     * 작성자 본인 여부를 이메일로 검증합니다.
     *
     * @throws UnauthorizedException 작성자가 아닌 경우
     */
    @Transactional
    public CounselingResponse update(Long id, CounselingRequest request, String teacherEmail) {
        Counseling counseling = findCounseling(id);

        if (!counseling.getTeacher().getEmail().equals(teacherEmail)) {
            throw new UnauthorizedException("본인이 작성한 상담 내역만 수정할 수 있습니다.");
        }

        counseling.setDate(request.getDate());
        counseling.setContent(request.getContent());
        counseling.setNextPlan(request.getNextPlan());
        if (request.getShareScope() != null) {
            counseling.setShareScope(request.getShareScope());
        }
        return new CounselingResponse(counseling);
    }

    /**
     * 상담 삭제
     * 작성자 본인 여부를 이메일로 검증합니다.
     *
     * @throws UnauthorizedException 작성자가 아닌 경우
     */
    @Transactional
    public void delete(Long id, String teacherEmail) {
        Counseling counseling = findCounseling(id);
        if (!counseling.getTeacher().getEmail().equals(teacherEmail)) {
            throw new UnauthorizedException("본인이 작성한 상담 내역만 삭제할 수 있습니다.");
        }
        counselingRepository.delete(counseling);
    }

    /** 상담 ID로 엔티티를 조회하는 공통 메서드 */
    private Counseling findCounseling(Long id) {
        return counselingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("상담 내역을 찾을 수 없습니다."));
    }
}
