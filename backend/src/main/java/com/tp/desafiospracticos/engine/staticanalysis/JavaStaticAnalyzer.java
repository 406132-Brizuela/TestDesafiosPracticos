package com.tp.desafiospracticos.engine.staticanalysis;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Analiza codigo Java con JavaParser: profundidad de anidamiento, complejidad ciclomatica
 * por metodo, anti-patron de loops anidados triples, nombres de variables/parametros y
 * longitud de metodos. Complexity y style consumen este resultado.
 */
@Component
public class JavaStaticAnalyzer implements StaticAnalyzer {

    private static final Pattern NOMBRE_NO_DESCRIPTIVO = Pattern.compile("^(tmp|aux\\d*|foo|bar)$");

    @Override
    public StaticAnalysisResult analyze(String lenguaje, String code) {
        if (!"java".equalsIgnoreCase(lenguaje)) {
            return StaticAnalysisResult.empty();
        }

        CompilationUnit unit;
        try {
            unit = StaticJavaParser.parse(code);
        } catch (ParseProblemException e) {
            return StaticAnalysisResult.empty();
        }

        int nestingDepthMax = 0;
        Map<String, Integer> complejidadPorMetodo = new LinkedHashMap<>();
        Map<String, Integer> lineasPorMetodo = new LinkedHashMap<>();
        List<String> antipatrones = new ArrayList<>();
        Set<String> nombresNoDescriptivos = new LinkedHashSet<>();

        for (MethodDeclaration metodo : unit.findAll(MethodDeclaration.class)) {
            String nombreMetodo = metodo.getNameAsString();
            complejidadPorMetodo.put(nombreMetodo, cyclomaticComplexity(metodo));
            metodo.getRange().ifPresent(r -> lineasPorMetodo.put(nombreMetodo, r.end.line - r.begin.line + 1));

            for (Parameter parametro : metodo.getParameters()) {
                agregarSiNoDescriptivo(parametro.getNameAsString(), nombresNoDescriptivos);
            }

            if (metodo.getBody().isPresent()) {
                NestingCounters counters = new NestingCounters();
                walk(metodo.getBody().get(), 0, counters);
                nestingDepthMax = Math.max(nestingDepthMax, counters.maxDepth);
                if (counters.tripleNestedLoop) {
                    antipatrones.add("Bucle anidado triple detectado en el metodo '" + nombreMetodo + "'");
                }
            }
        }

        for (VariableDeclarator variable : unit.findAll(VariableDeclarator.class)) {
            agregarSiNoDescriptivo(variable.getNameAsString(), nombresNoDescriptivos);
        }

        return new StaticAnalysisResult(
                nestingDepthMax,
                complejidadPorMetodo,
                antipatrones,
                List.copyOf(nombresNoDescriptivos),
                lineasPorMetodo
        );
    }

    private int cyclomaticComplexity(MethodDeclaration metodo) {
        int complejidad = 1;
        complejidad += metodo.findAll(IfStmt.class).size();
        complejidad += metodo.findAll(ForStmt.class).size();
        complejidad += metodo.findAll(ForEachStmt.class).size();
        complejidad += metodo.findAll(WhileStmt.class).size();
        complejidad += metodo.findAll(DoStmt.class).size();
        complejidad += metodo.findAll(CatchClause.class).size();
        complejidad += (int) metodo.findAll(SwitchEntry.class).stream()
                .filter(entry -> !entry.getLabels().isEmpty())
                .count();
        complejidad += metodo.findAll(ConditionalExpr.class).size();
        complejidad += (int) metodo.findAll(BinaryExpr.class).stream()
                .filter(bin -> bin.getOperator() == BinaryExpr.Operator.AND || bin.getOperator() == BinaryExpr.Operator.OR)
                .count();
        return complejidad;
    }

    /**
     * Recorrida manual (en vez de un visitor de JavaParser) para llevar profundidad de
     * anidamiento y contador de loops anidados a la vez, sin duplicar el recorrido.
     */
    private void walk(Statement stmt, int depth, NestingCounters counters) {
        if (stmt instanceof BlockStmt block) {
            for (Statement s : block.getStatements()) {
                walk(s, depth, counters);
            }
            return;
        }
        if (stmt instanceof IfStmt ifStmt) {
            counters.registrar(depth + 1);
            walk(ifStmt.getThenStmt(), depth + 1, counters);
            ifStmt.getElseStmt().ifPresent(e -> walk(e, depth + 1, counters));
            return;
        }
        if (isLoop(stmt)) {
            counters.registrar(depth + 1);
            counters.loopDepth++;
            if (counters.loopDepth >= 3) {
                counters.tripleNestedLoop = true;
            }
            walk(cuerpoDelLoop(stmt), depth + 1, counters);
            counters.loopDepth--;
            return;
        }
        if (stmt instanceof SwitchStmt switchStmt) {
            counters.registrar(depth + 1);
            for (SwitchEntry entry : switchStmt.getEntries()) {
                for (Statement s : entry.getStatements()) {
                    walk(s, depth + 1, counters);
                }
            }
            return;
        }
        if (stmt instanceof TryStmt tryStmt) {
            walk(tryStmt.getTryBlock(), depth, counters);
            for (CatchClause cc : tryStmt.getCatchClauses()) {
                walk(cc.getBody(), depth, counters);
            }
            tryStmt.getFinallyBlock().ifPresent(f -> walk(f, depth, counters));
        }
    }

    private boolean isLoop(Statement stmt) {
        return stmt instanceof ForStmt || stmt instanceof ForEachStmt || stmt instanceof WhileStmt || stmt instanceof DoStmt;
    }

    private Statement cuerpoDelLoop(Statement stmt) {
        if (stmt instanceof ForStmt forStmt) {
            return forStmt.getBody();
        }
        if (stmt instanceof ForEachStmt forEachStmt) {
            return forEachStmt.getBody();
        }
        if (stmt instanceof WhileStmt whileStmt) {
            return whileStmt.getBody();
        }
        if (stmt instanceof DoStmt doStmt) {
            return doStmt.getBody();
        }
        throw new IllegalArgumentException("No es un statement de loop: " + stmt);
    }

    private void agregarSiNoDescriptivo(String nombre, Set<String> acumulador) {
        if (nombre.length() == 1 || NOMBRE_NO_DESCRIPTIVO.matcher(nombre).matches()) {
            acumulador.add(nombre);
        }
    }

    private static class NestingCounters {
        int maxDepth = 0;
        int loopDepth = 0;
        boolean tripleNestedLoop = false;

        void registrar(int depth) {
            maxDepth = Math.max(maxDepth, depth);
        }
    }
}
