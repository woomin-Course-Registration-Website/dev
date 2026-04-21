package com.studentmanagement.fixture;

import com.studentmanagement.domain.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TestFixtures {

    // ── Users ─────────────────────────────────────────────────────────

    public static User teacherUser() {
        User u = new User("teacher@test.com", "encoded_pw", "김교사", User.Role.TEACHER);
        setId(u, 1L);
        return u;
    }

    public static User studentUser() {
        User u = new User("student@test.com", "encoded_pw", "홍길동", User.Role.STUDENT);
        setId(u, 2L);
        return u;
    }

    public static User parentUser() {
        User u = new User("parent@test.com", "encoded_pw", "홍부모", User.Role.PARENT);
        setId(u, 3L);
        return u;
    }

    public static User adminUser() {
        User u = new User("admin@test.com", "encoded_pw", "관리자", User.Role.ADMIN);
        setId(u, 4L);
        return u;
    }

    // ── Students ──────────────────────────────────────────────────────

    public static Student student(User linkedUser) {
        Student s = new Student("홍길동", 2, 3, 15);
        setId(s, 10L);
        s.setUser(linkedUser);
        return s;
    }

    public static Student studentNoUser() {
        Student s = new Student("홍길동", 2, 3, 15);
        setId(s, 10L);
        return s;
    }

    // ── Subject ───────────────────────────────────────────────────────

    public static Subject subject() {
        Subject s = new Subject("수학");
        setId(s, 100L);
        return s;
    }

    // ── Grade ─────────────────────────────────────────────────────────

    public static Grade grade(Student student, Subject subject) {
        Grade g = new Grade();
        setId(g, 200L);
        g.setStudent(student);
        g.setSubject(subject);
        g.setYear(2025);
        g.setSemester(1);
        g.setScore(new BigDecimal("85.00"));
        g.setGradeRank("B");
        return g;
    }

    // ── Feedback ──────────────────────────────────────────────────────

    public static Feedback feedback(User teacher, Student student) {
        Feedback f = new Feedback();
        setId(f, 300L);
        f.setTeacher(teacher);
        f.setStudent(student);
        f.setCategory(Feedback.Category.GRADE);
        f.setContent("성적이 향상되었습니다.");
        f.setPublic(true);
        return f;
    }

    public static Feedback privateFeedback(User teacher, Student student) {
        Feedback f = feedback(teacher, student);
        f.setPublic(false);
        return f;
    }

    // ── Counseling ────────────────────────────────────────────────────

    public static Counseling counseling(User teacher, Student student) {
        Counseling c = new Counseling();
        setId(c, 400L);
        c.setTeacher(teacher);
        c.setStudent(student);
        c.setDate(LocalDate.of(2025, 3, 10));
        c.setContent("학업 태도 향상 상담");
        c.setNextPlan("다음 달 재상담 예정");
        c.setShareScope(Counseling.ShareScope.ALL);
        return c;
    }

    // ── Notification ──────────────────────────────────────────────────

    public static Notification notification(User user) {
        Notification n = new Notification(user, Notification.Type.GRADE, "수학 성적이 등록되었습니다. (B등급)");
        setId(n, 500L);
        return n;
    }

    // ── Reflection helper ─────────────────────────────────────────────

    public static void setId(Object obj, Long id) {
        try {
            Class<?> clazz = obj.getClass();
            Field f = null;
            while (clazz != null) {
                try { f = clazz.getDeclaredField("id"); break; }
                catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
            }
            if (f == null) throw new RuntimeException("id field not found in " + obj.getClass());
            f.setAccessible(true);
            f.set(obj, id);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
