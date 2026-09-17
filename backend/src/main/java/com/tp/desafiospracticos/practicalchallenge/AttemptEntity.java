package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "attempts")
public class AttemptEntity implements Persistable<String> {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false, length = 36)
    private String id;

    // Persistable: distingue "nuevo" de "existente" para que Spring Data JPA
    // llame a persist() (falla rápido con id duplicado) en vez de merge()
    // en el primer save() de un intentoId que llega del cliente.
    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practical_challenge_id", nullable = false)
    private PracticalChallengeEntity practicalChallenge;

    // Referencia al repo/commit evaluado (DT-08, mismo shape que {repo, ref}).
    // Quedan null hasta que exista la integración real con GitHub.
    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "ref")
    private String ref;

    @Column(name = "creation_datetime", nullable = false)
    private Instant creationDatetime;

    @Column(name = "submission_datetime")
    private Instant submissionDatetime;

    @Column(name = "llm_conversation_id")
    private String llmConversationId;

    @Column(name = "user_id")
    private String userId;

    protected AttemptEntity() {
    }

    public AttemptEntity(String id, PracticalChallengeEntity practicalChallenge,
                         Instant creationDatetime, String userId) {
        this.id = id;
        this.practicalChallenge = practicalChallenge;
        this.creationDatetime = creationDatetime;
        this.userId = userId;
        this.isNew = true;
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

    public PracticalChallengeEntity getPracticalChallenge() {
        return practicalChallenge;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getRef() {
        return ref;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public Instant getSubmissionDatetime() {
        return submissionDatetime;
    }
}
