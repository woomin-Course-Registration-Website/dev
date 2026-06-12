package com.studentmanagement.analytics.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentmanagement.dto.analytics.AnalyticsOverviewResponse;
import com.studentmanagement.dto.analytics.ChatResponse;
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
                아래 [분석 데이터]에 근거해서만 한국어로 간결하고 정확하게 답하세요.
                데이터에 없는 내용은 추측하지 말고 모른다고 답하세요.
                점수·비율은 데이터의 값을 사용하고, 학생 개인정보(연락처 등)는 다루지 않습니다.

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
            if (studentId != null) {
                ctx.put("student", analyticsService.getStudentSummary(studentId));
            } else {
                AnalyticsOverviewResponse overview = analyticsService.getOverview();
                ctx.put("overview", overview);
                List<SubjectDistributionResponse> subjects = new ArrayList<>();
                for (AnalyticsOverviewResponse.SubjectRef s : overview.subjects()) {
                    subjects.add(analyticsService.getSubjectDistribution(s.id()));
                }
                ctx.put("subjectDistributions", subjects);
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
