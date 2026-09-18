package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.ProfileDimensionWeightEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileDimensionWeightRepository extends JpaRepository<ProfileDimensionWeightEntity, UUID> {
}
