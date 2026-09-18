package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.ExecutionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRepository extends JpaRepository<ExecutionEntity, UUID> {
}
