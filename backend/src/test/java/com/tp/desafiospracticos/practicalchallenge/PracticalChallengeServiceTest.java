package com.tp.desafiospracticos.practicalchallenge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({PracticalChallengeService.class, PracticalChallengeCatalog.class, AttemptService.class})
class PracticalChallengeServiceTest {

    @Autowired
    private PracticalChallengeService service;

    @Autowired
    private PracticalChallengeJpaRepository repository;

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private AttemptJpaRepository attemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guardaYRecuperaElDesafioConTodosSusCasos() {
        PracticalChallengeRequest request = request();

        PracticalChallengeResponse created = service.create(request, "profesor-1");
        repository.flush();
        PracticalChallengeResponse recovered = service.findById(created.id());

        assertEquals(1, repository.count());
        assertEquals("Sumar dos números", recovered.title());
        assertEquals(2, recovered.testCases().size());
        assertEquals("", recovered.testCases().get(1).input());
        assertEquals("", recovered.testCases().get(1).expectedOutput());
        assertEquals(TestVisibility.PRIVADO, recovered.testCases().get(1).visibility());
        assertEquals(2, countRows("challenge_versions"));
        assertEquals(2, countRows("tests"));
        assertEquals(1, countRows("languages"));
        assertEquals(1, countRows("profiles"));
        assertEquals(1, countRows("challenge_types"));
    }

    @Test
    void creaYListaIntentosSinEjecutarElDesafio() {
        PracticalChallengeResponse challenge = service.create(request(), null);

        AttemptResponse started = attemptService.start(challenge.id(), null);
        attemptRepository.flush();
        List<AttemptResponse> attempts = attemptService.findAll(null);

        assertEquals("INICIADO", started.status());
        assertEquals(challenge.id(), started.practicalChallengeId());
        assertEquals(1, attempts.size());
        assertEquals("Sumar dos números", attempts.get(0).challengeTitle());
        assertEquals(1, countRows("attempts"));
    }

    @Test
    void filtraLosDesafiosPorElCreadorAutenticado() {
        service.create(request(), "profesor-1");
        service.create(request(), "profesor-2");

        List<PracticalChallengeSummaryResponse> ownChallenges = service.findAll("profesor-1");

        assertEquals(1, ownChallenges.size());
        assertEquals("Sumar dos números", ownChallenges.get(0).title());
        assertEquals(2, ownChallenges.get(0).testCount());
    }

    @Test
    void creaLasTablasDelSectorG05DelDiagrama() {
        Set<String> tables = Set.copyOf(jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'",
                String.class
        ));

        assertTrue(tables.containsAll(Set.of(
                "LANGUAGES",
                "PROFILES",
                "CHALLENGE_TYPES",
                "PRACTICAL_CHALLENGES",
                "TESTS",
                "CHALLENGE_VERSIONS",
                "ATTEMPTS"
        )));
    }

    private PracticalChallengeRequest request() {
        return new PracticalChallengeRequest(
                "Sumar dos números",
                "Leer dos números y mostrar su suma.",
                Difficulty.BASICO,
                ChallengeType.ALGORITMOS_CON_PRUEBAS_AUTOMATICAS,
                ProgrammingLanguage.JAVA,
                "",
                List.of(
                        new PracticalChallengeRequest.TestCaseRequest(
                                "Caso público", "2 3", "5", TestVisibility.PUBLICO),
                        new PracticalChallengeRequest.TestCaseRequest(
                                "Caso vacío", "", "", TestVisibility.PRIVADO)
                )
        );
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
