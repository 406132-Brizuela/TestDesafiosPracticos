package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "challenge_versions")
public class ChallengeVersionEntity {

    @Id
    @Column(name = "challenge_version_id", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "test_id", nullable = false)
    private TestEntity test;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practical_challenge_id", nullable = false)
    private PracticalChallengeEntity practicalChallenge;

    @Column(nullable = false)
    private int version;

    @Column(name = "test_order", nullable = false)
    private int testOrder;

    protected ChallengeVersionEntity() {
    }

    public ChallengeVersionEntity(String id, TestEntity test, int version, int testOrder) {
        this.id = id;
        this.test = test;
        this.version = version;
        this.testOrder = testOrder;
    }

    void attachTo(PracticalChallengeEntity practicalChallenge) {
        this.practicalChallenge = practicalChallenge;
    }

    public TestEntity getTest() {
        return test;
    }

    public int getVersion() {
        return version;
    }

    public int getTestOrder() {
        return testOrder;
    }
}
