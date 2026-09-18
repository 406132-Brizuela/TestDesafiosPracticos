package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.CorrectionDimensionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionDimensionRepository extends JpaRepository<CorrectionDimensionEntity, UUID> {
}
