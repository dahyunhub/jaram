package com.ondo.photo.dto;

import java.time.LocalDateTime;

/** 사진 업로드 응답 — 프론트가 캐시 키(갱신시각)를 갱신하는 데 사용. */
public record PhotoUploadedResponse(LocalDateTime photoUpdatedAt) {
}
