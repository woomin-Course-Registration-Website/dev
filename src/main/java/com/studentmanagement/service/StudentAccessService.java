package com.studentmanagement.service;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.exception.UnauthorizedException;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생 데이터 접근 권한 검증 서비스
 *
 * GradeService, FeedbackService, CounselingService, StudentRecordService에서 공통으로 사용.
 * - TEACHER : 모든 학생 접근 가능
 * - STUDENT : student.user.email == requesterEmail 인 경우만 허용
 * - PARENT  : student_parents 테이블에 (studentId, parent.id) 쌍이 존재할 때만 허용
 */
@Service
@Transactional(readOnly = true)
public class StudentAccessService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentAccessService(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public void check(Long studentId, String requesterEmail, User.Role role) {
        if (role == User.Role.TEACHER) return;

        if (role == User.Role.STUDENT) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
            if (student.getUser() == null || !student.getUser().getEmail().equals(requesterEmail)) {
                throw new UnauthorizedException("본인의 정보만 조회할 수 있습니다.");
            }
            return;
        }

        if (role == User.Role.PARENT) {
            User parent = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            if (!studentRepository.existsByIdAndParentId(studentId, parent.getId())) {
                throw new UnauthorizedException("자녀의 정보만 조회할 수 있습니다.");
            }
        }
    }
}
