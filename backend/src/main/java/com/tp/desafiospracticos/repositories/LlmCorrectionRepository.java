package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.LlmCorrectionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmCorrectionRepository extends JpaRepository<LlmCorrectionEntity, UUID> {
}
