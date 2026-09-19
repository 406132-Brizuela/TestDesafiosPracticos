package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttemptJpaRepository extends JpaRepository<AttemptEntity, String> {

    List<AttemptEntity> findAllByOrderByCreationDatetimeDesc();

    List<AttemptEntity> findAllByUserIdOrderByCreationDatetimeDesc(String userId);

    @EntityGraph(attributePaths = {"practicalChallenge", "practicalChallenge.files"})
    @Query("select attempt from AttemptEntity attempt where attempt.id = :id")
    Optional<AttemptEntity> findWithChallengeById(@Param("id") String id);
}
