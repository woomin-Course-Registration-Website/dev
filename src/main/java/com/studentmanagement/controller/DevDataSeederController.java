package com.studentmanagement.controller;

import com.studentmanagement.domain.Counseling;
import com.studentmanagement.domain.Feedback;
import com.studentmanagement.domain.Grade;
import com.studentmanagement.domain.Student;
import com.studentmanagement.domain.StudentRecord;
import com.studentmanagement.domain.Subject;
import com.studentmanagement.domain.User;
import com.studentmanagement.dto.ApiResponse;
import com.studentmanagement.repository.CounselingRepository;
import com.studentmanagement.repository.FeedbackRepository;
import com.studentmanagement.repository.GradeRepository;
import com.studentmanagement.repository.StudentRecordRepository;
import com.studentmanagement.repository.StudentRepository;
import com.studentmanagement.repository.SubjectRepository;
import com.studentmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Profile("!prod")
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
        User teacher1 = upsertUser("teacher1@school.com", "Kim Minji", User.Role.TEACHER);
        User teacher2 = upsertUser("teacher2@school.com", "Lee Seojun", User.Role.TEACHER);
        User parent1 = upsertUser("parent1@school.com", "Park Parent", User.Role.PARENT);
        User parent2 = upsertUser("parent2@school.com", "Choi Parent", User.Role.PARENT);

        User studentUser1 = upsertUser("student1@school.com", "Park Jisoo", User.Role.STUDENT);
        User studentUser2 = upsertUser("student2@school.com", "Choi Yuna", User.Role.STUDENT);
        User studentUser3 = upsertUser("student3@school.com", "Jung Hajun", User.Role.STUDENT);
        User studentUser4 = upsertUser("student4@school.com", "Kang Seoyeon", User.Role.STUDENT);
        User studentUser5 = upsertUser("student5@school.com", "Oh Doyun", User.Role.STUDENT);
        upsertUser("admin@school.com", "Admin", User.Role.ADMIN);

        Student s1 = upsertStudent("Park Jisoo", 1, 1, 1, studentUser1, List.of(parent1));
        Student s2 = upsertStudent("Choi Yuna", 1, 1, 2, studentUser2, List.of(parent2));
        Student s3 = upsertStudent("Jung Hajun", 1, 2, 1, studentUser3, List.of());
        Student s4 = upsertStudent("Kang Seoyeon", 2, 1, 1, studentUser4, List.of());
        Student s5 = upsertStudent("Oh Doyun", 2, 2, 3, studentUser5, List.of());
        Student s6 = upsertStudent("Han Suho", 3, 1, 2, null, List.of());
        Student s7 = upsertStudent("Seo Taewon", 3, 2, 1, null, List.of());
        List<Student> students = List.of(s1, s2, s3, s4, s5, s6, s7);

        Subject math = upsertSubject("Mathematics");
        Subject english = upsertSubject("English");
        Subject korean = upsertSubject("Korean");
        Subject science = upsertSubject("Science");
        Subject history = upsertSubject("History");
        List<Subject> subjects = List.of(math, english, korean, science, history);

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

        int feedbackCount = seedFeedbacks(teacher1, teacher2, students);
        int counselingCount = seedCounselings(teacher1, teacher2, students);
        int recordCount = seedRecords(students);

        return ApiResponse.ok(Map.of(
            "users", (int) userRepository.count(),
            "students", (int) studentRepository.count(),
            "subjects", (int) subjectRepository.count(),
            "gradesCreated", gradeCount,
            "feedbacksCreated", feedbackCount,
            "counselingsCreated", counselingCount,
            "studentRecordsCreated", recordCount
        ));
    }

    private User upsertUser(String email, String name, User.Role role) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(new User(email, passwordEncoder.encode("password123"), name, role)));
    }

    private Student upsertStudent(String name, int grade, int classNum, int studentNum, User user, List<User> parents) {
        Student student = studentRepository.findByGradeAndClassNumAndStudentNum(grade, classNum, studentNum)
            .orElseGet(() -> studentRepository.save(new Student(name, grade, classNum, studentNum)));

        boolean changed = false;
        if (student.getUser() == null && user != null) {
            student.setUser(user);
            changed = true;
        }
        for (User parent : parents) {
            if (student.getParents().add(parent)) {
                changed = true;
            }
        }
        return changed ? studentRepository.save(student) : student;
    }

    private Subject upsertSubject(String name) {
        return subjectRepository.findByName(name)
            .orElseGet(() -> subjectRepository.save(new Subject(name)));
    }

    private int saveGrade(Student student, Subject subject, int year, int semester, double score) {
        if (gradeRepository.findByStudentIdAndSubjectIdAndYearAndSemester(
                student.getId(), subject.getId(), year, semester).isPresent()) {
            return 0;
        }
        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setYear(year);
        grade.setSemester(semester);
        grade.setScore(BigDecimal.valueOf(score));
        grade.setGradeRank(calcRank(score));
        gradeRepository.save(grade);
        return 1;
    }

    private int seedFeedbacks(User teacher1, User teacher2, List<Student> students) {
        if (feedbackRepository.count() > 0) {
            return 0;
        }
        List<Feedback> feedbacks = List.of(
            feedback(teacher1, students.get(0), Feedback.Category.GRADE, "Mathematics performance improved steadily.", true),
            feedback(teacher1, students.get(0), Feedback.Category.ATTITUDE, "Participates actively and asks thoughtful questions.", true),
            feedback(teacher1, students.get(1), Feedback.Category.BEHAVIOR, "Works well with classmates and keeps class routines.", true),
            feedback(teacher1, students.get(1), Feedback.Category.GRADE, "English comprehension is strong.", true),
            feedback(teacher2, students.get(2), Feedback.Category.ATTENDANCE, "Several late arrivals need follow-up.", false),
            feedback(teacher2, students.get(3), Feedback.Category.OTHER, "Shows consistent effort in art club activities.", true),
            feedback(teacher1, students.get(4), Feedback.Category.GRADE, "Strong overall achievement with interest in science.", true)
        );
        feedbackRepository.saveAll(feedbacks);
        return feedbacks.size();
    }

    private int seedCounselings(User teacher1, User teacher2, List<Student> students) {
        if (counselingRepository.count() > 0) {
            return 0;
        }
        List<Counseling> counselings = List.of(
            counseling(teacher1, students.get(0), LocalDate.of(2024, 3, 15),
                "Set first-semester learning goals.", "Review progress before midterms.", Counseling.ShareScope.ALL),
            counseling(teacher1, students.get(1), LocalDate.of(2024, 4, 10),
                "Discussed English education track interests.", "Share related major information.", Counseling.ShareScope.PRIVATE),
            counseling(teacher2, students.get(2), LocalDate.of(2024, 3, 25),
                "Discussed late arrivals and commute constraints.", "Contact guardian and check route options.", Counseling.ShareScope.PRIVATE),
            counseling(teacher2, students.get(3), LocalDate.of(2024, 6, 5),
                "Discussed study focus for math and science.", "Recommend after-school study program.", Counseling.ShareScope.ALL),
            counseling(teacher1, students.get(4), LocalDate.of(2024, 7, 1),
                "Discussed science competition preparation.", "Check competition schedule.", Counseling.ShareScope.ALL)
        );
        counselingRepository.saveAll(counselings);
        return counselings.size();
    }

    private int seedRecords(List<Student> students) {
        int count = 0;
        count += saveRecord(students.get(0), 185, 2, 3, "Improved mathematical reasoning and science club participation.");
        count += saveRecord(students.get(1), 182, 3, 5, "Excellent English presentation performance and school band participation.");
        count += saveRecord(students.get(2), 178, 8, 12, "Active in reading debate club and school newspaper.");
        count += saveRecord(students.get(3), 183, 5, 7, "Participated in art club exhibition.");
        count += saveRecord(students.get(4), 186, 1, 2, "Strong science and mathematics achievement.");
        count += saveRecord(students.get(5), 170, 15, 20, "Needs continued support for attendance and school routines.");
        count += saveRecord(students.get(6), 184, 3, 4, "Class leader with steady participation in school events.");
        return count;
    }

    private int saveRecord(Student student, int present, int absent, int late, String notes) {
        if (studentRecordRepository.findByStudentId(student.getId()).isPresent()) {
            return 0;
        }
        StudentRecord record = new StudentRecord(student);
        record.setAttendance(String.format("{\"present\":%d,\"absent\":%d,\"late\":%d}", present, absent, late));
        record.setSpecialNotes(notes);
        studentRecordRepository.save(record);
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
        Feedback feedback = new Feedback();
        feedback.setTeacher(teacher);
        feedback.setStudent(student);
        feedback.setCategory(category);
        feedback.setContent(content);
        feedback.setPublic(isPublic);
        return feedback;
    }

    private Counseling counseling(User teacher, Student student, LocalDate date, String content, String nextPlan, Counseling.ShareScope scope) {
        Counseling counseling = new Counseling();
        counseling.setTeacher(teacher);
        counseling.setStudent(student);
        counseling.setDate(date);
        counseling.setContent(content);
        counseling.setNextPlan(nextPlan);
        counseling.setShareScope(scope);
        return counseling;
    }
}
