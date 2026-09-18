package com.tp.desafiospracticos.repositories;

import com.tp.desafiospracticos.entities.LanguageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<LanguageEntity, UUID> {
}
