-- ============================================================================
-- 프로젝트코드 형식 변경
--   기존: P + 연도2 + 월2 + 순번4            (예: P26110001)
--   변경: 연도2-종류약어(2~3)-클라이언트약어(3)-순번2 (예: 26-DSH-BOT-03)
--
-- 새 코드에는 월이 없어서, 지금까지 코드에서 파생하던 month_code를 일반 컬럼으로 바꿔
-- 기존 값을 그대로 보존한다. (연/월 검색, 월 합계, 월별 통계가 이 컬럼을 사용)
-- year_code는 새 코드 앞 2자리에서 다시 파생한다.
--
-- 순번은 같은 (연도, 종류, 클라이언트) 안에서 기존 코드 순서(= 시간순)대로 01부터 매긴다.
-- H2용 같은 변환은 schema-h2-v4-data.sql 에 있음. 여기를 고치면 거기도 같이 맞출 것!
-- ============================================================================

-- 1) 월 보존: generated column → 일반 컬럼 (값은 그대로 남음, PostgreSQL 13+)
DROP INDEX idx_project_year_month;
ALTER TABLE project ALTER COLUMN month_code DROP EXPRESSION;
ALTER TABLE project DROP COLUMN year_code;

-- 2) 기존 코드 → 새 코드 매핑
CREATE TEMPORARY TABLE project_code_map AS
SELECT id,
       old_code,
       yy || '-' || type_abbr || '-' || client_abbr || '-' ||
       lpad(cast(row_number() OVER (PARTITION BY yy, type_abbr, client_abbr ORDER BY old_code) AS varchar), 2, '0') AS new_code
FROM (
    SELECT id,
           project_code AS old_code,
           substring(project_code from 2 for 2) AS yy,
           CASE project_type
               WHEN '플랫폼구축'   THEN 'PLT'
               WHEN '데이터분석'   THEN 'ANL'
               WHEN '대시보드구축' THEN 'DSH'
               WHEN '컨설팅'       THEN 'CON'
               ELSE 'ETC'
           END AS type_abbr,
           CASE client_name
               WHEN '블루오션테크'       THEN 'BOT'
               WHEN '파인트리컨설팅'     THEN 'PTC'
               WHEN '선샤인헬스케어'     THEN 'SSH'
               WHEN '선라인마케팅'       THEN 'SLM'
               WHEN '메트로데이터랩'     THEN 'MDL'
               WHEN '그린필드전자'       THEN 'GFE'
               WHEN '노스브릿지시스템즈' THEN 'NBS'
               WHEN '클리어웨이브통신'   THEN 'CWT'
               ELSE 'ETC'
           END AS client_abbr
    FROM project
    WHERE project_code LIKE 'P%'
) src;

UPDATE project
SET project_code = (SELECT m.new_code FROM project_code_map m WHERE m.id = project.id)
WHERE id IN (SELECT id FROM project_code_map);

-- 변경 이력에 남은 코드도 새 코드로 (삭제된 프로젝트의 이력은 매핑이 없어서 옛 코드 그대로 남음)
UPDATE project_audit_log
SET project_code = (SELECT min(m.new_code) FROM project_code_map m WHERE m.old_code = project_audit_log.project_code)
WHERE project_code IN (SELECT old_code FROM project_code_map);

DROP TABLE project_code_map;

-- 3) 새 형식 기준으로 연도 파생 컬럼/인덱스/제약 재구성
ALTER TABLE project ADD COLUMN year_code VARCHAR(4)
    GENERATED ALWAYS AS ('20' || substring(project_code from 1 for 2)) STORED;
ALTER TABLE project ALTER COLUMN month_code SET NOT NULL;
ALTER TABLE project ADD CONSTRAINT ck_project_month_code CHECK (month_code ~ '^(0[1-9]|1[0-2])$');
ALTER TABLE project ADD CONSTRAINT ck_project_code_format
    CHECK (project_code ~ '^[0-9]{2}-[A-Z]{2,3}-[A-Z]{3}-[0-9]{2}$');

DROP INDEX idx_project_code;
ALTER TABLE project ADD CONSTRAINT uq_project_code UNIQUE (project_code);
CREATE INDEX idx_project_year_month ON project (year_code, month_code);
