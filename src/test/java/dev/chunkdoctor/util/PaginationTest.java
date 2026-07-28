package dev.chunkdoctor.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationTest {
    @Test
    void pageClampsAndReturnsFinalPartialPage() {
        Pagination.Page<Integer> page = Pagination.page(List.of(1, 2, 3, 4, 5), 99, 2);
        assertEquals(3, page.number());
        assertEquals(3, page.totalPages());
        assertEquals(List.of(5), page.items());
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> Pagination.page(List.of(), 1, 0));
    }
}
