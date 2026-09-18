package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.AttemptEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<AttemptEntity, UUID> {
}
