package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;

/**
 * Contrato comun a todas las dimensiones de evaluacion (correctness, performance,
 * complexity, style, ...). Por ahora solo existe {@link CorrectnessEvaluator}; las demas
 * dimensiones se suman implementando esta misma interfaz.
 */
public interface Evaluator {

    String dimensionId();

    CorrectionDimension evaluate(EvaluationContext ctx);
}
