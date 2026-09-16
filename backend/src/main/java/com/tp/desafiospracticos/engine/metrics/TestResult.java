package com.tp.desafiospracticos.engine.metrics;

public record TestResult(String caseId, boolean passed, String expected, String obtained) {
}
