package com.studentmanagement.controller;

import com.studentmanagement.domain.*;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevDataSeederController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final FeedbackRepository feedbackRepository;
    private final CounselingRepository counselingRepository;
    private final StudentRecordRepository studentRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/seed")
    @Transactional
    public ApiResponse<Map<String, Integer>> seed() {
        String pw = passwordEncoder.encode("password123");

        // 교사 계정
        User teacher1 = userRepository.save(new User("teacher1@school.com", pw, "김민준", User.Role.TEACHER));
        User teacher2 = userRepository.save(new User("teacher2@school.com", pw, "이서연", User.Role.TEACHER));

        // 학생 계정 (User + Student 연동)
        User studentUser1 = userRepository.save(new User("student1@school.com", pw, "박지호", User.Role.STUDENT));
        User studentUser2 = userRepository.save(new User("student2@school.com", pw, "최유나", User.Role.STUDENT));
        User studentUser3 = userRepository.save(new User("student3@school.com", pw, "정하준", User.Role.STUDENT));
        User studentUser4 = userRepository.save(new User("student4@school.com", pw, "강서윤", User.Role.STUDENT));
        User studentUser5 = userRepository.save(new User("student5@school.com", pw, "윤도현", User.Role.STUDENT));

        // 학부모 계정
        User parent1 = userRepository.save(new User("parent1@school.com", pw, "박부모", User.Role.PARENT));
        User parent2 = userRepository.save(new User("parent2@school.com", pw, "최부모", User.Role.PARENT));

        // ADMIN
        userRepository.save(new User("admin@school.com", pw, "관리자", User.Role.ADMIN));

        // 학생 엔티티
        Student s1 = new Student("박지호", 1, 1, 1); s1.setUser(studentUser1); s1.getParents().add(parent1);
        Student s2 = new Student("최유나", 1, 1, 2); s2.setUser(studentUser2); s2.getParents().add(parent2);
        Student s3 = new Student("정하준", 1, 2, 1); s3.setUser(studentUser3);
        Student s4 = new Student("강서윤", 2, 1, 1); s4.setUser(studentUser4);
        Student s5 = new Student("윤도현", 2, 2, 3); s5.setUser(studentUser5);
        Student s6 = new Student("임수아",  3, 1, 2);
        Student s7 = new Student("한태양",  3, 2, 1);

        List<Student> students = studentRepository.saveAll(List.of(s1, s2, s3, s4, s5, s6, s7));

        // 과목
        Subject math    = subjectRepository.save(new Subject("수학"));
        Subject english = subjectRepository.save(new Subject("영어"));
        Subject korean  = subjectRepository.save(new Subject("국어"));
        Subject science = subjectRepository.save(new Subject("과학"));
        Subject history = subjectRepository.save(new Subject("한국사"));

        List<Subject> subjects = List.of(math, english, korean, science, history);

        // 성적 데이터
        double[][] scores2024s1 = {
            {92, 85, 78, 90, 88},
            {76, 91, 83, 70, 95},
            {88, 72, 95, 82, 79},
            {65, 80, 74, 68, 85},
            {90, 88, 91, 93, 87},
            {55, 62, 70, 58, 74},
            {83, 77, 86, 80, 91},
        };
        double[][] scores2024s2 = {
            {95, 88, 82, 91, 90},
            {80, 93, 86, 73, 97},
            {90, 75, 97, 85, 81},
            {68, 83, 76, 70, 87},
            {93, 91, 93, 95, 89},
            {58, 65, 72, 60, 76},
            {86, 80, 88, 83, 93},
        };

        int gradeCount = 0;
        for (int si = 0; si < students.size(); si++) {
            for (int sj = 0; sj < subjects.size(); sj++) {
                gradeCount += saveGrade(students.get(si), subjects.get(sj), 2024, 1, scores2024s1[si][sj]);
                gradeCount += saveGrade(students.get(si), subjects.get(sj), 2024, 2, scores2024s2[si][sj]);
            }
        }

        // 피드백
        List<Feedback> feedbacks = List.of(
            feedback(teacher1, s1, Feedback.Category.GRADE,      "수학 실력이 많이 향상되었습니다. 꾸준한 노력이 돋보입니다.", true),
            feedback(teacher1, s1, Feedback.Category.ATTITUDE,   "수업 태도가 매우 좋고 질문을 적극적으로 합니다.", true),
            feedback(teacher1, s2, Feedback.Category.BEHAVIOR,   "쉬는 시간에 다른 학생들과 잘 어울립니다.", true),
            feedback(teacher1, s2, Feedback.Category.GRADE,      "영어 성적이 우수합니다. 독해 능력이 특히 뛰어납니다.", true),
            feedback(teacher2, s3, Feedback.Category.ATTENDANCE, "지각이 잦아 주의가 필요합니다.", false),
            feedback(teacher2, s3, Feedback.Category.GRADE,      "국어 성적이 탁월합니다. 작문 능력이 돋보입니다.", true),
            feedback(teacher2, s4, Feedback.Category.OTHER,      "미술 동아리 활동에 열심히 참여하고 있습니다.", true),
            feedback(teacher1, s5, Feedback.Category.GRADE,      "전반적으로 성적이 우수하며 특히 과학 분야에 두각을 나타냅니다.", true),
            feedback(teacher2, s6, Feedback.Category.BEHAVIOR,   "학급 분위기를 저해하는 행동이 관찰됩니다. 면담 필요.", false),
            feedback(teacher1, s7, Feedback.Category.ATTITUDE,   "수업 참여도가 높고 리더십이 뛰어납니다.", true)
        );
        feedbackRepository.saveAll(feedbacks);

        // 상담
        List<Counseling> counselings = List.of(
            counseling(teacher1, s1, LocalDate.of(2024, 3, 15),
                "1학기 학습 목표 설정 상담. 수학 90점 이상 목표로 설정함.",
                "2주 후 중간고사 대비 2차 상담 예정", Counseling.ShareScope.ALL),
            counseling(teacher1, s1, LocalDate.of(2024, 5, 20),
                "중간고사 성적 확인 상담. 목표 달성에 근접, 수학 92점 획득.",
                "기말고사까지 현 수준 유지 독려", Counseling.ShareScope.ALL),
            counseling(teacher1, s2, LocalDate.of(2024, 4, 10),
                "진로 상담. 영어 교육 계열 희망.",
                "관련 대학 정보 수집 후 다음 상담 시 공유", Counseling.ShareScope.PRIVATE),
            counseling(teacher2, s3, LocalDate.of(2024, 3, 25),
                "지각 문제 상담. 거리가 멀어 교통 문제가 원인.",
                "학부모 연락 및 경로 변경 검토", Counseling.ShareScope.PRIVATE),
            counseling(teacher2, s4, LocalDate.of(2024, 6, 5),
                "학업 부진 상담. 수학·과학 집중 지도 필요.",
                "방과후 학습 프로그램 참여 권장", Counseling.ShareScope.ALL),
            counseling(teacher1, s5, LocalDate.of(2024, 7, 1),
                "우수 학생 심화 학습 상담. 과학올림피아드 참가 권유.",
                "대회 일정 확인 후 준비 지원", Counseling.ShareScope.ALL),
            counseling(teacher2, s6, LocalDate.of(2024, 4, 18),
                "생활 지도 상담. 학급 내 갈등 상황 파악 및 중재.",
                "1개월 후 관찰 후 재상담", Counseling.ShareScope.PRIVATE)
        );
        counselingRepository.saveAll(counselings);

        // 학생부
        List<StudentRecord> records = List.of(
            record(s1, 185, 2, 3, "2024년 수학경시대회 장려상 수상. 과학 탐구 동아리 활동 2년."),
            record(s2, 182, 3, 5, "영어 말하기 대회 최우수상. 교내 밴드 활동 참여."),
            record(s3, 178, 8, 12, "독서 토론 동아리 부장. 학교신문 편집부 활동."),
            record(s4, 183, 5, 7, "미술 동아리 부원. 학교 축제 미술 작품 전시 참여."),
            record(s5, 186, 1, 2, "과학올림피아드 지역 예선 2위. 수학·과학 성적 전교 상위권."),
            record(s6, 170, 15, 20, "방과후 학습 프로그램 이수. 생활 지도 개선 중."),
            record(s7, 184, 3, 4, "학급 회장. 학교 체육대회 기획 참여. 리더십 우수.")
        );
        studentRecordRepository.saveAll(records);

        return ApiResponse.ok(Map.of(
            "users", (int) userRepository.count(),
            "students", (int) studentRepository.count(),
            "subjects", (int) subjectRepository.count(),
            "grades", gradeCount,
            "feedbacks", feedbacks.size(),
            "counselings", counselings.size(),
            "studentRecords", records.size()
        ));
    }

    private int saveGrade(Student student, Subject subject, int year, int semester, double score) {
        if (gradeRepository.findByStudentIdAndSubjectIdAndYearAndSemester(
                student.getId(), subject.getId(), year, semester).isPresent()) {
            return 0;
        }
        Grade g = new Grade();
        g.setStudent(student);
        g.setSubject(subject);
        g.setYear(year);
        g.setSemester(semester);
        g.setScore(BigDecimal.valueOf(score));
        g.setGradeRank(calcRank(score));
        gradeRepository.save(g);
        return 1;
    }

    private String calcRank(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private Feedback feedback(User teacher, Student student, Feedback.Category category, String content, boolean isPublic) {
        Feedback f = new Feedback();
        f.setTeacher(teacher);
        f.setStudent(student);
        f.setCategory(category);
        f.setContent(content);
        f.setPublic(isPublic);
        return f;
    }

    private Counseling counseling(User teacher, Student student, LocalDate date, String content, String nextPlan, Counseling.ShareScope scope) {
        Counseling c = new Counseling();
        c.setTeacher(teacher);
        c.setStudent(student);
        c.setDate(date);
        c.setContent(content);
        c.setNextPlan(nextPlan);
        c.setShareScope(scope);
        return c;
    }

    private StudentRecord record(Student student, int present, int absent, int late, String notes) {
        StudentRecord r = new StudentRecord(student);
        r.setAttendance(String.format("{\"present\":%d,\"absent\":%d,\"late\":%d}", present, absent, late));
        r.setSpecialNotes(notes);
        return r;
    }
}
