package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practical_challenges")
public class PracticalChallengeEntity implements Persistable<String> {

    @Id
    @Column(name = "practical_challenge_id", nullable = false, updatable = false, length = 36)
    private String id;

    // Persistable: distingue "nuevo" de "existente" para que Spring Data JPA
    // llame a persist() (falla rápido con id duplicado) en vez de merge()
    // (que pisaría en silencio files/versions con orphanRemoval=true) en el
    // primer save() de un desafioId/intentoId que llega del cliente.
    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_type_id", nullable = false)
    private ChallengeTypeEntity challengeType;

    @Lob
    @Column(nullable = false)
    private String statement;

    @Column(name = "user_creator_id")
    private String userCreatorId;

    @Column(name = "creation_datetime", nullable = false)
    private Instant creationDatetime;

    @OneToMany(mappedBy = "practicalChallenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChallengeVersionEntity> versions = new ArrayList<>();

    @OneToMany(mappedBy = "practicalChallenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChallengeFileEntity> files = new ArrayList<>();

    protected PracticalChallengeEntity() {
    }

    public PracticalChallengeEntity(String id, ChallengeTypeEntity challengeType, String statement,
                                    String userCreatorId, Instant creationDatetime) {
        this.id = id;
        this.challengeType = challengeType;
        this.statement = statement;
        this.userCreatorId = userCreatorId;
        this.creationDatetime = creationDatetime;
        this.isNew = true;
    }

    public void addVersion(ChallengeVersionEntity version) {
        version.attachTo(this);
        versions.add(version);
    }

    public void addFile(ChallengeFileEntity file) {
        file.attachTo(this);
        files.add(file);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public String getStatement() {
        return statement;
    }

    public ChallengeTypeEntity getChallengeType() {
        return challengeType;
    }

    public String getUserCreatorId() {
        return userCreatorId;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public List<ChallengeVersionEntity> getVersions() {
        return versions;
    }

    public List<ChallengeFileEntity> getFiles() {
        return files;
    }
}
