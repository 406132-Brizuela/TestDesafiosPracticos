package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "languages")
public class LanguageEntity {

    @Id
    @Column(name = "language_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    protected LanguageEntity() {
    }

    public LanguageEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
