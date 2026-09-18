package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.EngineCorrectionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineCorrectionRepository extends JpaRepository<EngineCorrectionEntity, UUID> {
}
