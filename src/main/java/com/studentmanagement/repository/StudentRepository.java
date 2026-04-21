package com.studentmanagement.repository;

import com.studentmanagement.domain.Student;
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

    @Query("SELECT s FROM Student s WHERE s.user.id = :userId")
    Optional<Student> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) > 0 FROM Student s JOIN s.parents p WHERE s.id = :studentId AND p.id = :parentId")
    boolean existsByIdAndParentId(@Param("studentId") Long studentId, @Param("parentId") Long parentId);

    @Query("SELECT s FROM Student s JOIN s.parents p WHERE p.id = :parentId ORDER BY s.grade, s.classNum, s.studentNum")
    List<Student> findByParentId(@Param("parentId") Long parentId);
}
