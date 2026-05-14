package com.studentmanagement.dto;

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
public class PagedResponse<T> {
    private final List<T> content;
    private final int page;          // 0-based
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
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
