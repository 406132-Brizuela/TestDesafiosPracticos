package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.ChallengeTypeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeTypeRepository extends JpaRepository<ChallengeTypeEntity, UUID> {
}
