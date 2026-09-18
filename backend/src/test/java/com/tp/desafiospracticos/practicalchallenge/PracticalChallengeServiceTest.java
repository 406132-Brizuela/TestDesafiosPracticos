package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.attemptdraft.AttemptDraftJpaRepository;
import com.tp.desafiospracticos.motor.MotorChallenge;
import com.tp.desafiospracticos.motor.MotorChallengeResolver;
import com.tp.desafiospracticos.motor.MotorDesafioClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({PracticalChallengeService.class, PracticalChallengeCatalog.class, AttemptService.class,
        MotorChallengeResolver.class, LoggingDesafioContenidoEventPublisher.class})
class PracticalChallengeServiceTest {

    private final Set<String> motorIds = new HashSet<>();

    @MockBean
    private MotorDesafioClient motorClient;

    @Autowired
    private PracticalChallengeService service;

    @Autowired
    private PracticalChallengeJpaRepository repository;

    @Autowired
    private PracticalChallengeCatalog catalog;

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private AttemptJpaRepository attemptRepository;

    @Autowired
    private AttemptDraftJpaRepository draftRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void configureMotorMock() {
        motorIds.clear();
        when(motorClient.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            motorIds.add(id);
            return new MotorChallenge(id, "Sumar dos números", Difficulty.BASICO);
        });
        when(motorClient.findAllByIds(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.Collection<String> ids = invocation.getArgument(0);
            return ids.stream()
                    .filter(motorIds::contains)
                    .map(id -> new MotorChallenge(id, "Sumar dos números", Difficulty.BASICO))
                    .toList();
        });
    }

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

        AttemptResponse started = attemptService.start(
                new AttemptCreateRequest(UUID.randomUUID().toString(), challenge.id()), null);
        attemptRepository.flush();
        List<AttemptResponse> attempts = attemptService.findAll(null);

        assertEquals("INICIADO", started.status());
        assertEquals(challenge.id(), started.practicalChallengeId());
        assertEquals(1, attempts.size());
        assertEquals("Sumar dos números", attempts.get(0).challengeTitle());
        assertEquals(1, countRows("attempts"));
    }

    @Test
    void devuelveElDetalleDeUnIntentoConElCodigoInicialYSinBorrador() {
        PracticalChallengeResponse challenge = service.create(request(), null);
        AttemptResponse started = attemptService.start(
                new AttemptCreateRequest(UUID.randomUUID().toString(), challenge.id()), null);
        attemptRepository.flush();

        AttemptDetailResponse detail = attemptService.findById(started.id(), null);

        assertEquals(started.id(), detail.id());
        assertEquals(challenge.id(), detail.practicalChallengeId());
        assertEquals("Sumar dos números", detail.challengeTitle());
        assertEquals("Leer dos números y mostrar su suma.", detail.statement());
        assertEquals("", detail.starterCode());
        assertNull(detail.draftCode());
        assertEquals("INICIADO", detail.status());
    }

    @Test
    void guardaUnBorradorYLoDevuelveEnElDetalleSinTocarElIntentoReal() {
        PracticalChallengeResponse challenge = service.create(request(), null);
        AttemptResponse started = attemptService.start(
                new AttemptCreateRequest(UUID.randomUUID().toString(), challenge.id()), null);
        attemptRepository.flush();

        AttemptDetailResponse afterSave = attemptService.saveDraft(
                started.id(), "public class Main { /* borrador */ }", null);
        draftRepository.flush();
        AttemptDetailResponse recovered = attemptService.findById(started.id(), null);

        assertEquals("public class Main { /* borrador */ }", afterSave.draftCode());
        assertEquals("public class Main { /* borrador */ }", recovered.draftCode());
        assertEquals(1, countRows("attempt_drafts"));
        assertEquals(1, countRows("attempts"));
    }

    @Test
    void sobreescribeElBorradorExistenteEnVezDeDuplicarLaFila() {
        PracticalChallengeResponse challenge = service.create(request(), null);
        AttemptResponse started = attemptService.start(
                new AttemptCreateRequest(UUID.randomUUID().toString(), challenge.id()), null);
        attemptRepository.flush();

        attemptService.saveDraft(started.id(), "version 1", null);
        draftRepository.flush();
        AttemptDetailResponse afterSecondSave = attemptService.saveDraft(started.id(), "version 2", null);
        draftRepository.flush();

        assertEquals("version 2", afterSecondSave.draftCode());
        assertEquals(1, countRows("attempt_drafts"));
    }

    @Test
    void rechazaConsultarOGuardarUnBorradorDeUnIntentoInexistente() {
        assertThrows(AttemptNotFoundException.class,
                () -> attemptService.findById("no-existe", null));
        assertThrows(AttemptNotFoundException.class,
                () -> attemptService.saveDraft("no-existe", "codigo", null));
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
    void degradaUnaFilaSinDatosDeMotorEnElListadoEnVezDeTumbarloEntero() {
        service.create(request(), "profesor-1");

        PracticalChallengeEntity sinDatosDeMotor = new PracticalChallengeEntity(
                UUID.randomUUID().toString(),
                catalog.defaultType(),
                "Desafío sin fila en el stub de Motor",
                "profesor-1",
                Instant.now()
        );
        repository.save(sinDatosDeMotor);
        repository.flush();

        List<PracticalChallengeSummaryResponse> summaries = service.findAll("profesor-1");

        assertEquals(2, summaries.size());
        PracticalChallengeSummaryResponse degraded = summaries.stream()
                .filter(summary -> summary.id().equals(sinDatosDeMotor.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(MotorChallengeResolver.FALLBACK_TITLE, degraded.title());
        assertNull(degraded.difficulty());
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
                UUID.randomUUID().toString(),
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
