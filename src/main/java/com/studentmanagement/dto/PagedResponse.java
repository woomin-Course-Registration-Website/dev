package com.studentmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답 래퍼.
 *
 * Spring Data의 {@link Page} JSON 직렬화는 PageImpl 내부 구조에 의존해 외부 API
 * 안정성이 떨어진다. 명시적인 래퍼를 두어 클라이언트 계약을 고정한다.
 *
 * 사용 예:
 * <pre>
 *   Page&lt;Student&gt; page = repository.findByFiltersPaged(...);
 *   return PagedResponse.of(page, StudentResponse::new);
 * </pre>
 */
@Getter
@Schema(description = "페이지네이션 응답")
public class PagedResponse<T> {
    @Schema(description = "현재 페이지의 항목 리스트")
    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (0-based)", example = "0")
    private final int page;

    @Schema(description = "페이지 크기", example = "20")
    private final int size;

    @Schema(description = "전체 항목 수", example = "157")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "8")
    private final int totalPages;

    @Schema(description = "현재 페이지가 첫 페이지인지 여부")
    private final boolean first;

    @Schema(description = "현재 페이지가 마지막 페이지인지 여부")
    private final boolean last;

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages,
                         boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <S, T> PagedResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
