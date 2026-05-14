package com.studentmanagement.repository;

import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StudentRepository 슬라이스 테스트
 *
 * findIdsByFilters / existsByIdAndParentId 등 통계·인가 결정에 직접 영향을 주는
 * 쿼리들이 실 DB에서 정확한 결과를 내는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:student-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class StudentRepositoryTest {

    @Autowired StudentRepository studentRepository;
    @Autowired UserRepository userRepository;

    private Student s1;
    private Student s2;
    private Student s3;
    private User parent;

    @BeforeEach
    void setUp() {
        s1 = studentRepository.save(new Student("A", 1, 1, 1));
        s2 = studentRepository.save(new Student("B", 1, 1, 2));
        s3 = studentRepository.save(new Student("C", 2, 2, 1));

        parent = userRepository.save(new User("parent@test.com", "pw", "부모", User.Role.PARENT));
        s1.getParents().add(parent);
        studentRepository.save(s1);
    }

    @Test
    void findIdsByFilters_filtersByGradeAndClass() {
        List<Long> idsGrade1 = studentRepository.findIdsByFilters(1, null);
        assertThat(idsGrade1).containsExactlyInAnyOrder(s1.getId(), s2.getId());

        List<Long> idsClass2Grade2 = studentRepository.findIdsByFilters(2, 2);
        assertThat(idsClass2Grade2).containsExactly(s3.getId());

        List<Long> allIds = studentRepository.findIdsByFilters(null, null);
        assertThat(allIds).hasSize(3);
    }

    @Test
    void existsByIdAndParentId_returnsTrueOnlyWhenLinked() {
        assertThat(studentRepository.existsByIdAndParentId(s1.getId(), parent.getId())).isTrue();
        assertThat(studentRepository.existsByIdAndParentId(s2.getId(), parent.getId())).isFalse();
        assertThat(studentRepository.existsByIdAndParentId(s3.getId(), parent.getId())).isFalse();
    }

    @Test
    void findByParentId_returnsLinkedStudentsOnly() {
        List<Student> linked = studentRepository.findByParentId(parent.getId());
        assertThat(linked).extracting(Student::getId).containsExactly(s1.getId());
    }

    @Test
    void findByFiltersPaged_paginatesAndReportsTotals() {
        // 동일 학년/반에 5명 추가 — 총 7명 중 학년1·반1 = 4명 (s1, s2 + 신규 2명)
        for (int i = 3; i <= 4; i++) {
            studentRepository.save(new com.studentmanagement.domain.Student("학생" + i, 1, 1, i + 1));
        }

        org.springframework.data.domain.Page<Student> firstPage = studentRepository.findByFiltersPaged(
                1, 1, null,
                org.springframework.data.domain.PageRequest.of(0, 2,
                        org.springframework.data.domain.Sort.by("studentNum")));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();

        org.springframework.data.domain.Page<Student> secondPage = studentRepository.findByFiltersPaged(
                1, 1, null,
                org.springframework.data.domain.PageRequest.of(1, 2,
                        org.springframework.data.domain.Sort.by("studentNum")));

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.isLast()).isTrue();
    }
}
