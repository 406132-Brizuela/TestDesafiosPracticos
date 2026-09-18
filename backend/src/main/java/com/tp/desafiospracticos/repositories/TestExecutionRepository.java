package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.TestExecutionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestExecutionRepository extends JpaRepository<TestExecutionEntity, UUID> {
}
