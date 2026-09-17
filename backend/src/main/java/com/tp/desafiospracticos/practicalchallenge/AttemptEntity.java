package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "attempts")
public class AttemptEntity {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practical_challenge_id", nullable = false)
    private PracticalChallengeEntity practicalChallenge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_code", nullable = false, columnDefinition = "json")
    private Map<String, Object> answerCode;

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

    public AttemptEntity(String id, PracticalChallengeEntity practicalChallenge, Map<String, Object> answerCode,
                         Instant creationDatetime, String userId) {
        this.id = id;
        this.practicalChallenge = practicalChallenge;
        this.answerCode = answerCode;
        this.creationDatetime = creationDatetime;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public PracticalChallengeEntity getPracticalChallenge() {
        return practicalChallenge;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public Instant getSubmissionDatetime() {
        return submissionDatetime;
    }
}
