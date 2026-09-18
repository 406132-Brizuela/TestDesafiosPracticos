package com.tp.desafiospracticos.challenge;

import com.tp.desafiospracticos.practicalchallenge.ChallengeMainFile;
import com.tp.desafiospracticos.practicalchallenge.ChallengeVersionEntity;
import com.tp.desafiospracticos.practicalchallenge.ChallengeVersions;
import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeEntity;
import com.tp.desafiospracticos.practicalchallenge.PracticalChallengeJpaRepository;
import com.tp.desafiospracticos.practicalchallenge.TestEntity;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adaptador real de {@link ChallengeRepository}: resuelve un {@link Challenge} para el
 * engine a partir del contenido práctico persistido por creación de desafíos
 * (PracticalChallengeEntity + su versión vigente + los TestEntity de esa versión).
 * {@code challengeId} es el mismo {@code desafioId} real (Motor) — nunca uno generado acá.
 * <p>
 * {@code @Primary} para que {@code EngineServiceImpl} lo use en vez de
 * {@link InMemoryChallengeRepository} (que queda solo para tests unitarios que
 * construyen su propio {@code ChallengeRepository} a mano).
 * <p>
 * {@code @Transactional}: {@code PracticalChallengeEntity} tiene asociaciones LAZY
 * (challengeType→profile→language, versions, files) y el proyecto corre con
 * {@code open-in-view: false} en perfil local — si no se resuelven todas acá adentro,
 * revientan con {@code LazyInitializationException} apenas el método devuelve el
 * {@link Challenge} (que sí es seguro de usar afuera: es un record inmutable de Strings).
 */
@Repository
@Primary
public class JpaChallengeRepository implements ChallengeRepository {

    private final PracticalChallengeJpaRepository repository;

    public JpaChallengeRepository(PracticalChallengeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Challenge findById(String id) {
        PracticalChallengeEntity challenge = repository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException(id));

        List<TestCase> tests = ChallengeVersions.currentTests(challenge).stream()
                .map(ChallengeVersionEntity::getTest)
                .map(this::toTestCase)
                .toList();

        String lenguaje = challenge.getChallengeType().getProfile().getLanguage().getName();

        return new Challenge(
                challenge.getId(),
                challenge.getStatement(),
                lenguaje,
                ChallengeMainFile.contentOf(challenge),
                tests
        );
    }

    private TestCase toTestCase(TestEntity test) {
        return new TestCase(String.valueOf(test.getId()), test.getInput(), test.getExpectedOutput());
    }
}
