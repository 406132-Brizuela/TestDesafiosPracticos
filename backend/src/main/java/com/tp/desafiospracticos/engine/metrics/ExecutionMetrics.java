package com.tp.desafiospracticos.engine.metrics;

import java.util.List;

public record ExecutionMetrics(boolean compiled, int testsTotal, int testsPassed, List<TestResult> results) {
}
