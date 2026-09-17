package com.tp.desafiospracticos.engine.dimension;

import com.tp.desafiospracticos.engine.domain.CorrectionDimension;
import com.tp.desafiospracticos.engine.domain.DimensionSource;

/**
 * Contrato comun a todas las dimensiones de evaluacion (correctness, performance,
 * complexity, style). Cada evaluador se calcula igual sin conocer el perfil salvo para leer
 * su propio peso y sus propios parametros; el peso se aplica al agregar.
 */
public interface Evaluator {

    String dimensionId();

    DimensionSource source();

    CorrectionDimension evaluate(EvaluationContext ctx);
}
