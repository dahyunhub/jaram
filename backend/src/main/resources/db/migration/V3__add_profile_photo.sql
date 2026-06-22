-- V3__add_profile_photo.sql : 프로필 이미지(아이·교사). 클라이언트가 1:1 크롭+리사이즈한 작은 이미지를 저장.
-- 메인 테이블(child/teacher)에 BLOB 을 섞지 않도록 별도 테이블. owner_kind: CHILD / TEACHER.

CREATE TABLE profile_photo (
    owner_kind   VARCHAR(10)  NOT NULL,
    owner_id     BIGINT       NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    data         LONGBLOB     NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (owner_kind, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
