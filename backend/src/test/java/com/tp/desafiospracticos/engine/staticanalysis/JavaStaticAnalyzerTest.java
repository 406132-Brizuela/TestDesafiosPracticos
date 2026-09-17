package com.tp.desafiospracticos.engine.staticanalysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStaticAnalyzerTest {

    private final JavaStaticAnalyzer analyzer = new JavaStaticAnalyzer();

    @Test
    void detectaLoopsAnidadosTriplesYProfundidad() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 10; j++) {
                                for (int k = 0; k < 10; k++) {
                                    System.out.println(i + j + k);
                                }
                            }
                        }
                    }
                }
                """;

        StaticAnalysisResult result = analyzer.analyze("java", code);

        assertEquals(3, result.nestingDepthMax());
        assertEquals(1, result.antipatrones().size());
    }

    @Test
    void calculaComplejidadCiclomaticaPorMetodo() {
        String code = """
                public class Main {
                    public int clasificar(int n) {
                        if (n < 0 && n != -1) {
                            return -1;
                        } else if (n == 0) {
                            return 0;
                        }
                        for (int i = 0; i < n; i++) {
                            if (i % 2 == 0) {
                                continue;
                            }
                        }
                        return 1;
                    }
                }
                """;

        StaticAnalysisResult result = analyzer.analyze("java", code);

        assertTrue(result.complejidadCiclomaticaPorMetodo().get("clasificar") >= 5);
    }

    @Test
    void detectaNombresNoDescriptivos() {
        String code = """
                public class Main {
                    public void metodo() {
                        int tmp = 1;
                        int x = 2;
                        int contadorClaro = tmp + x;
                        System.out.println(contadorClaro);
                    }
                }
                """;

        StaticAnalysisResult result = analyzer.analyze("java", code);

        assertTrue(result.nombresNoDescriptivos().contains("tmp"));
        assertTrue(result.nombresNoDescriptivos().contains("x"));
        assertFalse(result.nombresNoDescriptivos().contains("contadorClaro"));
    }

    @Test
    void codigoQueNoParseaDevuelveResultadoVacio() {
        StaticAnalysisResult result = analyzer.analyze("java", "esto no es java valido {{{");

        assertEquals(StaticAnalysisResult.empty(), result);
    }
}
