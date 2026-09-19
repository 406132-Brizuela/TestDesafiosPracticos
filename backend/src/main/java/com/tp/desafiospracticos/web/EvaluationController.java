package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.engine.EngineService;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
import com.tp.desafiospracticos.engine.metrics.CompileCheckResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/engine")
public class EvaluationController {

    private final EngineService engineService;

    public EvaluationController(EngineService engineService) {
        this.engineService = engineService;
    }

    /** Solo compilación: sin tests, sin dimensiones, sin quality. Ver POST /engine/evaluate para la evaluación completa. */
    @PostMapping("/compile")
    public CompileCheckResult compile(@RequestBody CompileCheckRequest request) {
        return engineService.compileOnly(request.lenguaje(), request.code());
    }

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@RequestBody EvaluationRequest request) {
        return engineService.evaluate(request);
    }
}
