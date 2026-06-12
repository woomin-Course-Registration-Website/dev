package com.studentmanagement.analytics.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.dto.analytics.AnalyticsOverviewResponse;
import com.studentmanagement.dto.analytics.ChatResponse;
import com.studentmanagement.dto.analytics.StudentSummaryResponse;
import com.studentmanagement.dto.analytics.SubjectDistributionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 챗봇 서비스 (US-08-07, 선택) — Anthropic Claude 기반.
 *
 * 권한 경계(문서 §9.4): 본 기능은 컨트롤러에서 TEACHER/ADMIN으로 제한되며,
 * LLM에는 PII(연락처 등)가 아닌 **집계 분석 데이터만** 컨텍스트로 주입한다.
 * (function calling 대신, 권한 범위 내 분석 데이터를 시스템 프롬프트에 주입하는 방식 — 문서의 1안)
 *
 * LLM_API_KEY 미설정 시 비활성(503). 모델 기본값: claude-opus-4-8 (adaptive thinking).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private volatile AnthropicClient client;

    public ChatService(AnalyticsService analyticsService,
                       @Value("${anthropic.api-key:}") String apiKey,
                       @Value("${anthropic.model:claude-opus-4-8}") String model) {
        this.analyticsService = analyticsService;
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 학습 현황 질의에 답한다.
     * @param message   사용자 질문
     * @param studentId 특정 학생 한정(선택). null이면 전체 개요+과목 분포를 컨텍스트로 사용.
     */
    public ChatResponse ask(String message, Long studentId) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 챗봇이 비활성화되어 있습니다. 서버에 LLM_API_KEY를 설정하세요.");
        }

        String contextJson = buildContextJson(studentId);
        String system = """
                당신은 교사를 돕는 학습 분석 어시스턴트입니다.
                아래 [분석 데이터](JSON)에 근거해서만 한국어로 간결하고 정확하게 답하세요.

                데이터 구조 안내:
                - overview: 학생·과목 목록
                - subjectDistributions: 과목별 반 평균·점수 구간 분포·제출률
                - student(특정 학생) 또는 students[](전체): 학생별 상세
                  · gradeTrend: 학기별 평균 점수 추이
                  · subjectScores: 과목별 평균 점수 (낮을수록 약한 과목)
                  · attendance: 출석/결석/지각/출석률
                  · submission: 과제 제출률
                  · feedbackByCategory: 피드백 카테고리별 건수

                활용 지침:
                - "어떤 과목을 더 공부?" → subjectScores에서 평균이 낮은 과목을 짚고, subjectDistributions의 반 평균과 비교해 설명.
                - "성적 추이" → gradeTrend 사용. "출석/제출 문제 학생" → 전체 students[]를 비교.
                - 데이터에 없는 내용(연락처 등 개인정보)은 추측하지 말고 모른다고 답하세요.

                [분석 데이터]
                """ + contextJson;

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(2048L)
                    .thinking(ThinkingConfigAdaptive.builder().build())
                    .system(system)
                    .addUserMessage(message)
                    .build();

            Message response = client().messages().create(params);
            String answer = response.content().stream()
                    .flatMap(b -> b.text().stream())
                    .map(t -> t.text())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            return new ChatResponse(answer.isBlank() ? "(응답을 생성하지 못했습니다.)" : answer);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Throwable e) {
            // SDK가 던지는 Error(NoSuchMethodError 등)까지 잡아 깔끔한 502로 변환 + 전체 스택 로깅
            log.warn("[CHAT] LLM 호출 실패", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI 응답 생성 중 오류가 발생했습니다.");
        }
    }

    /** 권한 범위 내 분석 데이터를 JSON 컨텍스트로 구성 */
    private String buildContextJson(Long studentId) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            AnalyticsOverviewResponse overview = analyticsService.getOverview();
            ctx.put("overview", overview);

            // 과목별 반 평균·점수분포·제출률 — 항상 포함(개인 점수와 비교 근거 제공)
            List<SubjectDistributionResponse> subjects = new ArrayList<>();
            for (AnalyticsOverviewResponse.SubjectRef s : overview.subjects()) {
                subjects.add(analyticsService.getSubjectDistribution(s.id()));
            }
            ctx.put("subjectDistributions", subjects);

            if (studentId != null) {
                // 특정 학생 한정: 성적추이·과목별 점수·출결·제출·피드백 상세
                ctx.put("student", analyticsService.getStudentSummary(studentId));
            } else {
                // 전체: 모든 학생의 상세 요약 → 반 전체 비교·검색·추천 질의 가능
                List<StudentSummaryResponse> students = new ArrayList<>();
                for (AnalyticsOverviewResponse.StudentRef s : overview.students()) {
                    students.add(analyticsService.getStudentSummary(s.id()));
                }
                ctx.put("students", students);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ctx);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Anthropic 클라이언트 지연 생성(키가 있을 때만) */
    private AnthropicClient client() {
        AnthropicClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                }
                c = client;
            }
        }
        return c;
    }
}
