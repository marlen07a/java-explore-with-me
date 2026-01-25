package ru.practicum.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageableUtil {
    private PageableUtil() {
    }

    public static Pageable from(int from, int size) {
        int page = from / size;
        return PageRequest.of(page, size);
    }
}
