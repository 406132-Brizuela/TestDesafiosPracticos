package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practical_challenges")
public class PracticalChallengeEntity {

    @Id
    @Column(name = "practical_challenge_id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "challenge_id", length = 36)
    private String challengeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_type_id", nullable = false)
    private ChallengeTypeEntity challengeType;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Lob
    @Column(nullable = false)
    private String template;

    @Column(name = "user_creator_id")
    private String userCreatorId;

    @Column(name = "creation_datetime", nullable = false)
    private Instant creationDatetime;

    @OneToMany(mappedBy = "practicalChallenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChallengeVersionEntity> versions = new ArrayList<>();

    protected PracticalChallengeEntity() {
    }

    public PracticalChallengeEntity(String id, ChallengeTypeEntity challengeType, String title, String statement,
                                    Difficulty difficulty, String template, String userCreatorId,
                                    Instant creationDatetime) {
        this.id = id;
        this.challengeType = challengeType;
        this.title = title;
        this.statement = statement;
        this.difficulty = difficulty;
        this.template = template;
        this.userCreatorId = userCreatorId;
        this.creationDatetime = creationDatetime;
    }

    public void addVersion(ChallengeVersionEntity version) {
        version.attachTo(this);
        versions.add(version);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatement() {
        return statement;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public ChallengeTypeEntity getChallengeType() {
        return challengeType;
    }

    public String getTemplate() {
        return template;
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
}
