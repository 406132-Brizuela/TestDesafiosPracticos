package com.tp.desafiospracticos.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AttemptEntity attempt;

    @Column(name = "sanbox_execution_id")
    private UUID sandboxExecutionId;

    @Enumerated(EnumType.STRING)
    private ExecutionType type;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private Double score;

    private Integer statusCode;

    private Double ramUsage;

    private Double cpuUsage;

    private Long compilationTime;

    private Long executionTime;

    private LocalDateTime creationDatetime;

    private Boolean cheatingSuspected;

    @ToString.Exclude
    @OneToMany(mappedBy = "execution", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TestExecutionEntity> testExecutions = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "execution", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EngineCorrectionEntity> engineCorrections = new ArrayList<>();
}
