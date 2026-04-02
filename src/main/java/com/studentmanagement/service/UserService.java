package com.studentmanagement.service;

import com.studentmanagement.domain.User;
import com.studentmanagement.dto.user.UserRequest;
import com.studentmanagement.dto.user.UserResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import com.studentmanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 계정 관리 서비스 (ADMIN 전용)
 *
 * 교사·학생·학부모·관리자 계정의 CRUD를 담당합니다.
 * 비밀번호는 BCrypt로 암호화하여 저장됩니다.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 전체 사용자 목록 조회 */
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::new).toList();
    }

    /**
     * 사용자 생성
     * 이메일 중복 여부를 먼저 확인합니다.
     *
     * @throws IllegalArgumentException 이메일 중복 시
     */
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User user = new User(request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getRole());
        return new UserResponse(userRepository.save(user));
    }

    /**
     * 사용자 정보 수정
     * 이름·역할을 변경합니다.
     * 비밀번호는 요청에 값이 있을 때만 변경됩니다 (빈 문자열 무시).
     */
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        user.setName(request.getName());
        user.setRole(request.getRole());
        // 비밀번호 변경 요청이 있을 때만 업데이트
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return new UserResponse(user);
    }

    /**
     * 사용자 삭제
     * 연동된 학생의 user_id는 NULL로 변경됩니다 (ON DELETE SET NULL).
     */
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
        }
        userRepository.deleteById(id);
    }
}
