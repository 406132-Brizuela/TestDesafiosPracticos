package com.tp.desafiospracticos.practicalchallenge;

/**
 * El profesor carga un único archivo de código base por desafío (ver
 * CLAUDE.md, "Qué autoría el profesor vs. qué recibe el Sandbox"). Este
 * archivo se guarda siempre bajo el path fijo {@link #MAIN_FILE_PATH}.
 *
 * Centraliza acá la constante y la búsqueda del archivo principal para que
 * {@link PracticalChallengeService} y {@link AttemptService} no repitan el
 * mismo stream — antes cada uno tenía su propia copia.
 */
final class ChallengeMainFile {

    static final String MAIN_FILE_PATH = "Main.java";

    private ChallengeMainFile() {
    }

    /**
     * Busca el archivo por {@link #MAIN_FILE_PATH}; si no está, cae al
     * primer archivo cargado. Devuelve "" si el desafío no tiene archivos.
     */
    static String contentOf(PracticalChallengeEntity challenge) {
        return challenge.getFiles().stream()
                .filter(file -> MAIN_FILE_PATH.equals(file.getPath()))
                .findFirst()
                .or(() -> challenge.getFiles().stream().findFirst())
                .map(ChallengeFileEntity::getContent)
                .orElse("");
    }
}
