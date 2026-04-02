package com.studentmanagement.repository;

import com.studentmanagement.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE " +
           "(:grade IS NULL OR s.grade = :grade) AND " +
           "(:classNum IS NULL OR s.classNum = :classNum) AND " +
           "(:keyword IS NULL OR s.name LIKE %:keyword%) " +
           "ORDER BY s.grade, s.classNum, s.studentNum")
    List<Student> findByFilters(@Param("grade") Integer grade,
                                @Param("classNum") Integer classNum,
                                @Param("keyword") String keyword);
}
