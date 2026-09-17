package com.tp.desafiospracticos.engine;

import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.web.EvaluationRequest;

public interface EngineService {

    EvaluationResult evaluate(EvaluationRequest req);
}
