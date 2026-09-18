package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.TestCaseEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, UUID> {
}
