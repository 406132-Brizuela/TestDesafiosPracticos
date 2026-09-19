package com.tp.desafiospracticos.engine.metrics;

import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.SourceFile;

import java.util.List;

public interface SandboxClient {

    ExecutionMetrics run(String lenguaje, List<SourceFile> files, List<TestCase> tests);

    /** Solo compila (sin ejecutar tests). Usado por POST /engine/compile. */
    CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files);
}
