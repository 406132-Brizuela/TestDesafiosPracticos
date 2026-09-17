package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptJpaRepository extends JpaRepository<AttemptEntity, String> {

    List<AttemptEntity> findAllByOrderByCreationDatetimeDesc();

    List<AttemptEntity> findAllByUserIdOrderByCreationDatetimeDesc(String userId);
}
