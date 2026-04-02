package com.studentmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI 3) 설정 클래스
 *
 * - 접속 URL: http://localhost:8080/swagger-ui/index.html
 * - JWT 인증: 우측 상단 [Authorize] 버튼 → "Bearer {토큰}" 형식으로 입력
 *   예) Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 */
@Configuration
public class SwaggerConfig {

    /** SecurityScheme 이름 상수 — SecurityConfig의 permitAll 경로와 무관 */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("교사용 학생 관리 시스템 API")
                        .description("""
                                ## 학생 성적 및 상담 관리 시스템 REST API

                                ### 인증 방법
                                1. `POST /api/auth/login` 으로 로그인
                                2. 응답의 `accessToken` 값을 복사
                                3. 우측 상단 **🔒 Authorize** 버튼 클릭
                                4. `Bearer {토큰값}` 형식으로 입력 후 **Authorize** 클릭

                                ### 역할(Role) 별 접근 권한
                                | 역할 | 설명 |
                                |------|------|
                                | TEACHER | 학생 조회/등록, 성적/피드백/상담 관리 |
                                | STUDENT | 본인 성적·학생부·피드백(공개) 조회 |
                                | PARENT | 자녀 성적·학생부·피드백(공개) 조회 |
                                | ADMIN | 사용자 계정 관리, 과목 추가 |

                                ### 공통 응답 형식
                                ```json
                                { "success": true, "data": { ... }, "message": null }
                                ```
                                ### 에러 응답 형식
                                ```json
                                { "success": false, "error": "에러 메시지", "code": "ERROR_CODE" }
                                ```
                                """)
                        .version("v1.0")
                        .contact(new Contact().name("학생관리시스템 개발팀")))
                // 전역 JWT 보안 요구사항 — 모든 엔드포인트에 자물쇠 아이콘 표시
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급된 accessToken을 입력하세요. 'Bearer ' 접두사는 자동으로 붙습니다.")));
    }
}
