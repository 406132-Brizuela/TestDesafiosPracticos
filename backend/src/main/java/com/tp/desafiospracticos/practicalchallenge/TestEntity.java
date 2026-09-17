package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tests")
public class TestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Long id;

    @Lob
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "user_creator_id")
    private String userCreatorId;

    @Column(name = "creation_datetime", nullable = false)
    private Instant creationDatetime;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false)
    private String input;

    @Lob
    @Column(name = "expected_output", nullable = false)
    private String expectedOutput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestVisibility visibility;

    protected TestEntity() {
    }

    public TestEntity(String code, String userCreatorId, Instant creationDatetime, String name,
                      String input, String expectedOutput, TestVisibility visibility) {
        this.code = code;
        this.active = true;
        this.userCreatorId = userCreatorId;
        this.creationDatetime = creationDatetime;
        this.name = name;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.visibility = visibility;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInput() {
        return input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public TestVisibility getVisibility() {
        return visibility;
    }
}
