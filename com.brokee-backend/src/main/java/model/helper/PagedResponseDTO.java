package model.helper;

import java.util.List;

public record PagedResponseDTO<T>(
        List<T> items,
        int page,
        int size,
        long total
) {
}

