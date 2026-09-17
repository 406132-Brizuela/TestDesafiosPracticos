package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PracticalChallengeCatalog {

    static final String JAVA_LANGUAGE_ID = "java";
    static final String JAVA_IO_PROFILE_ID = "java-io-v1";
    static final String ALGORITHM_TYPE_ID = "algorithms-auto-tests-v1";

    private final LanguageJpaRepository languageRepository;
    private final ProfileJpaRepository profileRepository;
    private final ChallengeTypeJpaRepository challengeTypeRepository;

    public PracticalChallengeCatalog(LanguageJpaRepository languageRepository,
                                     ProfileJpaRepository profileRepository,
                                     ChallengeTypeJpaRepository challengeTypeRepository) {
        this.languageRepository = languageRepository;
        this.profileRepository = profileRepository;
        this.challengeTypeRepository = challengeTypeRepository;
    }

    ChallengeTypeEntity defaultType() {
        LanguageEntity language = languageRepository.findById(JAVA_LANGUAGE_ID)
                .orElseGet(() -> languageRepository.save(new LanguageEntity(JAVA_LANGUAGE_ID, "JAVA")));
        ProfileEntity profile = profileRepository.findById(JAVA_IO_PROFILE_ID)
                .orElseGet(() -> profileRepository.save(new ProfileEntity(
                        JAVA_IO_PROFILE_ID,
                        "java-input-output",
                        null,
                        language,
                        1,
                        true
                )));
        return challengeTypeRepository.findById(ALGORITHM_TYPE_ID)
                .orElseGet(() -> challengeTypeRepository.save(new ChallengeTypeEntity(
                        ALGORITHM_TYPE_ID,
                        profile,
                        Instant.now(),
                        1,
                        true,
                        true
                )));
    }
}
