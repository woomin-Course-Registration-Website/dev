package com.studentmanagement.service;

import com.studentmanagement.domain.Subject;
import com.studentmanagement.dto.subject.SubjectRequest;
import com.studentmanagement.dto.subject.SubjectResponse;
import com.studentmanagement.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과목 서비스
 *
 * 성적 입력 시 참조되는 과목 마스터 데이터를 관리합니다.
 * 과목명은 고유값(UNIQUE)이므로 중복 등록을 방지합니다.
 */
@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    /** 전체 과목 목록 조회 */
    public List<SubjectResponse> getAll() {
        return subjectRepository.findAll().stream().map(SubjectResponse::new).toList();
    }

    /**
     * 과목 추가
     *
     * @throws IllegalArgumentException 동일한 과목명이 이미 존재하는 경우
     */
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 존재하는 과목입니다.");
        }
        return new SubjectResponse(subjectRepository.save(new Subject(request.getName())));
    }
}
