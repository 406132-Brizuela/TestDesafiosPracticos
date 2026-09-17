package com.tp.desafiospracticos.engine.aggregation;

import com.tp.desafiospracticos.engine.domain.Verdict;

import java.math.BigDecimal;

public record AggregationResult(BigDecimal quality, Verdict suggestedVerdict) {
}
