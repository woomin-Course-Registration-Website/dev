package com.studentmanagement.service;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.PagedResponse;
import com.studentmanagement.dto.student.StudentRequest;
import com.studentmanagement.dto.student.StudentResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학생 관리 서비스
 *
 * 학생 등록·조회·수정을 담당합니다.
 * 학생 삭제는 제공하지 않습니다 — 연관된 성적·피드백·상담 내역 보존을 위해
 * 삭제 대신 비활성화 처리를 권장합니다 (향후 기능).
 */
@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 학생 목록 조회
     * grade / classNum / keyword(이름 부분 검색) 필터링을 지원합니다.
     * 파라미터가 null이면 해당 조건은 무시됩니다.
     * 결과는 학년·반·번호 오름차순으로 정렬됩니다.
     */
    public List<StudentResponse> getAll(Integer grade, Integer classNum, String keyword) {
        return studentRepository.findByFilters(grade, classNum, keyword)
                .stream().map(StudentResponse::new).toList();
    }

    /**
     * 학생 목록 페이지네이션 조회.
     * 정렬은 Pageable에 지정한 sort를 사용하거나, 미지정 시 학년·반·번호 오름차순으로 강제한다.
     */
    public PagedResponse<StudentResponse> getPage(Integer grade, Integer classNum, String keyword,
                                                  Pageable pageable) {
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by("grade", "classNum", "studentNum"));
        return PagedResponse.of(
                studentRepository.findByFiltersPaged(grade, classNum, keyword, effective),
                StudentResponse::new);
    }

    /** 학생 상세 조회 */
    public StudentResponse getById(Long id) {
        return new StudentResponse(findStudent(id));
    }

    /**
     * 학생 등록
     * userId가 있으면 해당 사용자 계정과 연동합니다.
     * userId가 없으면 계정 미연동 상태로 등록됩니다.
     */
    @Transactional
    public StudentResponse create(StudentRequest request) {
        Student student = new Student(request.getName(), request.getGrade(),
                request.getClassNum(), request.getStudentNum());
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            student.setUser(user);
        }
        return new StudentResponse(studentRepository.save(student));
    }

    /**
     * 학생 정보 수정
     * 학년·반·번호·이름 및 연동 계정을 변경할 수 있습니다.
     */
    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = findStudent(id);
        student.setName(request.getName());
        student.setGrade(request.getGrade());
        student.setClassNum(request.getClassNum());
        student.setStudentNum(request.getStudentNum());
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            student.setUser(user);
        }
        return new StudentResponse(studentRepository.save(student));
    }

    /** 현재 로그인한 STUDENT 사용자에 연동된 학생 정보 조회 */
    public StudentResponse getMyStudent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("연동된 학생 정보가 없습니다."));
        return new StudentResponse(student);
    }

    /** 현재 로그인한 PARENT 사용자의 자녀 목록 조회 */
    public List<StudentResponse> getMyChildren(String email) {
        User parent = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        return studentRepository.findByParentId(parent.getId())
                .stream().map(StudentResponse::new).toList();
    }

    /**
     * 학생에 학부모 계정 연동 (교사 전용)
     * parentUserId는 PARENT 역할의 User ID여야 합니다.
     */
    @Transactional
    public StudentResponse linkParent(Long studentId, Long parentUserId) {
        Student student = findStudent(studentId);
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        if (parent.getRole() != User.Role.PARENT) {
            throw new IllegalArgumentException("PARENT 역할의 계정만 연동할 수 있습니다.");
        }
        student.getParents().add(parent);
        return new StudentResponse(studentRepository.save(student));
    }

    /** 학생에서 학부모 계정 연동 해제 (교사 전용) */
    @Transactional
    public StudentResponse unlinkParent(Long studentId, Long parentUserId) {
        Student student = findStudent(studentId);
        student.getParents().removeIf(p -> p.getId().equals(parentUserId));
        return new StudentResponse(studentRepository.save(student));
    }

    /** 학생 ID로 엔티티를 조회하는 공통 메서드 */
    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다."));
    }
}
