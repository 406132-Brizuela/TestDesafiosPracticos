package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class ProfileEntity {

    @Id
    @Column(name = "profile_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "sandbox_image_id")
    private String sandboxImageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private LanguageEntity language;

    @Column(nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected ProfileEntity() {
    }

    public ProfileEntity(String id, String name, String sandboxImageId, LanguageEntity language,
                         int version, boolean active) {
        this.id = id;
        this.name = name;
        this.sandboxImageId = sandboxImageId;
        this.language = language;
        this.version = version;
        this.active = active;
    }

    public LanguageEntity getLanguage() {
        return language;
    }
}
