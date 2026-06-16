-- V2__add_child_gender.sql : 아이 성별 컬럼 추가 (디자인 정합 — 명단/등록 화면의 성별 토글)
-- enum 저장값: MALE / FEMALE (한글 표기는 프론트 매핑)

ALTER TABLE child
    ADD COLUMN gender VARCHAR(10) NOT NULL AFTER birth_date;
