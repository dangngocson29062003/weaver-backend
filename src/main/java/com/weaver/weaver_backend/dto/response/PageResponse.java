package com.weaver.weaver_backend.dto.response;

import lombok.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {
    private int currentPage;
    private long totalElements;
    private int totalPages;
    private int pageSize;
    private boolean isLast;

    @Builder.Default
    private List<T> content = Collections.emptyList();
}
