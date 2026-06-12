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

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

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

    @Transactional
    public StudentResponse create(StudentRequest request) {
        assertStudentNumberAvailable(request, null);

        Student student = new Student(
                request.getName(),
                request.getGrade(),
                request.getClassNum(),
                request.getStudentNum()
        );
        student.setUser(resolveStudentUser(request.getUserId(), null));
        return new StudentResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = findStudent(id);
        assertStudentNumberAvailable(request, id);

        student.setName(request.getName());
        student.setGrade(request.getGrade());
        student.setClassNum(request.getClassNum());
        student.setStudentNum(request.getStudentNum());
        student.setUser(resolveStudentUser(request.getUserId(), id));
        return new StudentResponse(studentRepository.save(student));
    }

    public StudentResponse getMyStudent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Linked student profile not found."));
        return new StudentResponse(student);
    }

    public List<StudentResponse> getMyChildren(String email) {
        User parent = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return studentRepository.findByParentId(parent.getId())
                .stream().map(StudentResponse::new).toList();
    }

    @Transactional
    public StudentResponse linkParent(Long studentId, Long parentUserId) {
        Student student = findStudent(studentId);
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (parent.getRole() != User.Role.PARENT) {
            throw new IllegalArgumentException("Only PARENT accounts can be linked as guardians.");
        }
        student.getParents().add(parent);
        return new StudentResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse unlinkParent(Long studentId, Long parentUserId) {
        Student student = findStudent(studentId);
        student.getParents().removeIf(p -> p.getId().equals(parentUserId));
        return new StudentResponse(studentRepository.save(student));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    private void assertStudentNumberAvailable(StudentRequest request, Long currentStudentId) {
        boolean duplicated = currentStudentId == null
                ? studentRepository.existsByGradeAndClassNumAndStudentNum(
                        request.getGrade(), request.getClassNum(), request.getStudentNum())
                : studentRepository.existsByGradeAndClassNumAndStudentNumAndIdNot(
                        request.getGrade(), request.getClassNum(), request.getStudentNum(), currentStudentId);
        if (duplicated) {
            throw new IllegalArgumentException("A student with the same grade, class, and number already exists.");
        }
    }

    private User resolveStudentUser(Long userId, Long currentStudentId) {
        if (userId == null) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("Only STUDENT accounts can be linked to a student profile.");
        }

        studentRepository.findByUserId(userId)
                .filter(existing -> currentStudentId == null || !existing.getId().equals(currentStudentId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This STUDENT account is already linked to another student.");
                });
        return user;
    }
}
