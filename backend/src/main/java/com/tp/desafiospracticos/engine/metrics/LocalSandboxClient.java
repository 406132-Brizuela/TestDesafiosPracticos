package com.tp.desafiospracticos.engine.metrics;

import com.tp.desafiospracticos.challenge.TestCase;
import com.tp.desafiospracticos.engine.SourceFile;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * Soporta submissions de un solo archivo (convencion: {@code public class Main}) o
 * multi-archivo ({@link SourceFile#path()} puede incluir subcarpetas de package). Con un solo
 * archivo, el comportamiento es identico al de antes de soportar multi-archivo.
 */
@Component
@Primary
public class LocalSandboxClient implements SandboxClient {

    private static final String MAIN_CLASS = "Main";
    private static final long TIMEOUT_SECONDS = 5;
    private static final String JAVA_HOME = System.getProperty("java.home");

    // javac -> "Main.java:6: error: ';' expected" (idem para "warning:"). Los archivos se
    // pasan a javac con su path RELATIVO al workDir (nunca absoluto), asi que no hay que
    // lidiar con el ':' de la letra de unidad de Windows en el prefijo.
    private static final Pattern JAVAC_DIAGNOSTIC =
            Pattern.compile("^(.*\\.java):(\\d+):\\s*(error|warning):\\s*(.*)$");

    private static final Pattern MAIN_METHOD =
            Pattern.compile("\\b(?:public\\s+static|static\\s+public)\\s+void\\s+main\\s*\\(");

    @Override
    public CompileCheckResult compileOnly(String lenguaje, List<SourceFile> files) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory("sandbox-compile-");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio temporal del sandbox", e);
        }
        try {
            writeFiles(workDir, files);
            return compileWithDiagnostics(workDir, files);
        } catch (IOException e) {
            throw new IllegalStateException("Error de I/O compilando en el sandbox local", e);
        } finally {
            deleteRecursively(workDir);
        }
    }

    @Override
    public ExecutionMetrics run(String lenguaje, List<SourceFile> files, List<TestCase> tests) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory("sandbox-");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio temporal del sandbox", e);
        }

        try {
            writeFiles(workDir, files);

            if (!compileWithDiagnostics(workDir, files).compiles()) {
                return new ExecutionMetrics(false, tests.size(), 0, 0L, false, List.of());
            }

            String mainClass = resolveMainClass(files);

            List<TestResult> results = new ArrayList<>();
            int testsPassed = 0;
            boolean timedOut = false;
            long startNanos = System.nanoTime();
            for (TestCase testCase : tests) {
                TestResult result = runTestCase(workDir, mainClass, testCase);
                if (result.passed()) {
                    testsPassed++;
                }
                if ("TIMEOUT".equals(result.obtained())) {
                    timedOut = true;
                }
                results.add(result);
            }
            long executionTimeMs = (System.nanoTime() - startNanos) / 1_000_000;

            return new ExecutionMetrics(true, tests.size(), testsPassed, executionTimeMs, timedOut, results);
        } catch (IOException e) {
            throw new IllegalStateException("Error de I/O ejecutando el sandbox local", e);
        } finally {
            deleteRecursively(workDir);
        }
    }

    /**
     * Escribe cada archivo en el temp dir respetando su {@code path} (incluidas subcarpetas
     * de package). Se valida que el path resuelto no escape del temp dir: el codigo del
     * submission no es de confianza (ver DEV ONLY / INSEGURO en la clase).
     */
    private void writeFiles(Path workDir, List<SourceFile> files) throws IOException {
        Path workDirNormalizado = workDir.normalize();
        for (SourceFile file : files) {
            Path destino = workDir.resolve(file.path()).normalize();
            if (!destino.startsWith(workDirNormalizado)) {
                throw new IllegalArgumentException("Path de archivo invalido: " + file.path());
            }
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            Files.writeString(destino, file.content(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Mismo paso de compilación que usa {@link #run}, pero además captura y parsea la
     * salida de javac en {@link CompileDiagnostic} cuando no compila. El orden importa:
     * esperamos a que el proceso termine (con timeout) ANTES de leer stdout, para no
     * arriesgar bloquear el hilo si javac quedara colgado sin cerrar el stream.
     */
    private CompileCheckResult compileWithDiagnostics(Path workDir, List<SourceFile> files) throws IOException {
        List<String> comando = new ArrayList<>();
        comando.add(toolPath("javac"));
        for (SourceFile file : files) {
            comando.add(file.path());
        }

        Process process = new ProcessBuilder(comando)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new CompileCheckResult(false,
                        List.of(new CompileDiagnostic(0, "javac no respondió a tiempo.")));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new CompileCheckResult(false, List.of(new CompileDiagnostic(0, "Compilación interrumpida.")));
        }
        boolean compiles = process.exitValue() == 0;
        if (compiles) {
            return new CompileCheckResult(true, List.of());
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new CompileCheckResult(false, parseDiagnostics(output));
    }

    private List<CompileDiagnostic> parseDiagnostics(String javacOutput) {
        List<CompileDiagnostic> diagnostics = new ArrayList<>();
        for (String rawLine : javacOutput.split("\n")) {
            Matcher matcher = JAVAC_DIAGNOSTIC.matcher(rawLine.strip());
            if (matcher.matches()) {
                String path = matcher.group(1);
                int line = Integer.parseInt(matcher.group(2));
                String message = matcher.group(3) + ": " + matcher.group(4);
                diagnostics.add(new CompileDiagnostic(path, line, message));
            }
        }
        if (diagnostics.isEmpty()) {
            diagnostics.add(new CompileDiagnostic(0, javacOutput.isBlank() ? "Error de compilación." : javacOutput.strip()));
        }
        return diagnostics;
    }

    /**
     * Si hay exactamente un archivo con {@code public static void main}, se ejecuta esa clase
     * (fully-qualified segun su path/package). Si no, se cae a la convencion previa: la clase
     * {@value #MAIN_CLASS}. Con un solo archivo ("Main.java") ambos caminos coinciden.
     */
    private String resolveMainClass(List<SourceFile> files) {
        List<SourceFile> conMain = files.stream()
                .filter(file -> MAIN_METHOD.matcher(file.content()).find())
                .toList();

        if (conMain.size() == 1) {
            return fullyQualifiedClassName(conMain.get(0).path());
        }

        return files.stream()
                .filter(file -> MAIN_CLASS.equals(simpleClassName(file.path())))
                .findFirst()
                .map(file -> fullyQualifiedClassName(file.path()))
                .orElse(MAIN_CLASS);
    }

    private String fullyQualifiedClassName(String path) {
        String normalizado = path.replace('\\', '/');
        if (normalizado.endsWith(".java")) {
            normalizado = normalizado.substring(0, normalizado.length() - ".java".length());
        }
        return normalizado.replace('/', '.');
    }

    private String simpleClassName(String path) {
        String fqcn = fullyQualifiedClassName(path);
        int punto = fqcn.lastIndexOf('.');
        return punto >= 0 ? fqcn.substring(punto + 1) : fqcn;
    }

    private TestResult runTestCase(Path workDir, String mainClass, TestCase testCase) {
        Process process;
        try {
            process = new ProcessBuilder(toolPath("java"), "-cp", workDir.toString(), mainClass)
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
