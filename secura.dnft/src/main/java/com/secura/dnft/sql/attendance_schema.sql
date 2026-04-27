-- ============================================================
-- Facial Recognition Attendance System — Schema
-- Run this once against your PostgreSQL database before starting
-- the application with spring.jpa.hibernate.ddl-auto=update.
-- ============================================================

-- Employees master table
CREATE TABLE IF NOT EXISTS attendance_employees (
    id            BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50)  UNIQUE NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    department    VARCHAR(100),
    phone         VARCHAR(20),
    email         VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Face embedding templates (one employee can have multiple templates)
CREATE TABLE IF NOT EXISTS attendance_face_templates (
    id            BIGSERIAL PRIMARY KEY,
    employee_id   BIGINT       NOT NULL REFERENCES attendance_employees(id),
    embedding_json TEXT        NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Attendance log entries (one row per entry; exit_time is filled on mark-exit)
CREATE TABLE IF NOT EXISTS attendance_logs (
    id                 BIGSERIAL PRIMARY KEY,
    employee_id        BIGINT    NOT NULL REFERENCES attendance_employees(id),
    entry_time         TIMESTAMP,
    exit_time          TIMESTAMP,
    device_id          VARCHAR(100),
    match_score_entry  DOUBLE PRECISION,
    match_score_exit   DOUBLE PRECISION,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Audit trail for all recognition attempts
CREATE TABLE IF NOT EXISTS attendance_audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    employee_id  BIGINT,
    details_json TEXT,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
