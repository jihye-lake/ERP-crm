-- ============================================================================
-- H2(로컬 확인용) 전용 스키마
-- db/migration/V1, V3 와 구조는 같고 H2가 못 읽는 문법만 바꿈.
--   - gen_random_uuid()                -> RANDOM_UUID()
--   - GENERATED ALWAYS AS (...) STORED -> GENERATED ALWAYS AS (...)   (H2는 STORED 미지원)
-- h2 프로필에서는 Flyway를 끄고 이 파일 + V2 시드를 sql.init 으로 돌린다.
-- 마이그레이션 추가하면 여기도 같이 맞춰야 함!
-- ============================================================================

-- 같은 JVM에서 컨텍스트가 여러 번 뜨면(테스트) 인메모리 DB가 재사용되므로 먼저 지움
DROP TABLE IF EXISTS project_audit_log;
DROP TABLE IF EXISTS direct_cost_item;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS admin_account;

CREATE TABLE admin_account (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE project (
    id                     UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    project_code           VARCHAR(20)  NOT NULL,
    kcode                  VARCHAR(20),
    title                  VARCHAR(500) NOT NULL,
    project_type           VARCHAR(100),
    manager                VARCHAR(100),
    client_name            VARCHAR(200),
    client_manager         VARCHAR(100),
    revenue_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,
    requires_partner_code  BOOLEAN NOT NULL DEFAULT FALSE,
    year_code              VARCHAR(4)  GENERATED ALWAYS AS ('20' || SUBSTRING(project_code FROM 2 FOR 2)),
    month_code             VARCHAR(2)  GENERATED ALWAYS AS (SUBSTRING(project_code FROM 4 FOR 2)),
    created_at             TIMESTAMP NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_code       ON project (project_code);
CREATE INDEX idx_project_year_month ON project (year_code, month_code);
CREATE INDEX idx_project_kcode      ON project (kcode);

CREATE TABLE direct_cost_item (
    id            BIGSERIAL PRIMARY KEY,
    project_id    UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    category_code VARCHAR(30) NOT NULL,
    unit_price    NUMERIC(15,2),
    quantity      NUMERIC(15,2),
    days          NUMERIC(15,2),
    amount        NUMERIC(15,2) NOT NULL DEFAULT 0,
    note          VARCHAR(500),
    CONSTRAINT uq_direct_cost_item UNIQUE (project_id, category_code)
);

CREATE INDEX idx_direct_cost_item_project ON direct_cost_item (project_id);

-- V3__project_audit_log.sql
CREATE TABLE project_audit_log (
    id            BIGSERIAL PRIMARY KEY,
    project_id    UUID         NOT NULL,
    project_code  VARCHAR(20),
    action        VARCHAR(30)  NOT NULL,
    old_value     VARCHAR(500),
    new_value     VARCHAR(500),
    changed_by    VARCHAR(50)  NOT NULL,
    changed_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_project    ON project_audit_log (project_id, changed_at DESC);
CREATE INDEX idx_audit_changed_at ON project_audit_log (changed_at DESC);
