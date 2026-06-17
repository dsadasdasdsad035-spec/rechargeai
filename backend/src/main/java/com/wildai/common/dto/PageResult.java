package com.wildai.common.dto;

import java.util.List;

public record PageResult<T>(int pageNo, int pageSize, long total, List<T> items) {
}
