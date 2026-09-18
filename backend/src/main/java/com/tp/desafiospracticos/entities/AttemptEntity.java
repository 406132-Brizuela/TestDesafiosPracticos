package com.tp.desafiospracticos.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practical_challenge_id")
    private PracticalChallengeEntity practicalChallenge;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String answerCode;

    private LocalDateTime creationDatetime;

    private LocalDateTime submissionDatetime;

    private UUID llmConversationId;

    @ToString.Exclude
    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExecutionEntity> executions = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeacherCorrectionEntity> teacherCorrections = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LlmCorrectionEntity> llmCorrections = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EngineCorrectionEntity> engineCorrections = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OriginalityAlertEntity> originalityAlerts = new ArrayList<>();
}
