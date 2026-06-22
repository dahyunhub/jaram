package com.ondo.photo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 프로필 이미지(아이·교사). 테이블 profile_photo 는 Flyway V3 정본. 복합 PK(owner_kind, owner_id).
 * data 는 클라이언트가 1:1 크롭+리사이즈한 작은 이미지 바이트(JPEG/PNG/WebP).
 */
@Entity
@Table(name = "profile_photo")
@IdClass(ProfilePhoto.PhotoId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfilePhoto {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_kind", nullable = false, length = 10)
    private OwnerKind ownerKind;

    @Id
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProfilePhoto(OwnerKind ownerKind, Long ownerId, String contentType, byte[] data) {
        this.ownerKind = ownerKind;
        this.ownerId = ownerId;
        replace(contentType, data);
    }

    public static ProfilePhoto of(OwnerKind ownerKind, Long ownerId, String contentType, byte[] data) {
        return new ProfilePhoto(ownerKind, ownerId, contentType, data);
    }

    /** 같은 소유자의 사진 교체(업서트 시 기존 행 갱신). */
    public void replace(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 복합 PK 클래스. */
    public static class PhotoId implements Serializable {
        private OwnerKind ownerKind;
        private Long ownerId;

        public PhotoId() {
        }

        public PhotoId(OwnerKind ownerKind, Long ownerId) {
            this.ownerKind = ownerKind;
            this.ownerId = ownerId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PhotoId that)) return false;
            return ownerKind == that.ownerKind && Objects.equals(ownerId, that.ownerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownerKind, ownerId);
        }
    }
}
