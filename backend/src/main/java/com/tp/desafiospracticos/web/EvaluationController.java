package com.tp.desafiospracticos.web;

import com.tp.desafiospracticos.engine.EngineService;
import com.tp.desafiospracticos.engine.domain.EvaluationResult;
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

    @PostMapping("/evaluate")
    public EvaluationResult evaluate(@RequestBody EvaluationRequest request) {
        return engineService.evaluate(request);
    }
}
