package com.ondo.photo;

import com.ondo.child.ChildService;
import com.ondo.photo.domain.OwnerKind;
import com.ondo.photo.domain.ProfilePhoto;
import com.ondo.photo.dto.PhotoUploadedResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 프로필 이미지 API(아이·교사). 업로드는 크롭+리사이즈된 작은 이미지 바이트(image/jpeg|png|webp).
 * 소유권: 아이는 ChildService.assertOwnedChild, 교사는 본인(@AuthenticationPrincipal).
 */
@RestController
@RequestMapping("/api/v1")
public class PhotoController {

    private final ProfilePhotoService photoService;
    private final ChildService childService;

    public PhotoController(ProfilePhotoService photoService, ChildService childService) {
        this.photoService = photoService;
        this.childService = childService;
    }

    // ---------- 아이 ----------

    @PutMapping("/children/{childId}/photo")
    public PhotoUploadedResponse uploadChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                  @PathVariable Long childId,
                                                  @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                                  @RequestBody byte[] data) {
        childService.assertOwnedChild(teacherId, childId);
        return new PhotoUploadedResponse(photoService.save(OwnerKind.CHILD, childId, contentType, data));
    }

    @GetMapping("/children/{childId}/photo")
    public ResponseEntity<byte[]> getChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                @PathVariable Long childId) {
        childService.assertOwnedChild(teacherId, childId);
        return toResponse(photoService.find(OwnerKind.CHILD, childId));
    }

    @DeleteMapping("/children/{childId}/photo")
    public ResponseEntity<Void> deleteChildPhoto(@AuthenticationPrincipal Long teacherId,
                                                 @PathVariable Long childId) {
        childService.assertOwnedChild(teacherId, childId);
        photoService.delete(OwnerKind.CHILD, childId);
        return ResponseEntity.noContent().build();
    }

    // ---------- 교사(본인) ----------

    @PutMapping("/teachers/me/photo")
    public PhotoUploadedResponse uploadMyPhoto(@AuthenticationPrincipal Long teacherId,
                                               @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                                               @RequestBody byte[] data) {
        return new PhotoUploadedResponse(photoService.save(OwnerKind.TEACHER, teacherId, contentType, data));
    }

    @GetMapping("/teachers/me/photo")
    public ResponseEntity<byte[]> getMyPhoto(@AuthenticationPrincipal Long teacherId) {
        return toResponse(photoService.find(OwnerKind.TEACHER, teacherId));
    }

    @DeleteMapping("/teachers/me/photo")
    public ResponseEntity<Void> deleteMyPhoto(@AuthenticationPrincipal Long teacherId) {
        photoService.delete(OwnerKind.TEACHER, teacherId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> toResponse(java.util.Optional<ProfilePhoto> photo) {
        return photo
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                        .eTag("\"" + p.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() + "\"")
                        .body(p.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
