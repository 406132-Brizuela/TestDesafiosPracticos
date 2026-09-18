package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.OriginalityAlertEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginalityAlertRepository extends JpaRepository<OriginalityAlertEntity, UUID> {
}
