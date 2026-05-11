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
    private long totalElement;
    private int totalPages;
    private int pageSize;

    @Builder.Default
    private List<T> content = Collections.emptyList();
}
