-- ============================================================================
-- H2 전용: V5__project_no_drop_kcode.sql 대응
-- V2 시드 + schema-h2-v4-data.sql 이 끝난 뒤에 실행된다.
-- 번호 매기는 순서는 V5와 반드시 같아야 함!
-- ============================================================================

-- 1) Kcode 제거
DROP INDEX IF EXISTS idx_project_kcode;
ALTER TABLE project DROP COLUMN kcode;
ALTER TABLE project DROP COLUMN requires_partner_code;

-- 2) No: V5와 같은 시간순으로 다시 번호 매김.
--    시드 N건이 IDENTITY로 1..N 을 이미 썼으므로 순서만 바뀌고, 다음 번호는 N+1 그대로.
CREATE TABLE project_no_map AS
SELECT id, ROW_NUMBER() OVER (ORDER BY year_code, month_code, created_at, project_code) AS rn
FROM project;

UPDATE project SET no = (SELECT m.rn FROM project_no_map m WHERE m.id = project.id);

DROP TABLE project_no_map;

ALTER TABLE project ALTER COLUMN no SET GENERATED ALWAYS;
ALTER TABLE project ADD CONSTRAINT uq_project_no UNIQUE (no);
