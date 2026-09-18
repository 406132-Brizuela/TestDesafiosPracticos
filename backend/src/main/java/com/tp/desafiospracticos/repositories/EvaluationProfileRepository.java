package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.EvaluationProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationProfileRepository extends JpaRepository<EvaluationProfileEntity, UUID> {
}
