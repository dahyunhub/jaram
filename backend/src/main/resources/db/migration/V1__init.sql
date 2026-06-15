-- V1__init.sql : 자람 초기 스키마 (7 tables)
-- DBMS: MySQL 8.x / InnoDB / utf8mb4
-- 정본: docs/specs/data-model-spec.md §4

CREATE TABLE teacher (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(100) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_teacher_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE classroom (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    year       INT          NOT NULL,
    start_date DATE         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_classroom_teacher_name_year UNIQUE (teacher_id, name, year),
    CONSTRAINT fk_classroom_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_classroom_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE child (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    classroom_id BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    birth_date   DATE         NOT NULL,
    token_alias  VARCHAR(50)  NOT NULL,
    deleted_at   DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_child_classroom_alias UNIQUE (classroom_id, token_alias),
    CONSTRAINT fk_child_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    INDEX idx_child_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE memo (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    child_id        BIGINT        NOT NULL,
    teacher_id      BIGINT        NOT NULL,
    content         VARCHAR(2000) NULL,
    play_activity   VARCHAR(500)  NULL,
    interaction     VARCHAR(500)  NULL,
    attitude        VARCHAR(500)  NULL,
    curriculum_area VARCHAR(20)   NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    deleted_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memo_child   FOREIGN KEY (child_id)   REFERENCES child (id),
    CONSTRAINT fk_memo_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_memo_child (child_id),
    INDEX idx_memo_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE daily_journal (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    teacher_id   BIGINT      NOT NULL,
    classroom_id BIGINT      NOT NULL,
    journal_date DATE        NOT NULL,
    content      MEDIUMTEXT  NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    analyzed_at  DATETIME(6) NULL,
    version      INT         NOT NULL DEFAULT 1,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_daily_journal_teacher_classroom_date UNIQUE (teacher_id, classroom_id, journal_date),
    CONSTRAINT fk_daily_journal_teacher   FOREIGN KEY (teacher_id)   REFERENCES teacher (id),
    CONSTRAINT fk_daily_journal_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE journal_memo_link (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    daily_journal_id BIGINT      NOT NULL,
    memo_id          BIGINT      NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_journal_memo_link UNIQUE (daily_journal_id, memo_id),
    CONSTRAINT fk_jml_journal FOREIGN KEY (daily_journal_id) REFERENCES daily_journal (id),
    CONSTRAINT fk_jml_memo    FOREIGN KEY (memo_id)          REFERENCES memo (id),
    INDEX idx_jml_journal (daily_journal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE child_report (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    child_id     BIGINT      NOT NULL,
    report_type  VARCHAR(20) NOT NULL,
    period_start DATE        NOT NULL,
    period_end   DATE        NOT NULL,
    report_month VARCHAR(7)  NULL,
    content      MEDIUMTEXT  NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_child_report_child_month UNIQUE (child_id, report_month),
    CONSTRAINT fk_child_report_child FOREIGN KEY (child_id) REFERENCES child (id),
    INDEX idx_child_report_child (child_id),
    INDEX idx_child_report_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
