package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "challenge_types")
public class ChallengeTypeEntity {

    @Id
    @Column(name = "challenge_type_id", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "modification_datetime", nullable = false)
    private Instant modificationDatetime;

    @Column(nullable = false)
    private int version;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "allow_multiple", nullable = false)
    private boolean allowMultiple;

    protected ChallengeTypeEntity() {
    }

    public ChallengeTypeEntity(String id, ProfileEntity profile, Instant modificationDatetime,
                               int version, boolean active, boolean allowMultiple) {
        this.id = id;
        this.profile = profile;
        this.modificationDatetime = modificationDatetime;
        this.version = version;
        this.active = active;
        this.allowMultiple = allowMultiple;
    }

    public ProfileEntity getProfile() {
        return profile;
    }
}
