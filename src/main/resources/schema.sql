-- ============================================================================
-- Nexora ERP - PostgreSQL schema
-- Classic ASP + MSSQL 레거시 ERP(사내 프로젝트 매출/직접비 관리)를
-- Spring Boot + PostgreSQL로 재구현하면서 정규화한 스키마입니다.
--
-- 원본(ASP) 대비 변경 사항:
--  - ERP_direct_cost 테이블의 "항목1_단가/항목1_수량/... x 27항목" 형태의
--    반복 컬럼(비정규화)을 direct_cost_item 자식 테이블로 정규화했습니다.
--  - 프로젝트코드 문자열에서 매번 substring으로 연/월을 파싱하던 방식을
--    generated column(year_code, month_code)으로 대체해 인덱싱/검색을 단순화했습니다.
--  - 프로젝트 IDX는 SQL Server의 uniqueidentifier 기본값 대신 gen_random_uuid()를 사용합니다.
-- ============================================================================

DROP TABLE IF EXISTS direct_cost_item;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS admin_account;

CREATE TABLE admin_account (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,      -- ADMIN / EDITOR / VIEWER
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE project (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_code           VARCHAR(20)  NOT NULL,                 -- 예: P26050001 (P + 연도2자리 + 월2자리 + 순번)
    kcode                  VARCHAR(20),
    title                  VARCHAR(500) NOT NULL,
    project_type           VARCHAR(100),
    manager                VARCHAR(100),
    client_name            VARCHAR(200),
    client_manager         VARCHAR(100),
    revenue_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,      -- 매출 금액(부가세 별도)
    requires_partner_code  BOOLEAN NOT NULL DEFAULT FALSE,        -- 파트너 정산 코드(Kcode) 입력 대상 여부
    year_code              VARCHAR(4)  GENERATED ALWAYS AS ('20' || substring(project_code from 2 for 2)) STORED,
    month_code             VARCHAR(2)  GENERATED ALWAYS AS (substring(project_code from 4 for 2)) STORED,
    created_at             TIMESTAMP NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_code   ON project (project_code);
CREATE INDEX idx_project_year_month ON project (year_code, month_code);
CREATE INDEX idx_project_kcode  ON project (kcode);

CREATE TABLE direct_cost_item (
    id            BIGSERIAL PRIMARY KEY,
    project_id    UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    category_code VARCHAR(30) NOT NULL,   -- CostCategory enum name
    unit_price    NUMERIC(15,2),
    quantity      NUMERIC(15,2),
    days          NUMERIC(15,2),
    amount        NUMERIC(15,2) NOT NULL DEFAULT 0,
    note          VARCHAR(500),
    CONSTRAINT uq_direct_cost_item UNIQUE (project_id, category_code)
);

CREATE INDEX idx_direct_cost_item_project ON direct_cost_item (project_id);
