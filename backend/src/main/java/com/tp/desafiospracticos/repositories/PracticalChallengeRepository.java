package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.PracticalChallengeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticalChallengeRepository extends JpaRepository<PracticalChallengeEntity, UUID> {
}
