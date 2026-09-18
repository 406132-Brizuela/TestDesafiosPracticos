package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.ChallengeProfileEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeProfileRepository extends JpaRepository<ChallengeProfileEntity, UUID> {
}
