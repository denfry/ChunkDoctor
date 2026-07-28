package dev.chunkdoctor.util;

import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static <T> Page<T> page(List<T> items, int requestedPage, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        int pages = Math.max(1, (items.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(pages, requestedPage));
        int from = Math.min(items.size(), (page - 1) * pageSize);
        int to = Math.min(items.size(), from + pageSize);
        return new Page<>(page, pages, items.subList(from, to));
    }

    public record Page<T>(int number, int totalPages, List<T> items) {
        public Page {
            items = List.copyOf(items);
        }
    }
}
