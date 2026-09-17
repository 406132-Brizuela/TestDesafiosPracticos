package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticalChallengeJpaRepository extends JpaRepository<PracticalChallengeEntity, String> {

    List<PracticalChallengeEntity> findAllByOrderByCreationDatetimeDesc();

    List<PracticalChallengeEntity> findAllByUserCreatorIdOrderByCreationDatetimeDesc(String userCreatorId);
}
