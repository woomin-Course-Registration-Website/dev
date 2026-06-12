package com.studentmanagement.analytics.service;

import com.studentmanagement.analytics.domain.DimStudent;
import com.studentmanagement.analytics.domain.DimSubject;
import com.studentmanagement.analytics.domain.FactAttendance;
import com.studentmanagement.analytics.repository.*;
import com.studentmanagement.analytics.repository.projection.CategoryCount;
import com.studentmanagement.analytics.repository.projection.TermAverage;
import com.studentmanagement.dto.analytics.StudentSummaryResponse;
import com.studentmanagement.dto.analytics.SubjectDistributionResponse;
import com.studentmanagement.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock DimStudentRepository dimStudentRepository;
    @Mock DimSubjectRepository dimSubjectRepository;
    @Mock FactGradeRepository factGradeRepository;
    @Mock FactAttendanceRepository factAttendanceRepository;
    @Mock FactSubmissionRepository factSubmissionRepository;
    @Mock FactFeedbackRepository factFeedbackRepository;

    @InjectMocks AnalyticsService service;

    @Test
    void studentSummary_computesRatesAndTrend() {
        given(dimStudentRepository.findById(1L))
                .willReturn(Optional.of(new DimStudent(1L, "Park", 1, 1, 1)));
        given(factGradeRepository.findStudentTermAverages(1L))
                .willReturn(List.of(termAverage(2024, 1, 88.0), termAverage(2024, 2, 92.0)));
        given(factAttendanceRepository.findById(1L))
                .willReturn(Optional.of(new FactAttendance(1L, 90, 5, 5)));
        given(factSubmissionRepository.countByStudentId(1L)).willReturn(10L);
        given(factSubmissionRepository.countByStudentIdAndStatusIn(eq(1L), anyCollection())).willReturn(8L);
        given(factFeedbackRepository.findCategoryCounts(1L))
                .willReturn(List.of(categoryCount("GRADE", 3)));

        StudentSummaryResponse summary = service.getStudentSummary(1L);

        assertThat(summary.name()).isEqualTo("Park");
        assertThat(summary.gradeTrend()).hasSize(2);
        assertThat(summary.gradeTrend().get(0).avgScore()).isEqualTo(88.0);
        // 출석률 90/(90+5+5) = 0.9
        assertThat(summary.attendance().rate()).isEqualTo(0.9);
        // 제출률 8/10 = 0.8
        assertThat(summary.submission().rate()).isEqualTo(0.8);
        assertThat(summary.feedbackByCategory()).extracting(StudentSummaryResponse.CategoryCount::category)
                .containsExactly("GRADE");
    }

    @Test
    void studentSummary_unknownStudent_throws() {
        given(dimStudentRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.getStudentSummary(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void subjectDistribution_bucketizesScores() {
        given(dimSubjectRepository.findById(2L))
                .willReturn(Optional.of(new DimSubject(2L, "Math")));
        given(factGradeRepository.findSubjectAverage(2L)).willReturn(75.0);
        given(factGradeRepository.findScoresBySubjectId(2L))
                .willReturn(List.of(bd(55), bd(65), bd(75), bd(85), bd(95)));
        given(factSubmissionRepository.countBySubjectId(2L)).willReturn(10L);
        given(factSubmissionRepository.countBySubjectIdAndStatusIn(eq(2L), anyCollection())).willReturn(7L);

        SubjectDistributionResponse dist = service.getSubjectDistribution(2L);

        assertThat(dist.name()).isEqualTo("Math");
        assertThat(dist.average()).isEqualTo(75.0);
        // 각 구간에 1명씩
        assertThat(dist.distribution()).extracting(SubjectDistributionResponse.Bucket::count)
                .containsExactly(1L, 1L, 1L, 1L, 1L);
        assertThat(dist.submission().rate()).isEqualTo(0.7);
    }

    // ── helpers ──

    private BigDecimal bd(double v) { return BigDecimal.valueOf(v); }

    private TermAverage termAverage(int year, int semester, double avg) {
        return new TermAverage() {
            public int getYear() { return year; }
            public int getSemester() { return semester; }
            public Double getAvgScore() { return avg; }
        };
    }

    private CategoryCount categoryCount(String category, long cnt) {
        return new CategoryCount() {
            public String getCategory() { return category; }
            public long getCnt() { return cnt; }
        };
    }
}
