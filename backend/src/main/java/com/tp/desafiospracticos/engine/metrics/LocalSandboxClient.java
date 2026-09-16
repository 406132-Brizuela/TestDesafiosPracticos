package com.tp.desafiospracticos.engine.metrics;

import com.tp.desafiospracticos.challenge.TestCase;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * DEV ONLY / INSEGURO — ejecucion local sin aislamiento.
 * <p>
 * Compila y corre el codigo recibido directamente en el mismo proceso/maquina que corre este
 * backend: sin contenedor, sin limites de memoria/CPU, con acceso al filesystem y la red del
 * host. Sirve unicamente para probar el flujo end-to-end del MVP con un desafio de confianza.
 * Reemplazar por un sandbox aislado real (contenedor efimero, gVisor, firecracker, etc.) antes
 * de aceptar codigo de usuarios no confiables.
 * <p>
 * Asume que el codigo define {@code public class Main} con un {@code main(String[])} que lee
 * de entrada estandar.
 */
@Component
@Primary
public class LocalSandboxClient implements SandboxClient {

    private static final String MAIN_CLASS = "Main";
    private static final long TIMEOUT_SECONDS = 5;
    private static final String JAVA_HOME = System.getProperty("java.home");

    @Override
    public ExecutionMetrics run(String lenguaje, String code, List<TestCase> tests) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory("sandbox-");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio temporal del sandbox", e);
        }

        try {
            Path sourceFile = workDir.resolve(MAIN_CLASS + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            if (!compile(workDir, sourceFile)) {
                return new ExecutionMetrics(false, tests.size(), 0, List.of());
            }

            List<TestResult> results = new ArrayList<>();
            int testsPassed = 0;
            for (TestCase testCase : tests) {
                TestResult result = runTestCase(workDir, testCase);
                if (result.passed()) {
                    testsPassed++;
                }
                results.add(result);
            }

            return new ExecutionMetrics(true, tests.size(), testsPassed, results);
        } catch (IOException e) {
            throw new IllegalStateException("Error de I/O ejecutando el sandbox local", e);
        } finally {
            deleteRecursively(workDir);
        }
    }

    private boolean compile(Path workDir, Path sourceFile) throws IOException {
        Process process = new ProcessBuilder(toolPath("javac"), sourceFile.toString())
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return false;
        }
    }

    private TestResult runTestCase(Path workDir, TestCase testCase) {
        Process process;
        try {
            process = new ProcessBuilder(toolPath("java"), "-cp", workDir.toString(), MAIN_CLASS)
                    .directory(workDir.toFile())
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            return new TestResult(testCase.id(), false, testCase.expected(), "ERROR: " + e.getMessage());
        }

        try (var stdin = process.getOutputStream()) {
            stdin.write(testCase.input().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // El proceso puede cerrar stdin si no lo lee (p. ej. no compila bien la lectura); se ignora
            // y se deja que el timeout/lectura de stdout mas abajo determine el resultado.
        }

        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new TestResult(testCase.id(), false, testCase.expected(), "TIMEOUT");
            }

            String obtained = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            boolean passed = obtained.equals(testCase.expected().trim());
            return new TestResult(testCase.id(), passed, testCase.expected(), obtained);
        } catch (IOException e) {
            return new TestResult(testCase.id(), false, testCase.expected(), "ERROR: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new TestResult(testCase.id(), false, testCase.expected(), "INTERRUMPIDO");
        }
    }

    private static String toolPath(String tool) {
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? tool + ".exe" : tool;
        return Path.of(JAVA_HOME, "bin", exe).toString();
    }

    private void deleteRecursively(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort: es un dir temporal, el SO lo termina limpiando igual
                }
            });
        } catch (IOException ignored) {
            // idem
        }
    }
}
