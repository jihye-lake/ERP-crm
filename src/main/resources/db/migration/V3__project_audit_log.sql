-- ============================================================================
-- 프로젝트 변경 이력(감사 로그)
-- 원본 ASP에는 매출/Kcode/직접비를 누가 언제 바꿨는지 남는 곳이 없어서,
-- 정산 금액이 안 맞을 때 추적이 안 되는 문제가 있었음.
--
-- project_id에 FK를 걸지 않은 이유: 프로젝트가 삭제돼도 "누가 지웠는지" 이력은 남아야 해서.
-- ============================================================================

CREATE TABLE project_audit_log (
    id            BIGSERIAL PRIMARY KEY,
    project_id    UUID         NOT NULL,
    project_code  VARCHAR(20),
    action        VARCHAR(30)  NOT NULL,   -- REVENUE_UPDATE / KCODE_UPDATE / DIRECT_COST_UPDATE / PROJECT_DELETE / EXCEL_IMPORT
    old_value     VARCHAR(500),
    new_value     VARCHAR(500),
    changed_by    VARCHAR(50)  NOT NULL,
    changed_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_project    ON project_audit_log (project_id, changed_at DESC);
CREATE INDEX idx_audit_changed_at ON project_audit_log (changed_at DESC);
