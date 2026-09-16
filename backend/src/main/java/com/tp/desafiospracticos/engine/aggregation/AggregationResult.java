package com.tp.desafiospracticos.engine.aggregation;

import com.tp.desafiospracticos.engine.domain.Verdict;

public record AggregationResult(int quality, Verdict suggestedVerdict) {
}
