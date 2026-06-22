package com.ondo.photo;

import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, ProfilePhoto.PhotoId> {

    Optional<ProfilePhoto> findByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    void deleteByOwnerKindAndOwnerId(OwnerKind ownerKind, Long ownerId);

    /** 목록 화면용 — 여러 소유자의 사진 갱신시각 일괄 조회(데이터 BLOB 은 로드하지 않도록 updatedAt 만). */
    List<ProfilePhoto> findByOwnerKindAndOwnerIdIn(OwnerKind ownerKind, List<Long> ownerIds);
}
