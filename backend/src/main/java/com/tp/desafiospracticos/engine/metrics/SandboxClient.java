package com.tp.desafiospracticos.engine.metrics;

import com.tp.desafiospracticos.challenge.TestCase;

import java.util.List;

public interface SandboxClient {

    ExecutionMetrics run(String lenguaje, String code, List<TestCase> tests);
}
