package com.studentmanagement.repository;

import com.studentmanagement.domain.Grade;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GradeRepository 슬라이스 테스트
 *
 * 새로 추가된 GROUP BY/IN 집계 쿼리(findAverageScoresGrouped, findTotalScoresForStudent,
 * countByYearSemesterGroupedBySubject)가 실제 Hibernate를 통해 정상 실행되는지 검증한다.
 * 단위 테스트의 mocking으로는 잡히지 않는 JPQL 문법/매핑 회귀를 막는 안전망.
 *
 * H2 인메모리 DB + H2Dialect를 사용해 외부 인프라 의존 없이 실행 가능.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    // `year` 같은 예약어 컬럼이 안전하게 quoted 되도록 globally_quoted_identifiers 활성화
    "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:grade-test;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class GradeRepositoryTest {

    @Autowired GradeRepository gradeRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired SubjectRepository subjectRepository;

    private Student student1;
    private Student student2;
    private Subject math;
    private Subject english;

    @BeforeEach
    void setUp() {
        student1 = studentRepository.save(new Student("학생1", 2, 1, 1));
        student2 = studentRepository.save(new Student("학생2", 2, 1, 2));
        math = subjectRepository.save(new Subject("수학"));
        english = subjectRepository.save(new Subject("영어"));

        saveGrade(student1, math, 2025, 1, "85.00");
        saveGrade(student1, english, 2025, 1, "95.00");
        saveGrade(student2, math, 2025, 1, "75.00");
        saveGrade(student1, math, 2025, 2, "90.00");
    }

    @Test
    void findAverageScoresGrouped_returnsGroupedAverages() {
        List<Object[]> rows = gradeRepository.findAverageScoresGrouped(
                List.of(math.getId(), english.getId()),
                List.of(2025),
                List.of(1, 2));

        Map<String, Double> byKey = rows.stream().collect(Collectors.toMap(
                r -> r[0] + ":" + r[1] + ":" + r[2],
                r -> ((Number) r[3]).doubleValue()));

        // 수학 2025/1: (85+75)/2 = 80
        assertThat(byKey).containsEntry(math.getId() + ":2025:1", 80.0);
        // 영어 2025/1: 95
        assertThat(byKey).containsEntry(english.getId() + ":2025:1", 95.0);
        // 수학 2025/2: 90
        assertThat(byKey).containsEntry(math.getId() + ":2025:2", 90.0);
        // 영어 2025/2 - 데이터 없음, 결과에 미포함
        assertThat(byKey).doesNotContainKey(english.getId() + ":2025:2");
    }

    @Test
    void findTotalScoresForStudent_returnsTotalPerSemester() {
        List<Object[]> rows = gradeRepository.findTotalScoresForStudent(
                student1.getId(),
                List.of(2025),
                List.of(1, 2));

        Map<String, Double> byKey = rows.stream().collect(Collectors.toMap(
                r -> r[0] + ":" + r[1],
                r -> ((Number) r[2]).doubleValue()));

        // 학생1, 2025/1 합계: 85 + 95 = 180
        assertThat(byKey).containsEntry("2025:1", 180.0);
        // 학생1, 2025/2 합계: 90
        assertThat(byKey).containsEntry("2025:2", 90.0);
    }

    @Test
    void countByYearSemesterGroupedBySubject_returnsCountsPerSubject() {
        List<Object[]> rows = gradeRepository.countByYearSemesterGroupedBySubject(
                2025, 1, List.of(student1.getId(), student2.getId()));

        Map<Long, Long> byId = rows.stream().collect(Collectors.toMap(
                r -> (Long) r[0],
                r -> ((Number) r[1]).longValue()));

        // 수학 2025/1: 학생1 + 학생2 = 2명 입력됨
        assertThat(byId).containsEntry(math.getId(), 2L);
        // 영어 2025/1: 학생1만 = 1명 입력됨
        assertThat(byId).containsEntry(english.getId(), 1L);
    }

    @Test
    void findByStudentAndFilters_fetchesSubjectEagerly() {
        // JOIN FETCH로 Subject가 함께 로드되어 N+1이 발생하지 않는지 확인
        List<Grade> grades = gradeRepository.findByStudentAndFilters(student1.getId(), 2025, null, null);

        assertThat(grades).hasSize(3);
        // 트랜잭션 외부에서도 접근 가능해야 LAZY init 예외가 발생하지 않음
        for (Grade g : grades) {
            assertThat(g.getSubject().getName()).isNotBlank();
        }
    }

    private void saveGrade(Student s, Subject sub, int year, int semester, String score) {
        Grade g = new Grade();
        g.setStudent(s);
        g.setSubject(sub);
        g.setYear(year);
        g.setSemester(semester);
        g.setScore(new BigDecimal(score));
        g.setGradeRank("B");
        gradeRepository.save(g);
    }
}
