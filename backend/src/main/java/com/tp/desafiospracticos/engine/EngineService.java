package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.metrics.CompileCheckResult;
import com.tp.desafiospracticos.web.EvaluationRequest;

import java.util.List;

public interface EngineService {

    EvaluationResult evaluate(EvaluationRequest req);

    /** Solo compilación (POST /engine/compile): sin tests, sin análisis estático, sin quality. */
    CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files);
}
