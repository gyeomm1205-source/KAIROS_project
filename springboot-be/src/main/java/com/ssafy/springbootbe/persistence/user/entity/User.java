package com.ssafy.springbootbe.persistence.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private UserPosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.GUEST;

    @Column(name = "calendar_sync_enabled", nullable = false)
    @Builder.Default
    private Boolean calendarSyncEnabled = false;

    @Column(name = "dark_mode_enabled", nullable = false)
    @Builder.Default
    private Boolean darkModeEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateProfile(String nickname, UserPosition position, Boolean calendarSyncEnabled) {
        if (nickname != null) this.nickname = nickname;
        if (position != null) this.position = position;
        if (calendarSyncEnabled != null) this.calendarSyncEnabled = calendarSyncEnabled;
    }

    public void updateDarkMode(Boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}
