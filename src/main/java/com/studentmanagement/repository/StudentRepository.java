package com.studentmanagement.repository;

import com.studentmanagement.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE " +
           "(:grade IS NULL OR s.grade = :grade) AND " +
           "(:classNum IS NULL OR s.classNum = :classNum) AND " +
           "(:keyword IS NULL OR s.name LIKE %:keyword%) " +
           "ORDER BY s.grade, s.classNum, s.studentNum")
    List<Student> findByFilters(@Param("grade") Integer grade,
                                @Param("classNum") Integer classNum,
                                @Param("keyword") String keyword);

    /**
     * 페이지네이션 지원 버전. countQuery 별도 명시로 ORDER BY 절을 제거하고 성능 확보.
     */
    @Query(value = "SELECT s FROM Student s WHERE " +
                   "(:grade IS NULL OR s.grade = :grade) AND " +
                   "(:classNum IS NULL OR s.classNum = :classNum) AND " +
                   "(:keyword IS NULL OR s.name LIKE %:keyword%)",
           countQuery = "SELECT count(s) FROM Student s WHERE " +
                        "(:grade IS NULL OR s.grade = :grade) AND " +
                        "(:classNum IS NULL OR s.classNum = :classNum) AND " +
                        "(:keyword IS NULL OR s.name LIKE %:keyword%)")
    Page<Student> findByFiltersPaged(@Param("grade") Integer grade,
                                     @Param("classNum") Integer classNum,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);

    @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
    Optional<Student> findByUserId(@Param("userId") Long userId);

    /**
     * 학년·반 조건에 해당하는 학생들의 ID만 조회.
     * 통계 집계처럼 학생 본문이 필요 없는 경우 사용해 메모리/네트워크 비용을 줄인다.
     */
    @Query("SELECT s.id FROM Student s WHERE " +
           "(:grade IS NULL OR s.grade = :grade) AND " +
           "(:classNum IS NULL OR s.classNum = :classNum)")
    List<Long> findIdsByFilters(@Param("grade") Integer grade,
                                @Param("classNum") Integer classNum);

    Optional<Student> findByGradeAndClassNumAndStudentNum(int grade, int classNum, int studentNum);

    @Query("SELECT COUNT(s) > 0 FROM Student s JOIN s.parents p WHERE s.id = :studentId AND p.id = :parentId")
    boolean existsByIdAndParentId(@Param("studentId") Long studentId, @Param("parentId") Long parentId);

    @Query("SELECT s FROM Student s JOIN s.parents p WHERE p.id = :parentId ORDER BY s.grade, s.classNum, s.studentNum")
    List<Student> findByParentId(@Param("parentId") Long parentId);
}
