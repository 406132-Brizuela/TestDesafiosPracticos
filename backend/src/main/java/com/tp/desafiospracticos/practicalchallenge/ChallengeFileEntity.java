package com.tp.desafiospracticos.practicalchallenge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "challenge_files")
public class ChallengeFileEntity {

    @Id
    @Column(name = "challenge_file_id", nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practical_challenge_id", nullable = false)
    private PracticalChallengeEntity practicalChallenge;

    @Column(nullable = false)
    private String path;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "file_order", nullable = false)
    private int fileOrder;

    protected ChallengeFileEntity() {
    }

    public ChallengeFileEntity(String id, String path, String content, int fileOrder) {
        this.id = id;
        this.path = path;
        this.content = content;
        this.fileOrder = fileOrder;
    }

    void attachTo(PracticalChallengeEntity practicalChallenge) {
        this.practicalChallenge = practicalChallenge;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public int getFileOrder() {
        return fileOrder;
    }
}
