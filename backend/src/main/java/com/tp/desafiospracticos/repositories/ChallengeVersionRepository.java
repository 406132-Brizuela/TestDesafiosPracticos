package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.ChallengeVersionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeVersionRepository extends JpaRepository<ChallengeVersionEntity, UUID> {
}
