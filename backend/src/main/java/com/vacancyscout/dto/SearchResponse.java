package com.vacancyscout.dto;

import java.util.List;

public record SearchResponse<T>(long total, List<T> results, int page, int pageSize) {}
