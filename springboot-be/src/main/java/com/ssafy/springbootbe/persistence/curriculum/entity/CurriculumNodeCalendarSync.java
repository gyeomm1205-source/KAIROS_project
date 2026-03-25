package com.ssafy.springbootbe.persistence.curriculum.entity;

import com.ssafy.springbootbe.persistence.curriculum.type.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "curriculum_node_calendar_sync")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CurriculumNodeCalendarSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "curriculum_node_calendar_sync_id")
    private Long curriculumNodeCalendarSyncId;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "curriculum_node_id", nullable = false)
    private CurriculumNode curriculumNode;

    @Column(name = "google_calendar_id", length = 255)
    private String googleCalendarId;

    @Column(name = "google_event_id", unique = true, length = 255)
    private String googleEventId;

    @Column(name = "google_etag", length = 255)
    private String googleEtag;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.NOT_SYNCED;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "last_google_updated_at")
    private LocalDateTime lastGoogleUpdatedAt;

    public void synced(String googleCalendarId, String googleEventId, String googleEtag) {
        this.googleCalendarId = googleCalendarId;
        this.googleEventId = googleEventId;
        this.googleEtag = googleEtag;
        this.syncStatus = SyncStatus.SYNCED;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public void syncFailed() {
        this.syncStatus = SyncStatus.SYNC_FAILED;
    }

    public void markNotSynced() {
        this.syncStatus = SyncStatus.NOT_SYNCED;
    }

    public void updateEtag(String googleEtag) {
        this.googleEtag = googleEtag;
        this.lastGoogleUpdatedAt = LocalDateTime.now();
    }
}
